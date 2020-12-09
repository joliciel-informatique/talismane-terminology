FROM flyway/flyway
ADD migration /flyway/sql

ENTRYPOINT ["/bin/sh", "-c"]
CMD ["/flyway/flyway -url=jdbc:postgresql://${POSTGRES_HOST?}:${POSTGRES_PORT?}/${POSTGRES_DATABASE?} -user=${POSTGRES_USER?} -password=${POSTGRES_PASSWORD?} migrate"]