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
            topic
            tags
            meta_tags
            type
            qid
            aid
            uid
            user_name
            user_avatar
            reply_to_uid
            reply_to_user_name
            reply_to_user_avatar
            status
            create_date
            last_update]
     }]

  (println @domain-to-connection)
  (if (or (nil? domain) (nil? (get @domain-to-connection domain)))
    {:error-code :domain-not-found} ;; domain error
    (let [now (new java.sql.Timestamp (.. (java.util.Calendar/getInstance) getTime getTime))]
      (try (jdbc/with-db-connection [conn {:datasource (deref (get @domain-to-connection domain))}]
             (let [result (jdbc/insert! conn
                                        :post
                                        {:title title
                                         :pid (or pid (java.util.UUID/randomUUID))
                                         :content content
                                         :content_lang content_lang
                                         :draft_title draft_title
                                         :draft_content draft_content
                                         :draft_content_lang draft_content_lang
                                         :domain domain
                                         :topic topic
                                         :tags tags
                                         :meta_tags meta_tags
                                         :type type
                                         :qid qid
                                         :aid aid
                                         :uid uid
                                         :user_name user_name
                                         :user_avatar user_avatar
                                         :reply_to_uid reply_to_uid
                                         :reply_to_user_name reply_to_user_name
                                         :reply_to_user_avatar reply_to_user_avatar
                                         :status status
                                         :create_date now
                                         :last_update now
                                         }
                                        :return-keys ["pid" "seq_id"])]
               (if ( or (nil? result) (empty? result) (nil? (:pid (first result))) )
                 {:error-code :insert-failed}
                 {:error-code :ok
                  :data {:pid (:pid (first result))
                         :seq_id (:seq_id (first result))
                         :last_update now
                         :create_date now}})))
           (catch Exception e ((println e) {:error-code :database-error}))
           (catch Throwable e ((println e) {:error-code :unkown-error}))))))
