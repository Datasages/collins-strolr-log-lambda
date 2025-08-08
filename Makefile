SCAC=AMTK
JAR=target/strolrloglambda-2.0.2.jar
BUCKET=lambda-artifacts
KEY=strolrloglambda-2.0.2.jar
FUNC_NAME=LogFileIndexerHandler
ROLE_ARN=arn:aws:iam::000000000000:role/lambda-ex
SECRET_NAME=DB_PASSWORD_SECRET_NAME
SECRET_VALUE=super-secret-password
POSTGRES_CONTAINER=lambda-test-postgres
POSTGRES_PORT=5432
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=super-secret-password

SECRET_CACHE_FILE := src/main/java/com/wabtec/railwaynet/strolrloglambda/util/SecretManagerCache.java


# Entry point
all: build initdb upload-test-log upload secrets deploy wait-for-lambda invoke

# Build the fat jar
build:
	sed -i '' 's/"default-db-password"/"XXXX"/g' $(SECRET_CACHE_FILE)
	mvn clean package
	sed -i '' 's/"XXXX"/"default-db-password"/g' $(SECRET_CACHE_FILE)

initdb:
	docker exec -i $(POSTGRES_CONTAINER) psql -U $(POSTGRES_USER) -d $(POSTGRES_DB) < scripts/schema.sql

upload-test-log:
	mkdir -p test-data
	echo "test log file content" > test-data/sample.log.gz
	awslocal s3api create-bucket --bucket test-bucket || true
	awslocal s3 cp test-data/sample.log.gz \
		s3://test-bucket/amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz
	awslocal s3 cp test-data/sample.log.gz \
		s3://test-bucket/amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/chr.AMTK.10.20250605042130.log.gz
	awslocal s3 cp test-data/sample.log.gz \
		s3://test-bucket/amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/tdat.AMTK.10.20250605042130.log.gz


# Upload to LocalStack S3
upload:
	awslocal s3api create-bucket --bucket $(BUCKET) || true
	awslocal s3 cp $(JAR) s3://$(BUCKET)/$(KEY)

# Create secret in LocalStack
secrets:
	awslocal secretsmanager create-secret \
		--name $(SECRET_NAME) \
		--secret-string "$(SECRET_VALUE)" || true

# Deploy Lambda to LocalStack
deploy:
	awslocal lambda delete-function --function-name $(FUNC_NAME) || true
	awslocal lambda create-function \
		--function-name $(FUNC_NAME) \
		--runtime java17 \
		--handler com.wabtec.railwaynet.strolrloglambda.service.LogFileIndexerHandler \
		--code S3Bucket=$(BUCKET),S3Key=$(KEY) \
		--role $(ROLE_ARN) \
		--environment Variables="{\
POWERTOOLS_LOG_LEVEL=DEBUG,\
ENABLE_REPLICATION=true,\
REPLICATION_BUCKET_NAME=ptc-p-logs, \
SCAC=AMTK,\
DB_URL=jdbc:postgresql://host.docker.internal:5432/$(POSTGRES_DB),\
DB_USER=$(POSTGRES_USER),\
DB_PASSWORD_SECRET_NAME=$(SECRET_NAME)}"  > /dev/null

# Wait for Lambda to become active
.PHONY: wait
wait-for-lambda:
	@echo "Waiting for Lambda to become active..."
	@timeout=60; \
	while [ $$timeout -gt 0 ]; do \
		state=$$(awslocal lambda get-function --function-name $(FUNC_NAME) 2>/dev/null | jq -r '.Configuration.State'); \
		if [ "$$state" = "Active" ]; then echo "Lambda is active!"; exit 0; fi; \
		echo "...still waiting..."; \
		sleep 2; timeout=$$((timeout - 2)); \
	done; \
	echo "Timed out waiting for Lambda to become active"; exit 1



# Invoke Lambda with sample event
invoke:
	mkdir -p out
	awslocal lambda invoke \
		--function-name $(FUNC_NAME) \
		--payload fileb://test/s3-event.json \
		out/response.json
	cat out/response.json | jq

get-logs:
	@echo "Fetching logs for Lambda function $(FUNC_NAME)..."
	@log_group="/aws/lambda/$(FUNC_NAME)"; \
	log_stream=$$(awslocal logs describe-log-streams --log-group-name $$log_group \
		--order-by LastEventTime --descending \
		--query 'logStreams[0].logStreamName' --output text); \
	echo "Using log stream: $$log_stream"; \
	awslocal logs get-log-events \
		--log-group-name $$log_group \
		--log-stream-name "$$log_stream" \
		--query 'events[*].message' --output text

# Cleanup everything
clean:
	-docker stop $(POSTGRES_CONTAINER)
	awslocal lambda delete-function --function-name $(FUNC_NAME) || true
	awslocal secretsmanager delete-secret --secret-id $(SECRET_NAME) --force-delete-without-recovery || true
	awslocal s3 rm s3://$(BUCKET)/$(KEY) || true
