(ns io.oqa.core.service.db.user-info
  (:import (io.vertx.pgclient
            PgConnectOptions
            PgPool)
           (io.vertx.sqlclient PoolOptions)
           (io.vertx.core Handler)
           (java.util UUID))

  (:require [io.oqa.core.service.db :refer [domain-to-connection]]
            [io.oqa.core.service.db.helpers :as helpers]
            [clj-postgresql.core :as pg]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]))

(defn update-user-info
  "Update user name and avartar accross post & vote"
  [{:keys [uid user_name user_avatar domain] :as user_info}]
  (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
         (let [update-post-result (jdbc/update! conn :post
                                                (dissoc user_info :uid :domain)
                                                ["uid = ?" uid])
               update-vote-result (jdbc/update! conn :vote
                                                (dissoc user_info :uid :domain)
                                                ["uid = ?" uid])]
           {:error-code :ok}))
       (catch Exception e (do (println e) {:error-code :database-error}))
       (catch Throwable e (do (println e) {:error-code :unkown-error}))))
