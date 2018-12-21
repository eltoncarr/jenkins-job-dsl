#!/bin/bash

set -x

cd $WORKSPACE/configuration/playbooks

ansible-playbook -i ./ec2.py --limit tag_Name_edx-admin-mms mongo_mms.yml -e@../../edx-internal/ansible/vars/edx.yml -e@../../edx-secure/ansible/vars/edx.yml -u ubuntu

