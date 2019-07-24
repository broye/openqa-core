(ns io.oqa.core.service.db.posts
  (:import (io.vertx.pgclient
            PgConnectOptions
            PgPool)
           (io.vertx.sqlclient PoolOptions)
           (io.vertx.core Handler)
           (java.util UUID))

  (:require [io.oqa.core.bootstrap.db :refer domain-to-connection]
            [clojure.string as str]))

(defn new-post
  "Create new post"
  [:keys {title
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
          last_update
          }]

  (if (or (nil? domain) (nil? (get @domain-to-connection domain)))
    :domain-not-found ;; domain error
    (let [pool (get @domain-to-connection domain)
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
                        :tags (fromat "'{%s}'" (str/join " " tags))
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
                        :last_updte now
                        }
          query (helpers/build-insert "post" field-values)
          ]
      ))
  )
