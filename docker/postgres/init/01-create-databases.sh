#!/bin/sh
# Creates the databases catalogo and turnos, each owned by its own user, on the first start of the volume.
# PUBLIC loses CONNECT and TEMPORARY on both, so each user reaches only its own database (the owner keeps them).
# The credentials come from the environment of the container (compose.yaml). psql quotes them (:"name" as an
# identifier, :'name' as a literal), so a quote in a password cannot break the SQL.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    --set=catalogo_user="$CATALOGO_DB_USERNAME" --set=catalogo_password="$CATALOGO_DB_PASSWORD" \
    --set=turnos_user="$TURNOS_DB_USERNAME" --set=turnos_password="$TURNOS_DB_PASSWORD" <<'SQL'
CREATE ROLE :"catalogo_user" LOGIN PASSWORD :'catalogo_password';
CREATE DATABASE catalogo OWNER :"catalogo_user";
REVOKE CONNECT, TEMPORARY ON DATABASE catalogo FROM PUBLIC;
CREATE ROLE :"turnos_user" LOGIN PASSWORD :'turnos_password';
CREATE DATABASE turnos OWNER :"turnos_user";
REVOKE CONNECT, TEMPORARY ON DATABASE turnos FROM PUBLIC;
SQL
