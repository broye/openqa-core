(ns io.oqa.core.service.db.vote
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

(defn- validate-vote
  "Validate vote data"
  [{:keys [pid uid type domain]}]
  (cond
    (str/blank? pid) {:error-code :pid-must-be-provided}
    (str/blank? uid) {:error-code :uid-must-be-provided}
    (str/blank? type) {:error-code :type-must-be-provided}
    (str/blank? domain) {:error-code :domain-must-be-provided}
    (and (not= type "u") (not= type "d")) {:error-code :wrong-type-provided}
    :else {:error-code :ok}))

(defn new-vote
  "Create new vote for a post"
  [{:keys [pid uid type domain] :as vote}]
  (let [{error-code :error-code :as error} (validate-vote vote)]
    (if (not= error-code :ok)
      error
      (try (jdbc/with-db-transaction [conn {:datasource (deref (get @domain-to-connection domain))}]
             (let [pid-uuid (java.util.UUID/fromString pid)
                   vote-get (jdbc/query conn
                                        ["select type from vote where pid = ? And uid = ?" pid-uuid uid])
                   [{type-get :type}] vote-get
                   _ (println "vote-get & type-get" vote-get type-get)]
               (if type-get
                 {:error-code :already-voted :data {:type type-get}}
                 (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))

                       update-post-result (if (= type "u")
                                            (jdbc/execute! conn
                                                           ["update post set upvote_count = upvote_count + 1 where pid = ?" pid-uuid])
                                            (jdbc/execute! conn
                                                           ["update post set downvote_count = downvote_count + 1 where pid = ?" pid-uuid]))
                       _ (when (= (first update-post-result) 0)
                           (throw (ex-info "Parent post not found" {:error-code :parent-not-found})))
                       new-vote-result (jdbc/insert! conn
                                                     :vote
                                                     (conj (dissoc vote :domain) {:last_update now :pid pid-uuid}))]
                   (if (= (count new-vote-result) 1)
                     {:error-code :ok}
                     (throw (ex-info "New vote error" {:error-code :new-vote-failed})))))))
           (catch clojure.lang.ExceptionInfo e (ex-data e))
           (catch Exception e (do (println e) {:error-code :database-error}))
           (catch Throwable e (do (println e) {:error-code :unkown-error}))))))

(defn unvote
  "Un vote for a post"
  [{:keys [pid uid type domain] :as vote}]
  (let [{error-code :error-code :as error} (validate-vote vote)]
    (if (not= error-code :ok)
      error
      (try (jdbc/with-db-transaction [conn {:datasource (deref (get @domain-to-connection domain))}]
             (let [pid-uuid (java.util.UUID/fromString pid)
                   delete-vote-result (jdbc/delete! conn
                                                    :vote
                                                    ["pid = ? And uid = ? And type = ?" pid-uuid uid type])
                   _ (when (= (first delete-vote-result) 0)
                       (throw (ex-info "vote not found not found" {:error-code :vote-not-found})))
                   update-post-result (if (= type "u")
                                        (jdbc/execute! conn
                                                       ["update post set upvote_count = upvote_count - 1 where pid = ?" pid-uuid])
                                        (jdbc/execute! conn
                                                       ["update post set downvote_count = downvote_count - 1 where pid = ?" pid-uuid]))]
               (if (= (first update-post-result) 1)
                 {:error-code :ok}
                 (throw (ex-info "New vote error" {:error-code :unvote-failed})))))
           (catch clojure.lang.ExceptionInfo e (ex-data e))
           (catch Exception e (do (println e) {:error-code :database-error}))
           (catch Throwable e (do (println e) {:error-code :unkown-error}))))))
