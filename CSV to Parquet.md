# Global Vars 


```sh
export AWS_PROFILE=<role@ADFS-role>
export SCRIPT_NAME=csv2parquet.py
export S3_PATH=perso-nta
export JOB_NAME="JobCsv2Parquet"
export SCRIPT_LOCATION="s3://$S3_PATH/aws-glue-scripts/$SCRIPT_NAME"
export ROLE_NAME="arn:aws:iam::118243050178:role/GlueServiceRole"
```


# Glue Script
[cvsToParquet.py](csv2parquet/csvToParquet.py)

# Glue Script Storing in S3
```sh
aws s3 cp $SCRIPT_NAME $SCRIPT_LOCATION
```
# Glue Job Creation 
```sh
aws glue create-job \
  --name $JOB_NAME \
  --role $ROLE_NAME \
  --command '{"Name":"glueetl","ScriptLocation":"'$SCRIPT_LOCATION'","PythonVersion":"3"}' \
  --default-arguments '{"--job-language":"python","--enable-continuous-cloudwatch-log": "true"}' \
  --max-capacity 2 \
  --region eu-west-3
  
```
# Glue Job Start

```sh
export S3_IN_PATH="s3://$S3_PATH/tpcds/"
export S3_OUT_PATH="s3://$S3_PATH/parquet/tpcds/"
aws glue start-job-run \
  --job-name $JOB_NAME \
  --arguments '{"--S3_IN_PATH":"'$S3_IN_PATH'", "--S3_OUT_PATH":"'$S3_OUT_PATH'", "--CSV_SEP":"|", "--SOURCE_DB_NAME":"tpcds_db", "--TARGET_DB_NAME":"tpcds_db_parquet"}'

```

```ad-note
title: policy nécessaire pour le job :

```json
{
	"Effect": "Allow",
	"Action": [
		"s3:GetObject",
		"s3:PutObject",
		"s3:DeleteObject"
	],
	"Resource": *
}
```
