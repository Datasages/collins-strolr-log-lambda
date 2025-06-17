# collins-strolr-log-lambda

![Build](https://github.com/Datasages/collins-strolr-log-lambda/actions/workflows/maven-ci.yml/badge.svg)
![Coverage](https://img.shields.io/badge/coverage-dynamic-lightgrey?style=flat) <!-- Replace with real badge if using a service -->

# environment variables lambda function

```
SCAC="AMTK"
ENABLE_REPLICATION=true
DB_URL="jdbc://postgresql://HOST_NAME:5432/strolr_logfile_db"
DB_USER="amtk_strolr_logfiledb_writer"
DB_PASSWORD_SECRET_NAME="log-indexer-db-password"
REPLICATION_BUCKET_NAME="ptc-p-logs
```

`SCAC` is the SCAC of the railroad
`ENABLE_REPLICATION` is a boolean value of whether to copy the file to another bucket. If true, the code must have a ReplicationPathProcessor defined for the SCAC
`DB_URL` is the jdbc URL of the Postgres database indexing the log files
`DB_USER` is the Postgres user with write access to the database
`DB_PASSORD_SECRET_NAME` is the AWS Secret Manager secret that stores the DB password
`REPLICATION_BUCKET_NAME` is the bucket name of the destination (customer) S3 bucket that will receive the log file. IAM permissions will be required on both our end and the customers end. the lambda function will need `PutObject`, `GetObject` and `GetObjectVersion` on the remote bucket. The remote bucket owner needs to allow the lambda function IAM role permissions to do the same
