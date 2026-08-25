# Class: idp::java
# Purpose: Install Eclipse Temurin JDK 21 from the Adoptium repository.
class idp::java {

  exec { 'add-adoptium-key':
    command => '/bin/bash -c "wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/trusted.gpg.d/adoptium.gpg"',
    creates => '/etc/apt/trusted.gpg.d/adoptium.gpg',
  }

  exec { 'add-adoptium-repo':
    command => '/bin/bash -c "echo \"deb https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main\" > /etc/apt/sources.list.d/adoptium.list"',
    creates => '/etc/apt/sources.list.d/adoptium.list',
    require => Exec['add-adoptium-key'],
  }

  exec { 'apt-update-adoptium':
    command     => '/usr/bin/apt-get update -qq',
    refreshonly => false,
    require     => Exec['add-adoptium-repo'],
    timeout     => 300,
  }

  package { 'temurin-21-jdk':
    ensure  => present,
    require => Exec['apt-update-adoptium'],
  }

  file { '/etc/profile.d/java.sh':
    ensure  => file,
    mode    => '0644',
    content => "export JAVA_HOME=/usr/lib/jvm/temurin-21-amd64\nexport PATH=\$JAVA_HOME/bin:\$PATH\n",
    require => Package['temurin-21-jdk'],
  }
}
