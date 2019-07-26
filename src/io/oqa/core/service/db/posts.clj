(ns io.oqa.core.service.db.posts
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


(defn- validate-new-post
  "Validate new post data"
  [ {:keys [title
            pid
            content
            content_lang
            draft_title
            draft_content
            draft_content_lang
            domain
            type
            qid
            aid
            uid
            user_name
            user_avatar
            status]
     }]
  (cond
    (nil? domain) {:error-code :domain-not-provided}
    (nil? (get @domain-to-connection domain))   {:error-code :domain-not-found} ;; domain error
    (or (str/blank? uid) (str/blank? user_name)) {:error-code :uid-username-required}
    (and (not= status "p") (not= status "i")) {:error-code :invalid-status-code}
    (= type "q") (cond
                   (str/blank? title) {:error-code :empy-title}
                   (and (= status "i") (str/blank? draft_title)) {:error-code :empy-draft-title}
                   :else {:error-code :ok})
    (= type "a") (cond
                   (str/blank? qid) {:error-code :empty-question-id}
                   (str/blank? title) {:error-code :empy-title}
                   (and (= status "i") (str/blank? draft_content)) {:error-cde :empty-draft-content}
                   (and (= status "p") (str/blank? content)) {:error-code :empty-content}
                   :else {:error-code :ok})
    (= type "c") (cond
                   (str/blank? qid) {:error-code :empty-question-id}
                   (and (= status "i") (str/blank? draft_content)) {:error-cde :empty-draft-content}
                   (and (= status "p") (str/blank? content)) {:error-code :empty-content}
                   :else {:error-code :ok})
    :else {:error-code :unkown-post-type}))

(defn new-post
  "Create new post"
  [ {:keys [title
            pid
            content
            content_lang
            draft_title
            draft_content
            draft_content_lang
            domain
            type
            qid
            aid
            uid
            user_name
            user_avatar
            status]
     :as post
     }]
  (let [{error-code :error-code :as error} (validate-new-post post)
        now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
        post (-> post
                 (assoc :create_date now
                        :last_update now
                        :qid (if qid (java.util.UUID/fromString qid) qid)
                        :aid (if aid (java.util.UUID/fromString aid) aid)
                        :pid (if pid (java.util.UUID/fromString pid) (java.util.UUID/randomUUID)))
                 (dissoc :seq_id))
        {:keys [pid
                qid
                aid] ;; rebind converted UUID format pid/qid/aid
         } post]
    (if (not= error-code :ok) ;;check validate result
      error
      (if  (= type "q")
        ;; question... straight forward insert.
        (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
               (let [result (jdbc/insert! conn
                                          :post
                                          post
                                          :return-keys ["pid" "seq_id"])]
                 (if ( or (nil? result) (empty? result) (nil? (:pid (first result))) )
                   {:error-code :insert-failed}
                   {:error-code :ok
                    :data {:pid (:pid (first result))
                           :seq_id (:seq_id (first result))
                           :last_update now
                           :create_date now}})))
             (catch Exception e ((println e) {:error-code :database-error}))
             (catch Throwable e ((println e) {:error-code :unkown-error})))

        ;; answer or comment... update parent count
        (try (jdbc/with-db-transaction [conn {:datasource (deref (get @domain-to-connection domain))}]
               (let [insert-result (jdbc/insert! conn
                                                 :post
                                                 post
                                                 :return-keys ["pid" "seq_id"])
                     update-result (cond
                                     ;; is answer
                                     (= type "a") (jdbc/execute! conn
                                                                 ["update post set answers_count = answers_count + 1 where pid = ?" qid])
                                     ;; is comment to question
                                     (and
                                      (= type "c")
                                      (nil? aid)) (jdbc/execute!
                                                   conn
                                                   ["update post set comments_count = comments_count + 1 where pid = ?" qid])
                                     ;; is comment to answer
                                     (= type "c") (jdbc/execute!
                                                   conn
                                                   ["update post set comments_count = comments_count + 1 where pid = ?" aid])
                                     )
                     _ (println "update result" update-result)]
                 (let [[count] update-result]
                   (when (= count 0)
                     (throw (ex-info "Parent post not found" {:error-code :parent-not-found}))))
                 (if ( or (nil? insert-result) (empty? insert-result) (nil? (:pid (first insert-result))) )
                   {:error-code :insert-failed}
                   {:error-code :ok
                    :data {:pid (:pid (first insert-result))
                           :seq_id (:seq_id (first insert-result))
                           :last_update now
                           :create_date now}})))
             (catch clojure.lang.ExceptionInfo e (ex-data e))
             (catch Exception e (do (println e) {:error-code :database-error}))
             (catch Throwable e (do (println) e) {:error-code :unkown-error} ))))))
