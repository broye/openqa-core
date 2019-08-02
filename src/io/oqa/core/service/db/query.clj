(ns io.oqa.core.service.db.query
  (:import (io.vertx.pgclient
            PgConnectOptions
            PgPool)
           (io.vertx.sqlclient PoolOptions)
           (io.vertx.core Handler)
           (java.util UUID))

  (:require [io.oqa.core.service.db :refer [domain-to-connection]]
            [clj-postgresql.core :as pg]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]))

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
                                                      [:>= :seq_id from_seq])]
                                            :limit (or (and size (inc size)) 1)
                                            :order-by [[:seq_id :asc]]})
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
                  :data query-result})))))))
