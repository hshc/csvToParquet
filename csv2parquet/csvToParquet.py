"""Glue job: read CSVs from S3, optionally via Glue Catalog, write Parquet.

Required arguments
  - JOB_NAME: Glue job name
  - S3_IN_PATH: input S3 prefix (folder of datasets or a flat folder)
  - S3_OUT_PATH: output S3 prefix for Parquet
  - SOURCE_DB_NAME: Glue Catalog database to look up table schemas
  - TARGET_DB_NAME: Glue Catalog database for target table schemas
  
Optional arguments
  - PARQUET_COMPRESSION: snappy | gzip | none (default: snappy)
  - PARQUET_PARTITION_KEYS: comma-separated list of keys (default: none)
  - CSV_WITH_HEADER: true | false (default: false)
  - CSV_SEP: CSV separator (default: ",")
  - CSV_QUOTE: CSV quote char (default: '"')
  - CSV_ESCAPE: CSV escape char (default: '\\\')

Flow
  1) Discover subfolders under S3_IN_PATH using S3 list with '/' delimiter.
  2) If no subfolders, read the whole folder as CSV and write Parquet.
  3) If subfolders exist, for each dataset:
     - Try using a Glue Catalog table (matching the subfolder name)
     - Fallback to raw CSV read from S3
  4) Write Parquet under S3_OUT_PATH with configured compression/partitions.

Example usage
  --JOB_NAME csv2parquet \
  --S3_IN_PATH s3://bucket/input/ \
  --S3_OUT_PATH s3://bucket/output/ \
  --SOURCE_DB_NAME my_db \
  --TARGET_DB_NAME target_db \
  --PARQUET_COMPRESSION snappy \
  --PARQUET_PARTITION_KEYS year,month \
  --CSV_WITH_HEADER true \
  --CSV_SEP , \
  --CSV_QUOTE '"' \
  --CSV_ESCAPE '\\\'
"""

import sys
import boto3
import botocore
from awsglue.context import GlueContext
from pyspark.context import SparkContext
from awsglue.dynamicframe import DynamicFrame
from awsglue.job import Job
from awsglue.utils import getResolvedOptions
from urllib.parse import urlparse
from pyspark.sql.functions import col, to_date, to_timestamp
from pyspark.sql.types import DateType, TimestampType, IntegerType, DoubleType, LongType

import logging


mandatory_args_list = [
    'JOB_NAME',
    'S3_IN_PATH',
    'S3_OUT_PATH',
    'SOURCE_DB_NAME',
    'TARGET_DB_NAME'
]

# Populate optional args with defaults if absent
_optional_defaults = {
    'PARQUET_COMPRESSION': 'snappy',
    'PARQUET_PARTITION_KEYS': '',
    'CSV_WITH_HEADER': 'false',
    'CSV_SEP': ',',
    'CSV_QUOTE': '"',
    'CSV_ESCAPE': '\\\'
}

def cast_columns_to_target_catalog(target_schema, df):
    for col_def in target_schema:
        col_name = col_def['Name']
        col_type = col_def['Type'].lower()
        if col_name not in df.columns:
            continue

        # Casting selon le type cible
        if col_type == "date":
            logger.info(f"Casting {col_name} -> DATE")
            df = df.withColumn(col_name, to_date(col(col_name), "yyyy-MM-dd"))
        elif col_type in ("timestamp", "timestamp with time zone"):
            logger.info(f"Casting {col_name} -> TIMESTAMP")
            df = df.withColumn(col_name, to_timestamp(col(col_name), "HH:mm:ss"))
        elif col_type in ("int", "integer"):
            logger.info(f"Casting {col_name} -> INT")
            df = df.withColumn(col_name, col(col_name).cast(IntegerType()))
        elif col_type in ("bigint", "long"):
            logger.info(f"Casting {col_name} -> LONG")
            df = df.withColumn(col_name, col(col_name).cast(LongType()))
        elif col_type in ("double", "float", "decimal"):
            logger.info(f"Casting {col_name} -> DOUBLE")
            df = df.withColumn(col_name, col(col_name).cast(DoubleType()))
        # sinon on laisse tel quel (string, etc.)
    return df
    
def getOptionalArg(argv, name, default):
    if f'--{name}' in argv:
        idx = argv.index(f'--{name}')
        if idx + 1 < len(argv):
            return argv[idx + 1]
    return default


def getResolvedOptionsWithOptionals(argv, mandatory_args_list, 
                                    _optional_defaults):
    # D'abord récupérer les arguments obligatoires
    args = getResolvedOptions(argv, mandatory_args_list)
    
    # Ensuite gérer les arguments optionnels avec leurs valeurs par défaut:
    for opt_name, opt_default in _optional_defaults.items():
        args[opt_name] = getOptionalArg(argv, opt_name, opt_default)
        
    # Validate compression argument
    valid_compressions = ['snappy', 'gzip', 'none']
    if args['PARQUET_COMPRESSION'].lower() not in valid_compressions:
        logger.warning(
            f"Compression '{args['PARQUET_COMPRESSION']}' non valide. "
            f"Utilisation de 'snappy' par défaut."
        )
        args['PARQUET_COMPRESSION'] = _optional_defaults['PARQUET_COMPRESSION']
        
    return args


