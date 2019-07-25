(ns io.oqa.core.service.db.posts
  (:import (io.vertx.pgclient
            PgConnectOptions
            PgPool)
           (io.vertx.sqlclient PoolOptions)
           (io.vertx.core Handler)
           (java.util UUID))

  (:require [io.oqa.core.service.db :refer [domain-to-connection]]
            [io.oqa.core.service.db.helpers :as helpers]
            [clojure.string :as str]))

(defn new-post
  "Create new post"
  [ {:keys [title
            content
            content_lang
            draft_title
            draft_content
            draft_content_lang
            domain
            topic
            tags
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
    :domain-not-found ;; domain error
    (let [pool (get @domain-to-connection domain)
          _ (println "Pool got is " (class pool))
          pid (UUID/randomUUID)
          now (java.time.OffsetDateTime/now)
          field-values {:title title
                        :content content
                        :content_lang content_lang
                        :draft_title draft_title
                        :draft_content draft_content
                        :draft_content_lang draft_content_lang
                        :domain domain
                        :topic topic
                        :tags (and tags (format "'{%s}'" (str/join " " tags)))
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
          query (helpers/build-insert "post" field-values "returning pid")
          {:keys [query-string tuple]} query
          _ (println "query-string enerated is " query-string)
          ]
      (. pool getConnection (reify Handler
                              (handle [this ar]
                                (println "getConnection: " ar)
                                (if (. ar succeeded)
                                  (let [conn (. ar result)]
                                    (println ">>>>>>>>> inserting " query-string)
                                    (. conn prepare query-string (reify Handler
                                                                   (handle [this ar]
                                                                     (if (. ar succeeded)
                                                                       (let [pg (. ar result)]
                                                                         (. pg execute tuple (reify Handler
                                                                                               (handle [this ar]
                                                                                                 (if (. ar succeeded)
                                                                                                   (println "created " (. ar result))
                                                                                                   (do
                                                                                                     (println "failed " (. ar cause))
                                                                                                     (. conn close)))))))
                                                                       (do (println "failed " (. ar cause))
                                                                           (. conn close)))))))
                                  (println "failed " (. ar cause)))))))))
