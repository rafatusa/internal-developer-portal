# System users and groups for the portal.
#
# The application container runs as an unprivileged user; on the host we create
# a matching service account that owns the deployment directory so Ansible does
# not need to write anything as root.
class portal_users (
  # The CI login user. Defaults to the Ubuntu cloud-image user; the bootstrap
  # script overrides it from the SSH_USER secret via FACTER_deploy_user.
  String $deploy_user = 'ubuntu',
) {

  group { 'portal':
    ensure => present,
    gid    => 1500,
  }

  user { 'portal':
    ensure     => present,
    uid        => 1500,
    gid        => 'portal',
    home       => '/opt/portal',
    managehome => false,
    shell      => '/usr/sbin/nologin',
    comment    => 'Internal Developer Portal service account',
    require    => Group['portal'],
  }

  file { '/opt/portal':
    ensure  => directory,
    owner   => 'portal',
    group   => 'portal',
    mode    => '0750',
    require => User['portal'],
  }

  file { '/opt/portal/data':
    ensure  => directory,
    owner   => 'portal',
    group   => 'portal',
    mode    => '0750',
    require => File['/opt/portal'],
  }

  # The CI login user must be able to drive Docker without sudo prompts.
  exec { "add-${deploy_user}-to-docker-group":
    command => "/usr/sbin/usermod -aG docker ${deploy_user}",
    unless  => "/usr/bin/id -nG ${deploy_user} | /bin/grep -qw docker",
    require => Class['portal_docker'],
  }
}
