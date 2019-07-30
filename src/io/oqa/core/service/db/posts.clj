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
      (if  (or  (= type "q") (= status "i"))
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

(defn- validate-update-post
  "validate an update post's fields"
  [{:keys [status type domain pid qid aid seq_id]}]
  (cond
    (not (str/blank? status)) { :error-code :can-not-modify-status}
    (not (str/blank? type)) { :error-code :can-not-modify-type}
    (str/blank? pid) { :error-code :pid-must-be-provided}
    (not (str/blank? qid)) { :error-code :can-not-update-qid}
    (not (str/blank? aid)) { :error-code :can-not-update-aid}
    (not (str/blank? seq_id)) { :error-code :can-not-update-seq_id}
    (str/blank? domain) { :error-code :domain-must-be-provided}
    :else { :error-code :ok}))

(defn update-post
  "Update a post fields except changing status and type"
  [{pid :pid domain :domain :as post}]
  (let [error (validate-update-post post)]
    (if (not= (:error-code error) :ok)
      error
      (try
        (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
              post (-> post
                       (dissoc :qid
                               :aid
                               :pid
                               :domain
                               :status
                               :type
                               :seq_id
                               :create_date) ;; remove un-modifiable fields
                       (assoc :last_update now))
              count (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
                      (jdbc/update! conn
                                    :post
                                    post
                                    ["pid = ? And domain = ? " (java.util.UUID/fromString pid) domain]))]
          (if (= (first count) 1)
            {:error-code :ok}
            {:error-code :post-not-found}))
        (catch Exception e (do (println e) {:error-code :database-error}))
        (catch Throwable e (do (println e) {:error-code :unknown-error}))))))


(defn publish-post
  "Change a post's status from i to p"
  [{:keys [pid qid aid domain type] :as post}]
  (try
    (if (= type "q")
      ;; question - change status to 'p' directly
      (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
            count (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
                    (jdbc/update! conn
                                  :post
                                  {:status "p" :last_update now} ;; change status only
                                  ["pid = ? And domain = ? And status = ? And type = ?" (java.util.UUID/fromString pid) domain "i" type]))]
        (if (= (first count) 1)
          {:error-code :ok}
          {:error-code :post-not-found}))

      ;; not question - update parent stat as well
      (jdbc/with-db-transaction [conn {:datasource (deref (get @domain-to-connection domain))}]
        (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
              update-result (jdbc/update! conn
                                          :post
                                          {:status "p" :last_update now} ;; change status only
                                          ["pid = ? And domain = ? And status = ? And type = ?" (java.util.UUID/fromString pid) domain "i" type])
              _ (when (= (first update-result) 0)
                  (throw (ex-info "Parent post not found" {:error-code :publishi-failed})))
              parent-update-result (cond
                                     ;; is answer
                                     (= type "a") (jdbc/execute! conn
                                                                 ["update post set answers_count = answers_count + 1 where pid = ?" (java.util.UUID/fromString qid)])
                                     ;; is comment to question
                                     (and
                                      (= type "c")
                                      (nil? aid)) (jdbc/execute!
                                                   conn
                                                   ["update post set comments_count = comments_count + 1 where pid = ?" (java.util.UUID/fromString qid)])
                                     ;; is comment to answer
                                     (= type "c") (jdbc/execute!
                                                   [conn
                                                    "update post set comments_count = comments_count + 1 where pid = ?" (java.util.UUID/fromString aid)]))
              _ (when (or (nil? parent-update-result) (empty? parent-update-result))
                  (throw (ex-info "Parent post not found" {:error-code :publishi-failed})))]
          {:error-code :ok
           :data {:last_update now}})))
    (catch clojure.lang.ExceptionInfo e (do (println e) (ex-data e)))
    (catch Exception e (do (println e) {:error-code :database-error}))
    (catch Throwable e (do (println e) {:error-code :unknown-error}))))

(defn mark-post-deleted
  "Mark post status to seleted but not pysically delete the post. Client must explicitly set fields to blank if necessary"
  [{pid :pid domain :domain :as post}]
  (try
    (println pid domain post)
    (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
          post (-> post
                   (dissoc :qid
                           :aid
                           :pid
                           :domain
                           :type
                           :seq_id
                           :create_date) ;; remove un-modifiable fields
                   (assoc :last_update now :status "d"))
          count (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
                  (jdbc/update! conn
                                :post
                                post
                                ["pid = ? And domain = ? And status != ?" (java.util.UUID/fromString pid) domain "d"]))]
      (if (= (first count) 1)
        {:error-code :ok}
        {:error-code :post-not-found}))
    (catch Exception e (do (println e) {:error-code :database-error}))
    (catch Throwable e (do (println e) {:error-code :unknown-error}))))


(defn mark-post-revising
  "Mark post status to revising. Client must explicitly set draft fields"
  [{pid :pid domain :domain :as post}]
  (try
    (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
          post (-> post
                   (dissoc :qid
                           :aid
                           :pid
                           :domain
                           :type
                           :seq_id
                           :create_date) ;; remove un-modifiable fields
                   (assoc :last_update now :status "r"))
          count (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
                  (jdbc/update! conn
                                :post
                                post
                                ["pid = ? And domain = ? And status = ?" (java.util.UUID/fromString pid) domain "p"]))]
      (if (= (first count) 1)
        {:error-code :ok}
        {:error-code :post-not-found}))
    (catch Exception e (do (println e) {:error-code :database-error}))
    (catch Throwable e (do (println e) {:error-code :unknown-error}))))
