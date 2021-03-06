#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/missing_alerts_checker

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

python missing_alerts_checker.py --new-relic-api-key ${NEW_RELIC_API_KEY}
