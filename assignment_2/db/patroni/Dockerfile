# Postgres managed by Patroni instead of the vanilla docker-entrypoint.sh used under db/primary
# and db/replica: Patroni owns initdb/cloning/promotion itself and coordinates over etcd (the
# same cluster already used for service discovery and load-balancer leader election), giving
# automatic failover instead of the manual `pg_promote()` used elsewhere in this project.
FROM postgres:16
RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 python3-pip \
 && pip3 install --break-system-packages --no-cache-dir "patroni[etcd3]" psycopg2-binary \
 && apt-get purge -y python3-pip \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/*
COPY patroni.yml /config/patroni.yml
COPY post-bootstrap.sh /scripts/post-bootstrap.sh
COPY on_role_change.py /scripts/on_role_change.py
RUN chmod +x /scripts/post-bootstrap.sh /scripts/on_role_change.py
USER postgres
ENTRYPOINT ["patroni"]
CMD ["/config/patroni.yml"]
