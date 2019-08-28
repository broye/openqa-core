(ns io.openqa.core.service.db.user-info
  (:import (io.vertx.pgclient
            PgConnectOptions
            PgPool)
           (io.vertx.sqlclient PoolOptions)
           (io.vertx.core Handler)
           (java.util UUID))

  (:require [io.openqa.core.service.db :refer [domain-to-connection]]
            [io.openqa.core.service.db.helpers :as helpers]
            [clj-postgresql.core :as pg]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]))

(defn- validate-update-user
  "Validate new post data"
  [ {:keys [uid
            user_name
            user_avatar
            domain]
     }]
  (cond
    (nil? domain) {:error-code :domain-not-provided}
    (nil? (get @domain-to-connection domain))   {:error-code :domain-not-found} ;; domain error
    (str/blank? uid) {:error-code :uid-required}
    (and (str/blank? user_name) (str/blank? user_avatar)) {:error-code :username-or-avatar-required}
    :else {:error-code :ok}))

(defn update-user-info
  "Update user name and avartar accross post & vote"
  [{:keys [uid user_name user_avatar domain] :as user_info}]
  (let [{error-code :error-code :as error} (validate-update-user user_info)]
    (if (not= error-code :ok)
      error
      (try (jdbc/with-db-transaction [conn {:datasource (deref (get @domain-to-connection domain))}]
             (let [update-post-result (jdbc/update! conn :post
                                                    (dissoc user_info :uid :domain)
                                                    ["uid = ?" uid])
                   update-vote-result (jdbc/update! conn :vote
                                                    (dissoc user_info :uid :domain)
                                                    ["uid = ?" uid])
                   update-reply-to-result (jdbc/update! conn :post
                                                        (-> (dissoc user_info :uid :domain :user_name :user_avatar)
                                                            (assoc :reply_to_user_name user_name
                                                                   :reply_to_user_avatar user_avatar))
                                                        ["reply_to_uid = ?" uid])
                   ]
               {:error-code :ok}))
           (catch Exception e (do (println e) {:error-code :database-error}))
           (catch Throwable e (do (println e) {:error-code :unkown-error}))))))
