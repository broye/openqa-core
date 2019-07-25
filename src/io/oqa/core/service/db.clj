(ns io.oqa.core.service.db
  (:require [clj-postgresql.core :as pg]
            [hikari-cp.core :refer :all]
            [clojure.java.jdbc :as jdbc]))

;; domain to connection mapping.
;; Could be updated in different threads
(def domain-to-connection (ref {}))

(defn initiate-pool
  "Initiate a connection pool with postgresql"
  [{:keys [host port user password db poolSize]}]
  (println host port user password db poolSize)
  (def datasource-options {:auto-commit        true
                           :read-only          false
                           :validation-timeout 5000
                           :idle-timeout       600000
                           :max-lifetime       1800000
                           :minimum-idle       10
                           :adapter            "postgresql"
                           :maximum-pool-size (or poolSize 10)
                           :username user
                           :password password
                           :database-name db
                           :server-name  (or host "localhost")
                           :port-number  (or port  5432)
                           :register-mbeans    false})
  (delay (make-datasource datasource-options)))

(defn init-db-service
  "Initiate db services for all postgresql db opertions"
  [Default Shards]
  (when-not (and (not (nil? Default)) (:domain Default) (:shard Default))
    (throw (new Exception "Config entry 'Default' with both keys domain and shard must be defined")))
  (println "Initating db services...")
  (let [defaultShard (:shard Default)
        defaultShardConfig ((keyword defaultShard) Shards)
        defaultShardPool (initiate-pool defaultShardConfig)
        _ (println "default shard pool is... " defaultShardPool)]
    (dosync
     (alter domain-to-connection assoc (Default :domain) defaultShardPool))
    (println "building domain to client mapping")
    (jdbc/with-db-connection [conn {:datasource @defaultShardPool}]
      (let [result (jdbc/query conn "select domain, shard from domain_shard")]
        (println "domin shard fetched " result)
        (doseq [{:keys [domain shard]} result]
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
            (println "domain-to-connection" @domain-to-connection)))))))
