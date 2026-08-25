# Class: idp::docker
# Purpose: Install Docker Engine CE and configure daemon settings.
class idp::docker {

  exec { 'add-docker-key':
    command => '/bin/bash -c "curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/trusted.gpg.d/docker.gpg"',
    creates => '/etc/apt/trusted.gpg.d/docker.gpg',
  }

  exec { 'add-docker-repo':
    command => '/bin/bash -c "echo \"deb [arch=amd64 signed-by=/etc/apt/trusted.gpg.d/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable\" > /etc/apt/sources.list.d/docker.list"',
    creates => '/etc/apt/sources.list.d/docker.list',
    require => Exec['add-docker-key'],
  }

  exec { 'apt-update-docker':
    command => '/usr/bin/apt-get update -qq',
    require => Exec['add-docker-repo'],
    timeout => 300,
  }

  $docker_packages = [
    'docker-ce', 'docker-ce-cli', 'containerd.io',
    'docker-buildx-plugin', 'docker-compose-plugin',
  ]

  package { $docker_packages:
    ensure  => present,
    require => Exec['apt-update-docker'],
  }

  file { '/etc/docker/daemon.json':
    ensure  => file,
    mode    => '0644',
    content => '{"log-driver":"json-file","log-opts":{"max-size":"100m","max-file":"3"},"live-restore":true}',
    require => Package['docker-ce'],
    notify  => Service['docker'],
  }

  service { 'docker':
    ensure  => running,
    enable  => true,
    require => Package['docker-ce'],
  }
}
