# Installs the Java runtime.
#
# The portal itself ships as a container with its own JRE, but a host-level JDK
# is required for operator tooling (jcmd, jstack, jmap) when diagnosing the
# running application, and for any future non-containerised sidecar.
#
# Ubuntu 22.04 (jammy) carries openjdk-21-jdk-headless in its default archive,
# so no third-party PPA is needed.
class portal_java {

  package { 'openjdk-21-jdk-headless':
    ensure => installed,
  }

  # Make the runtime discoverable for login shells and systemd units.
  file { '/etc/profile.d/java.sh':
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    content => "export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64\nexport PATH=\"\$JAVA_HOME/bin:\$PATH\"\n",
    require => Package['openjdk-21-jdk-headless'],
  }
}
