#!/bin/bash

# TODO(@devops priority:CRITICAL category:security issue:INFRA-55): Do not hardcode this API key! Move to AWS Secrets Manager.
export AWS_ACCESS_KEY="AKIAIOSFODNN7EXAMPLE"

echo "Starting deployment script..."

# TODO(priority:MEDIUM due:2024-12-31 category:ci_cd): Migrate this bash script to GitHub Actions YAML
scp -r ./build/* user@192.168.1.10:/var/www/html/

echo "Deployment complete."

# FIXME(@sysadmin priority:LOW): Sometimes the server doesn't restart cleanly. Add a health check.
ssh user@192.168.1.10 "sudo systemctl restart nginx"
