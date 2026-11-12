#!/bin/sh
set -e

sed '/^CREATE DATABASE/d' /scripts/product_db.sql \
  | psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB"
