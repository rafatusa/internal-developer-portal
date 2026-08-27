# Deployment Guide

How the Internal Developer Portal gets from a commit to a running host, how to
verify it, and how to undo it.

## Deployment diagram

```
 GitHub push (main)
        │
        ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  1. lint      │──▶│  2. test      │   │  3. security  │
│  Checkstyle   │   │  unit + IT    │   │  OWASP DC     │
│  SpotBugs     │   └───────┬───────┘   └───────┬───────┘
└───────┬───────┘           │                   │
        └───────────────────┴───────────────────┘
                            ▼
                  ┌───────────────────┐
                  │  4. build_push    │  mvn package → docker build
                  │                   │  → ghcr.io/<repo>:<sha>
                  └─────────┬─────────┘
                            ▼
                  ┌───────────────────┐        ┌──────────────────┐
                  │  5. provision     │───────▶│ AWS: VPC, subnet │
                  │  terraform apply  │        │ IGW, RT, SG, IAM │
                  └─────────┬─────────┘        │ EC2, EIP         │
                            │                  └──────────────────┘
                            ▼
                  ┌───────────────────┐        ┌──────────────────┐
                  │  6. puppet        │───SSH─▶│ Java 21          │
                  │  bootstrap.sh     │        │ Docker engine    │
                  │  puppet apply     │        │ users, hardening │
                  └─────────┬─────────┘        └──────────────────┘
                            ▼
                  ┌───────────────────┐        ┌──────────────────┐
                  │  7. configure     │───SSH─▶│ nginx vhost      │
                  │  ansible-playbook │        │ app.env          │
                  │                   │        │ postgres + app   │
                  └─────────┬─────────┘        │ containers       │
                            │                  └──────────────────┘
                            ▼
                  ┌───────────────────┐
                  │  8. verify        │  curl /actuator/health → 200
                  │                   │  archive /v3/api-docs
                  └─────────┬─────────┘
                            ▼
                  ┌───────────────────┐
                  │  9. perf          │  k6: p95 < 800ms, errors < 1%
                  └───────────────────┘
```

Every stage after `provision` reads the instance IP by running
`terraform output -raw instance_public_ip` against the shared remote state.
Infrastructure values are never threaded between jobs — GitHub drops job
outputs containing secret substrings, and `PROJECT_NAME` is a secret.

## Prerequisites

| Requirement | Notes |
|---|---|
| AWS account | With permission to create VPC, EC2, EIP, IAM roles |
| GitHub repository | Actions enabled; GHCR package write permission |
| Platform secrets | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `PROJECT_NAME`, `TF_STATE_BUCKET`, `SSH_USER`, `SSH_PRIVATE_KEY`, `SSH_PUBLIC_KEY` |
| Project secrets | `DB_PASSWORD`, `JWT_SECRET` (≥ 32 chars), `ADMIN_PASSWORD` |

All secrets are alphanumeric by policy — `%`, `$`, quotes and URL-special
characters break shell interpolation, YAML parsing and JDBC URLs.

## Provisioned resources

| Resource | Configuration |
|---|---|
| VPC | `10.42.0.0/16`, DNS support and hostnames enabled |
| Public subnet | `10.42.1.0/24`, first available AZ, auto-assign public IP |
| Internet gateway + route table | Default route `0.0.0.0/0` |
| Security group | Inbound 80, 443 (any), 22 (`ssh_ingress_cidr`); all outbound |
| IAM role + instance profile | `AmazonSSMManagedInstanceCore` only |
| EC2 instance | `t3.medium`, Ubuntu 22.04 LTS (latest Canonical AMI), 30 GiB encrypted gp3, IMDSv2 required |
| Elastic IP | Associated with the instance — the address survives replacement |

Every resource is tagged `Project=<project>` and `ManagedBy=udap`.

## Configuration stages in detail

### Puppet bootstrap (stage 6)

`puppet/bootstrap.sh` runs as root over SSH. It waits for the cloud-init dpkg
lock to clear, installs `puppet` from the Ubuntu archive, then runs a masterless
`puppet apply` over `puppet/manifests/site.pp`:

| Class | Effect |
|---|---|
| `portal_base` | apt refresh (with retries), base packages, chrony time sync |
| `portal_java` | `openjdk-21-jdk-headless` for host-level diagnostics (jcmd, jstack) |
| `portal_docker` | Docker CE from Docker's apt repo; json-file logging capped at 20 MB × 5 |
| `portal_users` | `portal` service account (uid/gid 1500), `/opt/portal`, deploy user added to the `docker` group |
| `portal_hardening` | SSH key-only + no root login, sysctl network hardening, `ufw` (22/80/443), unattended security upgrades, `/etc/shadow` permissions |

