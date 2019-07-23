(ns io.oqa.core.service.db
  (:import (io.vertx.pgclient
            PgConnectOptions
            PgPool)
           (io.vertx.sqlclient PoolOptions)
           (io.vertx.core Handler)))


;; domain to connection mapping.
;; Could be updated in different threads
(def ^{:private true} domain-to-connection (ref {}))

(defn initiate-pool
  "Initiate a connection pool with postgresql"
  [{:keys [host port user password db poolSize]}]
  (println host port user password db poolSize)
  (let [poolOptions (. (PoolOptions.) setMaxSize (or poolSize 5))
        connectOptions (.. (PgConnectOptions.)
                           (setPort (or port 5432))
                           (setDatabase (or db "oqa"))
                           (setUser user)
                           (setPassword password))]
    (println "creating pool...")
    (let [client (. PgPool pool connectOptions poolOptions)]
      (println "client created" client)
      client)))

(defn init-db-service
  "Initiate db services for all postgresql db opertions"
  [Default Shards]
  (when-not (and (not (nil? Default)) (:domain Default) (:shard Default))
    (throw (new Exception "Config entry 'Default' with both keys domain and shard must be defined")))
  (println "Initating db services...")
  (let [defaultShard (:shard Default)
        defaultShardConfig ((keyword defaultShard) Shards)
        defaultShardPool (initiate-pool defaultShardConfig)]
    (dosync
     (alter domain-to-connection assoc (Default :domain) defaultShardPool))
    (println "building domain to client mapping")
    (. defaultShardPool
       query "select domain, shard from domain_shard"
       (reify Handler
         (handle [this ar]
           (if (. ar succeeded)
             (let [result (. ar result)]
               (when (> (. result size) 0)
                 (println "building shard connections..."))
               (doseq [[domain shard] (iterator-seq (. result iterator))]
                 (let [shardConfig ((keyword shard) Shards)]
                   (println shardConfig)
                   (when (nil? shardConfig)
                     (println "Shard not found")
                     (throw "Shard config not found"))
                   (let [pool (initiate-pool shardConfig)]
                     (println "pool is: " pool)
                     (when pool
                       (dosync
                        (alter domain-to-connection assoc domain pool))))
                   (println "domain-to-connection" @domain-to-connection))))
             (println "failure" (.. ar cause getMessage))))))))
