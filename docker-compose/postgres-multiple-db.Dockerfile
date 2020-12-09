FROM postgres:10.10
COPY ./create-multiple-postgresql-database.sh /docker-entrypoint-initdb.d/create-multiple-postgresql-database.sh