def create_csv_datasource(glueContext, input_path, args):
    return glueContext.create_dynamic_frame.from_options(
        connection_type="s3",
        connection_options={"paths": [input_path], "recurse": True},
        format="csv",
        format_options={
            "withHeader": args['CSV_WITH_HEADER'].lower() == 'true',
            "separator": args['CSV_SEP'],
            "quoteChar": args['CSV_QUOTE'],
            "escapeChar": args['CSV_ESCAPE']
        },
    )


args = getResolvedOptionsWithOptionals(
    sys.argv,
    mandatory_args_list,
    _optional_defaults
)

sc = SparkContext()
glueContext = GlueContext(sc)
logger = glueContext.get_logger()
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args['JOB_NAME'], args)

# Configure logger to also write to stdout for easier observability
# logger.setLevel(logging.INFO)
# stream_handler = logging.StreamHandler(sys.stdout)
# stream_handler.setLevel(logging.INFO)
# formatter = logging.Formatter(
#     '%(asctime)s %(levelname)s %(name)s - %(message)s'
# )
# stream_handler.setFormatter(formatter)
# if not any(isinstance(h, logging.StreamHandler) for h in logger.handlers):
#     logger.addHandler(stream_handler)
# logger.propagate = False
# ----------------------------------------

s3_in_path = args['S3_IN_PATH'].rstrip('/')
s3_out_path = args['S3_OUT_PATH'].rstrip('/')
source_db_name = args['SOURCE_DB_NAME']
target_db_name = args['TARGET_DB_NAME']

partition_keys = [
    key.strip()
    for key in args['PARQUET_PARTITION_KEYS'].split(',')
    if key.strip()
]

parsed_url = urlparse(s3_in_path)
bucket = parsed_url.netloc
prefix = parsed_url.path.lstrip('/') + '/'

logger.info(f"Chemin source : {s3_in_path}")
logger.info(f"Chemin destination : {s3_out_path}")
logger.info(f"Bucket : {bucket}")
logger.info(f"Prefix : {prefix}")

# --- Récupérer les tables du Catalog input ---
glue = boto3.client('glue')
try:
    tables = []
    paginator = glue.get_paginator('get_tables')
    for page in paginator.paginate(DatabaseName=source_db_name):
        tables.extend(page['TableList'])
    datasets = [t['Name'] for t in tables]
except Exception as e:
    logger.error(f"Impossible de récupérer les tables du catalogue '{source_db_name}': {e}")
    raise

logger.info(f"Tables détectées dans la base {source_db_name} : {datasets}")
logger.info(f"Nombre de datasets à traiter : {len(datasets)}")

frame_count = 0
for idx, dataset_name in enumerate(datasets, start=1):
    # Extraire le nom du dataset depuis le chemin S3
    # (ex: "tpcds/call_center/")
    # dataset_name = subdir[len(prefix):].strip('/')

    input_path = f"{s3_in_path}/{dataset_name}"
    output_path = f"{s3_out_path}/{dataset_name}"

    logger.info(f"[Frame {idx}] Dataset : {dataset_name or prefix}")
    logger.info(f"Chemin lecture : {input_path}")
    logger.info(f"Chemin écriture : {output_path}")

    # --- Lecture ---
    try:
        datasource = glueContext.create_dynamic_frame.from_catalog(
            database=source_db_name,
            table_name=dataset_name,
            additional_options={
                "paths": [input_path],
                "recurse": True
            },
            transformation_ctx=f"datasource_{dataset_name}"
        )
        logger.info(
            f"Table Glue trouvée pour '{dataset_name}', "
            f"chargement depuis le catalogue"
        )
    except Exception as e:
        logger.warning(f"Pas de table Catalog pour '{dataset_name}', lecture brute CSV : {e}")
        datasource = create_csv_datasource(glueContext, input_path, args)
        

    # --- Cast selon schéma cible ---
    df = datasource.toDF()
    try:
        response = glue.get_table(DatabaseName=target_db_name, Name=dataset_name)
        schema = response['Table']['StorageDescriptor']['Columns']
        logger.info(f"Schéma cible pour {dataset_name} : {[c['Name'] + ':' + c['Type'] for c in schema]}")
        df = cast_columns_to_target_catalog(schema, df)
    except botocore.exceptions.ClientError as e:
            error_code = e.response['Error']['Code']
            # except glue.exceptions.EntityNotFoundException:
            logger.warning(
                f"Code erreur '{error_code}', "
                f"Aucune table Glue trouvée pour dataset '{dataset_name}', "
                f"lecture brute sur S3 en CSV"
            )
    datasource_casted = DynamicFrame.fromDF(df, glueContext, f"casted_{dataset_name}")

    # --- Comptage ---
    try:
        row_count = datasource.count()
        logger.info(
            f"Nombre de lignes lues pour {dataset_name or prefix} : "
            f"{row_count}"
        )
    except Exception as e:
        logger.warning(
            (
                f"Impossible de compter les lignes "
                f"({dataset_name or prefix}) : {e}"
            )
        )


    # --- Écriture ---
    write_options = {"path": output_path}
    if partition_keys:
        write_options["partitionKeys"] = partition_keys
    else:
        logger.info("Aucune clé de partition définie, pas de partitionnement.")

    glueContext.write_dynamic_frame.from_options(
        frame=datasource_casted,
        connection_type="s3",
        connection_options=write_options,
        format="parquet",
        format_options={
            "compression": args['PARQUET_COMPRESSION'].lower()
        }
    )
    frame_count += 1

logger.info(f"Nombre total de frames/datasets traités : {frame_count}")
job.commit()
logger.info("Job Glue terminé avec succès.")
