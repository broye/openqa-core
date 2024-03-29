(ns io.openqa.core.service.db.domain
  (:import (io.vertx.pgclient
            PgConnectOptions
            PgPool)
           (io.vertx.sqlclient PoolOptions)
           (io.vertx.core Handler)
           (java.sql SQLException)
           (java.util UUID))

  (:require [io.openqa.core.service.db :refer [domain-to-connection]]
            [io.openqa.core.service.db.helpers :as helpers]
            [clj-postgresql.core :as pg]
            [clojure.java.jdbc :as jdbc]
            [io.openqa.core.bootstrap.config :as config]
            [clojure.string :as str]))

(defn- validate-domain
  "Validate domain data"
  [{:keys [domain shard]}]
  (let [config @config/config
        Default (:Default config)
        default-domain (:domain Default)
        shards (:Shards config)]
    (cond
      (str/blank? domain) {:error-code :domain-must-be-provided}
      (str/blank? shard) {:error-code :shard-must-be-provided}
      (= domain default-domain) {:error-code :can-not-use-default-domain-name}
      (not (contains? (into {} shards) (keyword shard))) {:error-code :shard-not-valid}
      :else {:error-code :ok})))

(defn new-domain
  "Create new domain with shard configuration"
  [{:keys [domain shard] :as domain-shard}]
  (let [config @config/config
        Default (:Default config)
        default-domain (:domain Default)
        shards (:Shards config)
        {error-code :error-code :as error} (validate-domain domain-shard)]
    (if (not= error-code :ok)
      error
      (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection default-domain))}]
             (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
                   new-domain-result (jdbc/insert! conn
                                                   :domain_shard
                                                   (into domain-shard {:last_update now :create_date now}))]
               (if (= (count new-domain-result) 1)
                 {:error-code :ok}
                 (throw (ex-info "New domain error" {:error-code :new-domain-failed})))))
           (catch clojure.lang.ExceptionInfo e (ex-data e))
           (catch SQLException e (do (println e) {:error-code :database-error :db-error-code (. e getErrorCode) :db-error-state (. e getSQLState)}))
           (catch Throwable e (do (println e) {:error-code :unkown-error}))))))

(defn update-domain
  "Update a domain"
  [{:keys [domain shard] :as domain-shard}]
  (let [config @config/config
        Default (:Default config)
        default-domain (:domain Default)
        shards (:Shards config)
        {error-code :error-code :as error} (validate-domain domain-shard)]
    (if (not= error-code :ok)
      error
      (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection default-domain))}]
             (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
                   update-domain-result (jdbc/update! conn
                                                      :domain_shard
                                                      (into domain-shard {:last_update now})
                                                      ["domain = ?" domain])
                   update-count (first update-domain-result)]
               (cond
                 (= update-count 1) {:error-code :ok}
                 (= update-count 0) {:error-code :domain-not-found}
                 :else (throw (ex-info "update domain error" {:error-code :update-domain-failed})))))
           (catch clojure.lang.ExceptionInfo e (ex-data e))
           (catch Exception e (do (println e) {:error-code :database-error}))
           (catch Throwable e (do (println e) {:error-code :unkown-error}))))))

(defn delete-domain
  "Delete a domain"
  [domain]
  (let [config @config/config
        Default (:Default config)
        default-domain (:domain Default)]
    (if (= domain default-domain)
      {:error-code :can-not-remove-default-domain}
      (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection default-domain))}]
             (let [delete-domain-result (jdbc/delete! conn
                                                      :domain_shard
                                                      ["domain = ?" domain])
                   delete-count (first delete-domain-result)]
               (cond
                 (= delete-count 1) {:error-code :ok}
                 (= delete-count 0) {:error-code :domain-not-found}
                 :else (throw (ex-info "Delete domain error" {:error-code :delete-domain-failed})))))
           (catch clojure.lang.ExceptionInfo e (ex-data e))
           (catch Exception e (do (println e) {:error-code :database-error}))
           (catch Throwable e (do (println e) {:error-code :unkown-error}))))))