`--detailed-exitcodes` is used: exit 0 (no changes) and 2 (changes applied) are
success; 4 and 6 fail the stage.

The deploy user is passed in as `DEPLOY_USER` and surfaces to Puppet as the
`deploy_user` fact — never hardcoded, because it derives from the AMI's OS.

### Ansible deployment (stage 7)

`ansible/playbook.yml` uses only `ansible.builtin` modules, so `ansible-core`
needs no galaxy collections installed.

1. Verify Docker is present (proves the Puppet stage did its job).
2. Template `/opt/portal/app.env` (`no_log: true`, mode `0640`).
3. Create the `portal-net` docker network and log in to GHCR.
4. Start PostgreSQL 16 on `portal-net` with a named volume — **no host port
   published**, so the database is unreachable from outside the host.
5. Wait for `pg_isready`.
6. Pull the image tagged with the commit SHA and start the application bound to
   `127.0.0.1:8080`.
7. Poll `/actuator/health` directly until it returns 200.
8. Install nginx, remove the default site, template and enable the portal
   vhost, flush handlers.
9. Re-verify health *through* nginx.

The playbook is idempotent: rerunning it is the normal recovery action.

## Verification

The pipeline verifies automatically (stage 8). Manually:

```bash
IP=$(cd infra && terraform output -raw instance_public_ip)

curl -i http://$IP/actuator/health          # expect HTTP 200, {"status":"UP"}
curl -s http://$IP/v3/api-docs | head -c 200
open http://$IP/swagger-ui.html
```

End-to-end smoke test:

```bash
TOKEN=$(curl -sX POST http://$IP/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<ADMIN_PASSWORD>"}' | jq -r .token)

curl -s http://$IP/api/teams -H "Authorization: Bearer $TOKEN"   # expect []
curl -s -o /dev/null -w '%{http_code}\n' http://$IP/api/teams    # expect 401
```

On the host itself:

```bash
ssh -i <key> <ssh_user>@$IP
docker ps                                          # two containers, healthy
docker logs internal-developer-portal-app --tail 50
sudo nginx -t
sudo systemctl status nginx
```

## Operations

### Deploy a new version

Push to `main`. The image is tagged with the commit SHA; Ansible pulls that
exact tag. There is a short interruption while the container is replaced.

### Roll back

Use the platform's rollback action: it reverts the repository to the last green
deploy's commit and redeploys it. Because the image tag is the commit SHA,
reverting the commit deploys the previously working image, and re-applying the
previous Terraform is the infrastructure rollback.

### Restart the application

```bash
ssh -i <key> <ssh_user>@$IP
sudo docker restart internal-developer-portal-app
```

### Read logs

```bash
sudo docker logs -f internal-developer-portal-app     # application
sudo docker logs -f internal-developer-portal-db      # database
sudo tail -f /var/log/nginx/portal.error.log          # proxy
```

### Back up the database

There is no automated backup at this tier. Manually:

```bash
sudo docker exec internal-developer-portal-db \
  pg_dump -U idp -d idp > portal-$(date +%F).sql
```

### Tear down

Use the platform's destroy action, which runs the rendered destroy workflow
(`terraform destroy` against the same state). The repository and all
configuration survive — redeploying afterwards is just a deploy.

## Troubleshooting

| Symptom | Likely cause | Action |
|---|---|---|
| `provision` fails at `terraform init` | Backend flags drifted | Confirm `-reconfigure` and the bucket/key/region flags; never write import scripts |
| Duplicate-resource errors on retry | Same as above — state was not found | Fix init flags, not the resources |
| `Permission denied (publickey)` | SSH user does not match the AMI family | Ubuntu offers plain `publickey`; a `gssapi` list means an Amazon Linux/RHEL host. Verify `SSH_USER` matches the provisioned image |
| Puppet stage: apt 404s | Stale image package index | `portal_base` refreshes unconditionally with retries; a persistent 404 means a mirror outage |
| `configure` fails pulling the image | GHCR package not visible | Confirm `build_push` pushed, and that the workflow has `packages: write` |
| App health never reaches 200 | Bad `JWT_SECRET` (< 32 chars) or DB unreachable | `docker logs …-app`; the application refuses to start on a short secret |
| `verify` 502 through nginx | App not listening on 127.0.0.1:8080 | `docker ps`, then `sudo nginx -t` and check `portal.error.log` |
| `perf` threshold breach | p95 > 800 ms or error rate ≥ 1% | Read `k6-summary.json`; a cold JVM on the first run is the usual cause |
