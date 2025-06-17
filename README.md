# collins-strolr-log-lambda

![Build](https://github.com/Datasages/collins-strolr-log-lambda/actions/workflows/maven-ci.yml/badge.svg)  
![Coverage](https://img.shields.io/badge/coverage-dynamic-lightgrey?style=flat)

## Overview

The **collins-strolr-log-lambda** is an AWS Lambda function that:

- Indexes locomotive log files uploaded to an S3 bucket
- Parses the S3 object key for metadata (mark, loco number, device, end time)
- Saves metadata to a PostgreSQL database
- Optionally replicates the log file to a customer-managed S3 bucket (with IAM controls)

---

## 🔧 Environment Variables

The function is configured via the following environment variables:

```bash
SCAC="AMTK"
ENABLE_REPLICATION=true
DB_URL="jdbc:postgresql://HOST_NAME:5432/strolr_logfile_db"
DB_USER="amtk_strolr_logfiledb_writer"
DB_PASSWORD_SECRET_NAME="log-indexer-db-password"
REPLICATION_BUCKET_NAME="ptc-p-logs"
```

| Variable                  | Required                           | Description                                                    |
| ------------------------- | ---------------------------------- | -------------------------------------------------------------- |
| `SCAC`                    | ✅                                 | The SCAC code of the railroad (e.g., `AMTK`, `BNSF`)           |
| `ENABLE_REPLICATION`      | ✅                                 | Boolean flag to enable or disable S3 log replication           |
| `DB_URL`                  | ✅                                 | JDBC URL for the PostgreSQL database                           |
| `DB_USER`                 | ✅                                 | PostgreSQL user with write access to the log metadata table    |
| `DB_PASSWORD_SECRET_NAME` | ✅                                 | Name of the AWS Secrets Manager secret holding the DB password |
| `REPLICATION_BUCKET_NAME` | ➖ (if `ENABLE_REPLICATION=false`) | Name of the target S3 bucket to receive replicated log files   |

🔐 IAM & S3 Permissions
If ENABLE_REPLICATION=true, ensure the following IAM permissions are in place:

Lambda Role Permissions (Your Side)
The Lambda IAM role must have:

{
"Effect": "Allow",
"Action": [
"s3:GetObject",
"s3:GetObjectVersion",
"s3:PutObject"
],
"Resource": [
"arn:aws:s3:::source-bucket-name/*",
"arn:aws:s3:::destination-bucket-name/*"
]
}

Destination Bucket Policy (Customer Side)
The destination bucket owner must allow your IAM role to replicate files:

{
"Effect": "Allow",
"Principal": {
"AWS": "arn:aws:iam::<your-account-id>:role/<lambda-role-name>"
},
"Action": "s3:PutObject",
"Resource": "arn:aws:s3:::ptc-p-logs/\*"
}

🧪 Local Development & Testing
Integration tests use Testcontainers to simulate:

PostgreSQL (for log metadata)

AWS S3 (via LocalStack)

Refer to LogFileIndexerHandlerIT for setup and assertions.
