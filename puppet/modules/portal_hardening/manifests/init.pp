# Baseline OS hardening aligned with the CIS Ubuntu Linux benchmark.
#
# Deliberately conservative: every control here is safe to apply to a host that
# CI must still reach over SSH on port 22.
class portal_hardening {

  # --- SSH daemon -----------------------------------------------------------
  # Key-only authentication, no direct root login, no stale sessions.
  file { '/etc/ssh/sshd_config.d/99-portal-hardening.conf':
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0600',
    content => @(SSHD),
      # Managed by Puppet — portal_hardening
      PermitRootLogin no
      PasswordAuthentication no
      ChallengeResponseAuthentication no
      KbdInteractiveAuthentication no
      PermitEmptyPasswords no
      X11Forwarding no
      MaxAuthTries 4
      LoginGraceTime 30
      ClientAliveInterval 300
      ClientAliveCountMax 2
      | SSHD
    notify  => Service['ssh'],
  }

  service { 'ssh':
    ensure => running,
    enable => true,
  }

  # --- Kernel network parameters -------------------------------------------
  file { '/etc/sysctl.d/99-portal-hardening.conf':
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    content => @(SYSCTL),
      # Managed by Puppet — portal_hardening
      net.ipv4.conf.all.accept_redirects = 0
      net.ipv4.conf.default.accept_redirects = 0
      net.ipv4.conf.all.send_redirects = 0
      net.ipv4.conf.default.send_redirects = 0
      net.ipv4.conf.all.accept_source_route = 0
      net.ipv4.conf.default.accept_source_route = 0
      net.ipv4.conf.all.log_martians = 1
      net.ipv4.tcp_syncookies = 1
      net.ipv4.icmp_echo_ignore_broadcasts = 1
      kernel.randomize_va_space = 2
      | SYSCTL
    notify  => Exec['sysctl-reload'],
  }

  exec { 'sysctl-reload':
    command     => '/usr/sbin/sysctl --system',
    refreshonly => true,
  }

  # --- Unattended security updates -----------------------------------------
  package { 'unattended-upgrades':
    ensure => installed,
  }

  file { '/etc/apt/apt.conf.d/20auto-upgrades':
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    content => @(UPGRADES),
      APT::Periodic::Update-Package-Lists "1";
      APT::Periodic::Unattended-Upgrade "1";
      | UPGRADES
    require => Package['unattended-upgrades'],
  }

  # --- File permissions on sensitive paths ---------------------------------
  file { '/etc/shadow':
    ensure => file,
    owner  => 'root',
    group  => 'shadow',
    mode   => '0640',
  }

  file { '/etc/gshadow':
    ensure => file,
    owner  => 'root',
    group  => 'shadow',
    mode   => '0640',
  }

  # --- Host firewall --------------------------------------------------------
  # The security group is the primary control; ufw is defence in depth.
  package { 'ufw':
    ensure => installed,
  }

  exec { 'ufw-allow-ssh':
    command => '/usr/sbin/ufw allow 22/tcp',
    unless  => '/usr/sbin/ufw status | /usr/bin/grep -qw "22/tcp"',
    require => Package['ufw'],
  }

  exec { 'ufw-allow-http':
    command => '/usr/sbin/ufw allow 80/tcp',
    unless  => '/usr/sbin/ufw status | /usr/bin/grep -qw "80/tcp"',
    require => Package['ufw'],
  }

  exec { 'ufw-allow-https':
    command => '/usr/sbin/ufw allow 443/tcp',
    unless  => '/usr/sbin/ufw status | /usr/bin/grep -qw "443/tcp"',
    require => Package['ufw'],
  }

  exec { 'ufw-enable':
    command => '/usr/sbin/ufw --force enable',
    unless  => '/usr/sbin/ufw status | /usr/bin/grep -qw "Status: active"',
    require => [
      Exec['ufw-allow-ssh'],
      Exec['ufw-allow-http'],
      Exec['ufw-allow-https'],
    ],
  }
}
