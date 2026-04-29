#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
  CREATE DATABASE user_management;
  CREATE DATABASE payments;

  CREATE USER userservice WITH PASSWORD 'userpass123';
  CREATE USER paymentservice WITH PASSWORD 'paymentpass123';

  GRANT ALL PRIVILEGES ON DATABASE user_management TO userservice;
  GRANT ALL PRIVILEGES ON DATABASE payments TO paymentservice;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname="user_management" <<-EOSQL
  GRANT ALL PRIVILEGES ON DATABASE user_management TO userservice;
  GRANT USAGE, CREATE ON SCHEMA public TO userservice;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname="payments" <<-EOSQL
  GRANT ALL PRIVILEGES ON DATABASE payments TO paymentservice;
  GRANT USAGE, CREATE ON SCHEMA public TO paymentservice;
EOSQL