(ns io.oqa.core.service.db
  (:import (io.vertx.pgclient PgConnectOptions)))

(defn init-db-service [Default Shards]
  (when (or (nil? Default) (:domain Default) (:shard Default))
    (throw (new Exception "Config entry 'Default' with both keys domain and shard must be defined"))
    )

  )
