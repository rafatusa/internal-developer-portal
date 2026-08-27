# Masterless Puppet bootstrap for the Internal Developer Portal host.
#
# Responsibilities (initial server bootstrap ONLY):
#   * install the Java runtime
#   * install and enable Docker
#   * create system users and groups
#   * apply baseline OS hardening
#
# Application deployment is deliberately NOT here — Ansible owns that step and
# runs after this manifest completes.

node default {

  # The bootstrap script exports FACTER_deploy_user from the SSH_USER secret so
  # the login account is never hardcoded to a specific cloud image's default.
  $deploy_user = $facts['deploy_user'] ? {
    undef   => 'ubuntu',
    default => $facts['deploy_user'],
  }

  class { 'portal_base': }
  class { 'portal_java': }
  class { 'portal_docker': }
  class { 'portal_users':
    deploy_user => $deploy_user,
  }
  class { 'portal_hardening': }

  Class['portal_base']
  -> Class['portal_java']
  -> Class['portal_docker']
  -> Class['portal_users']
  -> Class['portal_hardening']
}
