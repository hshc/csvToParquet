SELECT count(*) FROM "tpcds_db"."dbgen_version" limit 10;
CREATE EXTERNAL TABLE IF NOT EXISTS tpcds_db.customer_address (
  ca_address_sk INT,
  ca_address_id STRING,
  ca_street_number STRING,
  ca_street_name STRING,
  ca_street_type STRING,
  ca_suite_number STRING,
  ca_city STRING,
  ca_county STRING,
  ca_state STRING,
  ca_zip STRING,
  ca_country STRING,
  ca_gmt_offset DECIMAL(5,2),
  ca_location_type STRING
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '|'
STORED AS TEXTFILE
LOCATION 's3://perso-nta/tpcds/customer_address/'
TBLPROPERTIES ('classification'='csv');
SELECT count(*) FROM "tpcds_db"."dbgen_version" limit 10;