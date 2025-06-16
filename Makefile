JAR=target/strolrloglambda-2.0-SNAPSHOT.jar
BUCKET=lambda-artifacts
KEY=strolrloglambda-2.0-SNAPSHOT.jar
FUNC_NAME=LogFileIndexerHandler
ROLE_ARN=arn:aws:iam::000000000000:role/lambda-ex
SECRET_NAME=DB_PASSWORD_SECRET_NAME
SECRET_VALUE=super-secret-password
POSTGRES_CONTAINER=lambda-test-postgres
POSTGRES_PORT=5432
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=super-secret-password

# Entry point
all: build upload secrets deploy wait-for-lambda invoke

# Build the fat jar
build:
	mvn clean package



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
ENABLE_REPLICATION=false,\
SCAC=AMTK,\
DB_URL=jdbc:postgresql://host.docker.internal:5432/$(POSTGRES_DB),\
DB_USER=$(POSTGRES_USER),\
DB_PASSWORD_SECRET_NAME=$(SECRET_NAME)}"

# Wait for Lambda to become active
.PHONY: wait
wait-for-lambda:
	@echo "Waiting for Lambda to become active..."
	@until awslocal lambda get-function --function-name $(LAMBDA_NAME) 2>/dev/null | jq -r '.Configuration.State' | grep -q Active; do \
		echo "...still waiting..."; \
		sleep 2; \
	done
	@echo "Lambda is active!"


# Invoke Lambda with sample event
invoke:
	mkdir -p out
	awslocal lambda invoke \
		--function-name $(FUNC_NAME) \
		--payload fileb://test/s3-event.json \
		out/response.json
	cat out/response.json | jq

# Cleanup everything
clean:
	-docker stop $(POSTGRES_CONTAINER)
	awslocal lambda delete-function --function-name $(FUNC_NAME) || true
	awslocal secretsmanager delete-secret --secret-id $(SECRET_NAME) --force-delete-without-recovery || true
	awslocal s3 rm s3://$(BUCKET)/$(KEY) || true
