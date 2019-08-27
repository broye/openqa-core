(ns io.oqa.core.service.db.posts
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
            folder
            topic
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
                        :last_active now
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
      (if  (= status "i")
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
               (let [insert-result (jdbc/insert! conn          ;; insert post
                                                 :post
                                                 post
                                                 :return-keys ["pid" "seq_id"])
                     update-result (cond  ;; update stats
                                     ;; is question
                                     (= type "q") (let [sql-command (sql/format {:update :stats
                                                                                 :set {:question_count (sql/call :+ :question_count 1)}
                                                                                 :where [:and [:= :domain domain] [:= :topic topic] [:= :folder folder]]})
                                                        _ (println sql-command)
                                                        result (jdbc/execute! conn
                                                                              sql-command)]
                                                    (println "update stats ..." result domain topic folder)
                                                    (if (= (first result) 1)
                                                      result
                                                      (let [result (jdbc/insert! conn
                                                                                 :stats
                                                                                 {:domain domain
                                                                                  :topic topic
                                                                                  :folder folder
                                                                                  :question_count 1}
                                                                                 :return-keys ["pid"]
                                                                                 )]
                                                        (if (or (nil? result) (empty? result) )
                                                          [0]
                                                          [1]))))

                                     ;; is answer
                                     (= type "a") (jdbc/execute! conn
                                                                 ["update post set answers_count = answers_count + 1, last_active = ?, last_active_seq = nextval('post_last_active_seq_seq')  where pid = ?" now qid])
                                     ;; is comment to question
                                     (and
                                      (= type "c")
                                      (nil? aid)) (jdbc/execute!
                                                   conn
                                                   ["update post set comments_count = comments_count + 1 , last_active = ?, last_active_seq = nextval('post_last_active_seq_seq') where pid = ?" now qid])
                                     ;; is comment to answer
                                     (= type "c") (do (jdbc/execute! ;; update parent answer last active
                                                       conn
                                                       ["update post set comments_count = comments_count + 1, last_active = ?, last_active_seq = nextval('post_last_active_seq_seq') where pid = ?" now aid])
                                                      (jdbc/execute! ;; update parent question last active
                                                       conn
                                                       ["update post set last_active = ?, last_active_seq = nextval('post_last_active_seq_seq') where pid = ?" now qid])))]
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
    (str/blank? pid) { :error-code :pid-must-be-provided}
    (str/blank? type) { :error-code :type-must-be-provided}
    (not (str/blank? qid)) { :error-code :can-not-update-qid}
    (not (str/blank? aid)) { :error-code :can-not-update-aid}
    (not (str/blank? seq_id)) { :error-code :can-not-update-seq_id}
    (str/blank? domain) { :error-code :domain-must-be-provided}
    :else { :error-code :ok}))

(defn update-post
  "Update a post fields except changing status and type"
  [{:keys [pid domain type folder] :as post}]
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
                       (assoc :last_update now :last_active now))
              count (jdbc/with-db-transaction [conn {:datasource (deref (get @domain-to-connection domain))}]
                      (when (and (= type "q") (not (str/blank? (:title post))))
                        (jdbc/update! conn
                                      :post
                                      {:title (:title post)}
                                      ["qid = ? And domain = ? " (java.util.UUID/fromString pid) domain]))
                      (jdbc/update! conn
                                    :post
                                    post
                                    ["type = ? And pid = ? And domain = ? " type (java.util.UUID/fromString pid) domain]))]
          (if (= (first count) 1)
            {:error-code :ok}
            {:error-code :post-not-found}))
        (catch Exception e (do (println e) {:error-code :database-error}))
        (catch Throwable e (do (println e) {:error-code :unknown-error}))))))

(defn publish-post
  "Change a post's status from i to p"
  [{:keys [pid qid aid domain type folder topic] :as post}]
  (try
    (if (= type "q")
      ;; question - change status to 'p' directly
      (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
            _ (jdbc/with-db-transaction [conn {:datasource (deref (get @domain-to-connection domain))}]
                (let [[count] (jdbc/update! conn
                                            :post
                                            {:status "p" :last_update now :last_active now} ;; change status only
                                            ["pid = ? And domain = ? And status = ? And type = ?" (java.util.UUID/fromString pid) domain "i" type])]
                  (if (not= count 1) ;; change status error
                    (throw (ex-info "Publish post not found" {:error-code :publish-failed}))
                    (let [result (jdbc/execute! conn
                                                (sql/format {:update :stats
                                                             :set {:question_count (sql/call :+ :question_count 1)}
                                                             :where [:and [:= :domain domain] [:= :topic topic] [:= :folder folder]]}))]
                      (if (= (first result) 1)
                        result
                        (let [result (jdbc/insert! conn
                                                   :stats
                                                   {:domain domain
                                                    :topic topic
                                                    :folder folder
                                                    :question_count 1}
                                                   :return-keys ["pid"]
                                                   )]
                          (if (or (nil? result) (empty? result) )
                            (throw (ex-info "Publish post not found" {:error-code :publish-failed})))))))))]
        {:error-code :ok
         :data {:last_update now}})

      ;; not question - update parent stat as well
      (jdbc/with-db-transaction [conn {:datasource (deref (get @domain-to-connection domain))}]
        (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))
              update-result (jdbc/update! conn
                                          :post
                                          {:status "p" :last_update now :last_active now} ;; change status only
                                          ["pid = ? And domain = ? And status = ? And type = ?" (java.util.UUID/fromString pid) domain "i" type])
              _ (when (= (first update-result) 0)
                  (throw (ex-info "Parent post not found" {:error-code :publishi-failed})))
              parent-update-result (cond
                                     ;; is answer
                                     (= type "a") (jdbc/execute! conn
                                                                 ["update post set answers_count = answers_count + 1, last_active = ? , last_active_seq = nextval('post_last_active_seq_seq') where pid = ?" now (java.util.UUID/fromString qid)])
                                     ;; is comment to question
                                     (and
                                      (= type "c")
                                      (nil? aid)) (jdbc/execute!
                                                   conn
                                                   ["update post set comments_count = comments_count + 1, last_active = ? , last_active_seq = nextval('post_last_active_seq_seq') where pid = ?" now (java.util.UUID/fromString qid)])
                                     ;; is comment to answer
                                     (= type "c") (jdbc/execute!
                                                   [conn
                                                    "update post set comments_count = comments_count + 1 , last_active = ? , last_active_seq = nextval('post_last_active_seq_seq') where pid = ?" now (java.util.UUID/fromString aid)]))
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

(defn delete-draft
  "Delete draft post. Applicable only to post with status 'i' and 'r'"
  [{:keys [pid status domain]}]
  (try
    (cond
      (= status "i") (let [[delete-result] (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
                                             (jdbc/delete! conn
                                                           :post
                                                           ["pid = ? And status = ?" (java.util.UUID/fromString pid) status]))]
                       (if (= delete-result 1)
                         {:error-code :ok}
                         {:error-code :post-not-found}))
      (= status "r") (let [ [update-result] (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
                                              (jdbc/update! conn
                                                            :post
                                                            {:draft_title nil
                                                             :draft_content nil
                                                             :draft_content_lang nil
                                                             :draft_content_external nil
                                                             :status "p"}
                                                            ["pid = ? And status = ?" (java.util.UUID/fromString pid) status]))]
                       (if (= update-result 1)
                         {:error-code :ok}
                         {:error-code :post-not-found}))
      :else {:error-code :wrong-type-for-delete})
    (catch Exception e (do (println e) {:error-code :database-error}))
    (catch Throwable e (do (println e) {:error-code :unknown-error}))))
