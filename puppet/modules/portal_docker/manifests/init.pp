# Installs Docker Engine from Docker's official apt repository and ensures the
# daemon is running with production-appropriate log rotation.
class portal_docker {

  $keyring = '/etc/apt/keyrings/docker.asc'

  file { '/etc/apt/keyrings':
    ensure => directory,
    owner  => 'root',
    group  => 'root',
    mode   => '0755',
  }

  exec { 'docker-gpg-key':
    command => "/usr/bin/curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o ${keyring}",
    creates => $keyring,
    require => File['/etc/apt/keyrings'],
  }

  file { $keyring:
    ensure  => file,
    mode    => '0644',
    require => Exec['docker-gpg-key'],
  }

  file { '/etc/apt/sources.list.d/docker.list':
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    content => "deb [arch=amd64 signed-by=${keyring}] https://download.docker.com/linux/ubuntu jammy stable\n",
    require => File[$keyring],
  }

  exec { 'apt-update-docker':
    command     => '/usr/bin/apt-get update -y',
    subscribe   => File['/etc/apt/sources.list.d/docker.list'],
    refreshonly => true,
    timeout     => 600,
  }

  package { ['docker-ce', 'docker-ce-cli', 'containerd.io', 'docker-compose-plugin']:
    ensure  => installed,
    require => Exec['apt-update-docker'],
  }

  # Bounded json-file logging: an unbounded container log fills the root volume
  # and takes the portal down with it.
  file { '/etc/docker/daemon.json':
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    content => @(JSON),
      {
        "log-driver": "json-file",
        "log-opts": {
          "max-size": "20m",
          "max-file": "5"
        },
        "live-restore": true
      }
      | JSON
    require => Package['docker-ce'],
    notify  => Service['docker'],
  }

  service { 'docker':
    ensure  => running,
    enable  => true,
    require => Package['docker-ce'],
  }
}
