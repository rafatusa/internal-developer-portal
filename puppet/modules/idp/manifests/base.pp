# Class: idp::base
# Purpose: Update apt cache, install essential system packages, set timezone.
class idp::base {

  exec { 'apt-update':
    command => '/usr/bin/apt-get update -qq',
    timeout => 300,
  }

  $packages = [
    'curl', 'wget', 'git', 'unzip', 'vim', 'htop',
    'net-tools', 'ca-certificates', 'gnupg', 'lsb-release',
    'software-properties-common', 'apt-transport-https',
    'python3', 'python3-pip', 'jq', 'fail2ban', 'ufw',
  ]

  package { $packages:
    ensure  => present,
    require => Exec['apt-update'],
  }

  exec { 'set-timezone':
    command => '/usr/bin/timedatectl set-timezone UTC',
    unless  => '/usr/bin/timedatectl | /bin/grep "UTC"',
  }

  service { 'fail2ban':
    ensure  => running,
    enable  => true,
    require => Package['fail2ban'],
  }
}
