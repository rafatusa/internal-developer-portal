# Class: idp::hardening
# Purpose: OS hardening — SSH policy, sysctl tuning, UFW firewall rules.
class idp::hardening {

  # ─── SSH hardening ────────────────────────────────────────────────────────
  file { '/etc/ssh/sshd_config.d/99-idp-hardening.conf':
    ensure  => file,
    mode    => '0600',
    owner   => 'root',
    group   => 'root',
    content => @("SSH_CONF"),
      PermitRootLogin no
      PasswordAuthentication no
      ChallengeResponseAuthentication no
      UsePAM yes
      X11Forwarding no
      AllowAgentForwarding no
      MaxAuthTries 3
      LoginGraceTime 30
      ClientAliveInterval 300
      ClientAliveCountMax 2
      | SSH_CONF
    notify  => Service['sshd'],
  }

  service { 'sshd':
    ensure => running,
    enable => true,
  }

  # ─── Sysctl hardening ─────────────────────────────────────────────────────
  file { '/etc/sysctl.d/99-idp-hardening.conf':
    ensure  => file,
    mode    => '0644',
    owner   => 'root',
    group   => 'root',
    content => @("SYSCTL"),
      net.ipv4.tcp_syncookies = 1
      net.ipv4.conf.all.rp_filter = 1
      net.ipv4.conf.default.rp_filter = 1
      net.ipv4.conf.all.accept_redirects = 0
      net.ipv4.conf.default.accept_redirects = 0
      net.ipv4.conf.all.send_redirects = 0
      net.ipv6.conf.all.disable_ipv6 = 1
      kernel.dmesg_restrict = 1
      fs.protected_hardlinks = 1
      fs.protected_symlinks = 1
      | SYSCTL
    notify  => Exec['sysctl-reload'],
  }

  exec { 'sysctl-reload':
    command     => '/sbin/sysctl --system',
    refreshonly => true,
  }

  # ─── UFW firewall ─────────────────────────────────────────────────────────
  exec { 'ufw-allow-ssh':
    command => '/usr/sbin/ufw allow 22/tcp',
    unless  => '/usr/sbin/ufw status | grep "22/tcp.*ALLOW"',
    require => Package['ufw'],
  }

  exec { 'ufw-allow-http':
    command => '/usr/sbin/ufw allow 80/tcp',
    unless  => '/usr/sbin/ufw status | grep "80/tcp.*ALLOW"',
    require => Package['ufw'],
  }

  exec { 'ufw-allow-https':
    command => '/usr/sbin/ufw allow 443/tcp',
    unless  => '/usr/sbin/ufw status | grep "443/tcp.*ALLOW"',
    require => Package['ufw'],
  }

  exec { 'ufw-enable':
    command => '/bin/bash -c "echo \"y\" | /usr/sbin/ufw enable"',
    unless  => '/usr/sbin/ufw status | grep "Status: active"',
    require => [Exec['ufw-allow-ssh'], Exec['ufw-allow-http'], Exec['ufw-allow-https']],
  }
}
