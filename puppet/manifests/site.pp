# Puppet site manifest — Internal Developer Portal bootstrap
# Runs on the EC2 instance via `puppet apply` in the pipeline's puppet_bootstrap stage.

node default {
  include idp::base
  include idp::hardening
  include idp::java
  include idp::docker
  include idp::users
}
