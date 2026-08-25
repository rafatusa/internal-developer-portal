# Class: idp::users
# Purpose: Create application service account and grant Docker access.
class idp::users {

  group { 'idpapp':
    ensure => present,
    system => true,
  }

  user { 'idpapp':
    ensure     => present,
    gid        => 'idpapp',
    groups     => ['docker'],
    shell      => '/bin/bash',
    home       => '/opt/idpapp',
    managehome => true,
    system     => true,
    require    => [Group['idpapp'], Service['docker']],
  }

  file { '/opt/idpapp':
    ensure  => directory,
    owner   => 'idpapp',
    group   => 'idpapp',
    mode    => '0750',
    require => User['idpapp'],
  }

  file { '/opt/idpapp/logs':
    ensure  => directory,
    owner   => 'idpapp',
    group   => 'idpapp',
    mode    => '0750',
    require => File['/opt/idpapp'],
  }
}
