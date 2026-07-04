# syntax=docker/dockerfile:1
FROM postgres:16-alpine

LABEL org.opencontainers.image.title="apex-karting-db" \
      org.opencontainers.image.description="PostgreSQL БД клиентского API картинг-центра «Апекс»" \
      org.opencontainers.image.licenses="Proprietary"

COPY migrations/*.sql /docker-entrypoint-initdb.d/

HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=10 \
  CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1