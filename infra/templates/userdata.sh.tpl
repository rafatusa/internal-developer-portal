#!/bin/bash
# EC2 User Data — minimal bootstrap; Puppet & Ansible handle full configuration
set -euo pipefail

# Set hostname
hostnamectl set-hostname ${project_name}

# Ensure SSM agent is running (pre-installed on Ubuntu 22.04 AMI)
systemctl enable amazon-ssm-agent
systemctl start  amazon-ssm-agent

# Write instance tags for identification
cat > /etc/idp-environment <<EOF
IDP_PROJECT=${project_name}
IDP_ENV=${environment}
EOF

echo "User-data complete — Puppet/Ansible will handle full configuration"
