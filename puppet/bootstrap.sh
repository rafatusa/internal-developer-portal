#!/usr/bin/env bash
#
# Puppet bootstrap entrypoint.
#
# Runs on the freshly provisioned host as root. Installs puppet-agent from the
# distribution repositories (no Puppet Server required — this is a masterless
# `puppet apply` bootstrap), then applies the portal's base manifest.
#
# Idempotent: safe to re-run on every pipeline retry.
#
# Environment:
#   DEPLOY_USER  login account CI uses (from the SSH_USER secret). Exported to
#                Puppet as the `deploy_user` fact. Defaults to 'ubuntu'.

set -euo pipefail

MANIFEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export DEBIAN_FRONTEND=noninteractive
export FACTER_deploy_user="${DEPLOY_USER:-ubuntu}"

log() {
  echo "[puppet-bootstrap] $*"
}

wait_for_apt() {
  # cloud-init runs unattended-upgrades on first boot and holds the dpkg lock.
  local attempt
  for attempt in $(seq 1 60); do
    if ! fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 \
      && ! fuser /var/lib/apt/lists/lock >/dev/null 2>&1; then
      return 0
    fi
    log "apt/dpkg lock held, waiting (attempt ${attempt}/60)"
    sleep 5
  done
  log "ERROR: apt lock still held after 5 minutes"
  return 1
}

install_puppet() {
  if command -v puppet >/dev/null 2>&1; then
    log "puppet already installed: $(puppet --version)"
    return 0
  fi

  log "installing puppet agent"
  wait_for_apt
  # Always refresh: the archive index baked into a cloud image is stale enough
  # to 404 on dependency versions that have already been superseded.
  apt-get update -y
  apt-get install -y --no-install-recommends puppet
  log "installed puppet: $(puppet --version)"
}

apply_manifest() {
  log "applying manifest ${MANIFEST_DIR}/manifests/site.pp (deploy_user=${FACTER_deploy_user})"
  # --detailed-exitcodes: 0 = no changes, 2 = changes applied successfully.
  # 4 and 6 indicate failures. Treat 0 and 2 as success.
  set +e
  puppet apply \
    --detailed-exitcodes \
    --modulepath "${MANIFEST_DIR}/modules" \
    "${MANIFEST_DIR}/manifests/site.pp"
  local rc=$?
  set -e

  case "${rc}" in
    0) log "no changes required" ;;
    2) log "changes applied successfully" ;;
    *) log "ERROR: puppet apply failed with exit code ${rc}"; exit "${rc}" ;;
  esac
}

main() {
  log "starting bootstrap on $(hostname)"
  install_puppet
  apply_manifest
  log "bootstrap complete"
}

main "$@"
