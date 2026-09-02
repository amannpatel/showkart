# ShowKart

A distributed real-time seat-booking platform built as a portfolio project. It demonstrates end-to-end distributed-system patterns — microservices, distributed seat locking, transactional outbox, saga orchestration, idempotent workflows, Kafka retry + DLQ, Redis caching — in a domain where those patterns are visibly necessary.

Planning artifacts: [PRD](_bmad-output/planning-artifacts/prds/prd-showkart-2026-09-02/prd.md) · [Architecture Spine](_bmad-output/planning-artifacts/architecture/architecture-showkart-2026-09-02/ARCHITECTURE-SPINE.md) · [Epics & Stories](_bmad-output/planning-artifacts/epics.md).

---

## Prerequisites

- Docker Desktop 4.x with Compose v2 (`docker compose` CLI)
- ~4 GB free RAM for the running stack
- Ports free on your host: 3000, 5432, 6379, 8080, 9090, 9092

Java, Maven, and Node are **not** required to run the stack — everything runs in containers. They are only needed if you plan to work on service code outside Docker.

## One-command startup

```bash
cp .env.example .env
docker compose up -d
```

Within ~2 minutes on a typical dev laptop the entire stack reaches a healthy state:

| Service | URL / port |
|---|---|
| API Gateway (Actuator health) | http://localhost:8080/actuator/health |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (login: `admin` / `change-me-in-local`) |
| Postgres | `localhost:5432` (user: `showkart`) |
| Redis | `localhost:6379` |
| Kafka (host listener) | `localhost:9092` |

Check status with:

```bash
docker compose ps
curl -f http://localhost:8080/actuator/health
```

## What comes up

- **Gateway** (Spring Boot 4.1 + Spring Cloud Gateway 4.3, Java 25) — the only application container in this story. Exposes Actuator health, readiness, liveness, and Prometheus metrics.
- **Postgres 17** — one instance, six logical DBs (`auth_db`, `show_db`, `booking_db`, `payment_db`, `notif_db`, `gateway_db`). Tables land per-service in later stories.
- **Redis 7.4** — reachable but not yet exercised by any service code.
- **Kafka 3.9 in KRaft mode** — three topics pre-created (`booking.events`, `payment.events`, `inventory.events`, 3 partitions each). Auto-topic-create is disabled.
- **Prometheus 2.55** — scrapes the gateway's `/actuator/prometheus` endpoint every 15 s.
- **Grafana 11** — provisioned with Prometheus as the default datasource. Dashboards land in Epics 4 and 6.

## Repository layout

```
showkart/
├── pom.xml                       # Maven aggregator (Spring Boot 4.1 parent, Java 25)
├── common/                       # Shared utilities (populated in Epic 5)
├── services/
│   ├── gateway/                  # Spring Cloud Gateway — Story 1.1 (this one)
│   ├── auth-service/             # Skeleton — Epic 1 Stories 1.2, 1.3
│   ├── show-service/             # Skeleton — Epics 2, 3, 4
│   ├── booking-service/          # Skeleton — Epic 5
│   ├── payment-service/          # Skeleton — Epic 5
│   └── notification-worker/      # Skeleton — Epic 6
├── infra/
│   ├── postgres/init/            # DB provisioning at container init
│   ├── kafka/                    # Topic-creation script
│   ├── prometheus/               # Scrape config
│   └── grafana/provisioning/     # Datasources (dashboards later)
├── docker-compose.yml
├── .env.example                  # copy to .env
└── _bmad-output/                 # planning artifacts (gitignored)
```

## Building services locally (optional)

Requires Java 25 + Maven 3.9+ on `PATH`.

```bash
./mvnw -DskipTests -q package
```

Only `gateway` has a real executable in Story 1.1; the other service modules are Maven scaffolds with no `@SpringBootApplication` yet.

## Shutting down

```bash
docker compose down          # stop containers, keep volumes
docker compose down -v       # nuke volumes too (fresh start next time)
```

## Troubleshooting

- **A container is `unhealthy`** — inspect with `docker compose logs <service>`. Most first-run failures are missing `.env` values or port conflicts on the host.
- **Kafka won't start** — the KRaft cluster stores its `CLUSTER_ID` in the `kafka_data` volume; if you change compose config incompatibly, `docker compose down -v` clears state.
- **Gateway healthcheck fails** — the gateway can take 20–30 s on first boot for Spring's warmup. Give it up to 90 s before assuming failure.
- **Windows line endings on `create-topics.sh`** — if you're on Windows and the `kafka-init` container fails with `not found`, make sure the file uses LF line endings (`git config core.autocrlf false` in the repo, or `dos2unix` it).

## Next steps

Follow the [epic slate](_bmad-output/planning-artifacts/epics.md). Story 1.2 (user registration) unlocks the real auth path.
