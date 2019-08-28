(ns io.oqa.core.service.db.query
  (:import (io.vertx.pgclient
            PgConnectOptions
            PgPool)
           (io.vertx.sqlclient PoolOptions)
           (io.vertx.core Handler)
           (java.util UUID))

  (:require [io.oqa.core.service.db :refer [domain-to-connection]]
            [clj-postgresql.core :as pg]
            [honeysql.format :as fmt]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [io.oqa.core.bootstrap.config :as config]
            [clojure.string :as str]))

(defmethod fmt/fn-handler "any" [_ field value]
  (str  (fmt/to-sql value) " = ANY( " (fmt/to-sql field) " )"))

(defn- validate-query-vote
  "Validate query vote options"
  [{:keys [pid from_seq size uid domain]}]
  (cond
    (str/blank? domain) {:error-code :domain-must-be-provided}
    (str/blank? pid) {:error-code :pid-must-be-provided}
    (and (str/blank? uid) (nil? size)) {:error-code :size-must-be-provided}
    :else {:error-code :ok}))

(defn query-vote
  "Query vote from postgresql"
  [{:keys [pid from_seq size uid domain] :as query_options}]
  (let [error (validate-query-vote query_options)]
    (if (not= (:error-code error) :ok)
      error
      (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
             (let [sql-command (sql/format {:select [:*]
                                            :from [:vote]
                                            :where [:and
                                                    [:= :pid (java.util.UUID/fromString pid)]
                                                    (if (str/blank? uid)
                                                      nil
                                                      [:= :uid uid])
                                                    (if (and (nil? from_seq) (str/blank? uid))
                                                      nil
                                                      [:>= :seq_id (or from_seq 0)])]
                                            :limit (or (and size (inc size)) 1)
                                            :order-by [[:seq_id :asc]]})
                   _ (println "sql-command is >>>>>>>> " sql-command)
                   query-result (jdbc/query conn sql-command)]
               (println (count query-result))
               (if (str/blank? uid)
                 (if (<= (count query-result) size)
                   {:error-code :ok
                    :has-more :false
                    :data query-result}
                   {:error-cde :ok
                    :has-more :true
                    :data (take size query-result)})
                 {:error-code :ok
                  :has-more false
                  :data query-result})))
           (catch Exception e (do (println e) {:error-code :database-error}))
           (catch Throwable e (do (println e) {:error-code :unkown-error}))
           ))))


(defn- validate-query-post
  "Validate query vote options"
  [{:keys [domain]}]
  (cond
    (str/blank? domain) {:error-code :domain-must-be-provided}
    :else {:error-code :ok}))

(defn query-post
  "Query vote from postgresql"
  [{:keys [
           domain
           folder
           fields
           from_create_date
           from_last_active
           from_last_active_seq
           from_seq
           pid
           size
           status
           topic
           type
           uid
           tag
           meta_tag
           fields
           order_by
           ] :as query_options}]
  (let [error (validate-query-post query_options)
        size (or size 20)]
    (if (not= (:error-code error) :ok)
      error
      (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
             (let [sql-command (sql/format {:select (or fields [:*])
                                            :from [:post]
                                            :where (if (not (str/blank? pid))
                                                     [:= :pid (java.util.UUID/fromString pid)]
                                                     [:and
                                                      [:= :domain domain]
                                                      (if (str/blank? folder)
                                                        nil
                                                        [:= :folder folder])
                                                      (if (str/blank? status)
                                                        nil
                                                        [:= :status status])
                                                      (if (str/blank? type)
                                                        nil
                                                        [:= :type type])
                                                      (if (str/blank? uid)
                                                        nil
                                                        [:= :uid uid])
                                                      (if (nil? from_seq)
                                                        nil
                                                        [:< :seq_id from_seq])
                                                      (if (nil? from_last_active_seq)
                                                        nil
                                                        [:< :last_active_seq from_last_active_seq])
                                                      (if (nil? tag)
                                                        nil
                                                        [:any :tags tag ])
                                                      (if (nil? meta_tag)
                                                        nil
                                                        [:any :meta_tags meta_tag ])
                                                      ]
                                                     )
                                            :limit (or (and pid 1) (inc size))
                                            :order-by [(cond
                                                         (not (nil? order_by)) [order_by :desc]
                                                         (not (nil? from_seq)) [:last_active_seq :desc]
                                                         :else [:last_active_seq :desc])]})
                   _ (println sql-command)
                   query-result (jdbc/query conn sql-command)]
               (if (str/blank? pid)
                 (if (<= (count query-result) (or size 20)) ;; query all
                   {:error-code :ok
                    :has-more :false
                    :data query-result}
                   {:error-cde :ok
                    :has-more :true
                    :data (take size query-result)})
                 {:error-code :ok ;; get a specifc pid
                  :has-more false
                  :data query-result})))
           (catch Exception e (do (println e) {:error-code :database-error}))
           (catch Throwable e (do (println e) {:error-code :unkown-error}))
           ))))


(defn- validate-query-stats
  "Validate query stats options"
  [{:keys [domain]}]
  (cond
    (str/blank? domain) {:error-code :domain-must-be-provided}
    :else {:error-code :ok}))


(defn query-stats
  "Query stats from postgresql"
  [{:keys [domain topic size from_seq folder] :as query_options}]
  (let [error (validate-query-stats query_options)
        size (or size 200)]
    (if (not= (:error-code error) :ok)
      error
      (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
             (let [sql-command (sql/format {:select [:*]
                                            :from [:stats]
                                            :where [:and
                                                    [:= :domain domain]
                                                    (if (str/blank? topic)
                                                      nil
                                                      [:= :topic topic])
                                                    (if (str/blank? folder)
                                                      nil
                                                      [:= :folder folder])
                                                    (if (nil? from_seq)
                                                      nil
                                                      [:>= :seq_id from_seq])]
                                            :limit (inc size)
                                            :order-by [[:seq_id :desc]]})
                   query-result (jdbc/query conn sql-command)]
               (println (count query-result))
               (if (<= (count query-result) size)
                 {:error-code :ok
                  :has-more :false
                  :data query-result}
                 {:error-cde :ok
                  :has-more :true
                  :data (take size query-result)})))
           (catch Exception e (do (println e) {:error-code :database-error}))
           (catch Throwable e (do (println e) {:error-code :unkown-error}))
           ))))

(defn query-domain
  "Query stats from postgresql"
  []
  (let [config @config/config
        Default (:Default config)
        default-domain (:domain Default)]
    (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection default-domain))}]
           (let [sql-command (sql/format {:select [:*]
                                          :from [:domain_shard]
                                          :order-by [[:last_update :desc]]})
                 query-result (jdbc/query conn sql-command)]
             {:error-code :ok
              :has-more :false
              :data query-result}))
         (catch Exception e (do (println e) {:error-code :database-error}))
         (catch Throwable e (do (println e) {:error-code :unkown-error})))))
