# Baseline packages and package-manager state shared by every other class.
class portal_base {

  # The host is freshly provisioned on every run, so the apt index is always
  # stale enough to 404 on dependencies. Refresh unconditionally — there is no
  # cache worth preserving here.
  exec { 'apt-update':
    command => '/usr/bin/apt-get update -y',
    timeout => 600,
    tries   => 3,
    try_sleep => 10,
  }

  file { '/var/lib/portal':
    ensure => directory,
    owner  => 'root',
    group  => 'root',
    mode   => '0755',
  }

  package { ['ca-certificates', 'curl', 'gnupg', 'unzip', 'jq', 'chrony']:
    ensure  => installed,
    require => Exec['apt-update'],
  }

  service { 'chrony':
    ensure  => running,
    enable  => true,
    require => Package['chrony'],
  }
}
