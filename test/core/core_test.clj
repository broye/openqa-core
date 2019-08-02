(ns core.core-test
  (:import (io.vertx.core Vertx Handler))
  (:require [clojure.test :refer :all]
            [io.oqa.core.service.db.helpers :as helpers]
            [io.oqa.core.service.db :as db]
            [io.oqa.core.service.db.posts :as posts]
            [io.oqa.core.service.db.vote :as vote]
            [io.oqa.core.service.db.query :as query]
            [io.oqa.core.service.db.user-info :as user-info]
            [io.oqa.core.bootstrap.config :as config]))

(def config-file "/home/fqye/projects/oqa/core/config/config.yaml")

(def config (config/load-config config-file))

(let [vertx (Vertx/vertx)
      {:keys [REST Default Shards]} config]
  (db/init-db-service Default Shards))

;; (println "NEW POST test >>>"  (posts/new-post {:title "test"
;;                                                :content "alright"
;;                                                :draft_title "Draft..."
;;                                                :draft_content "Draft content..."
;;                                                :domain "default"
;;                                                :topic "topic2"
;;                                                :tags ["a" "b" "c"]
;;                                                :uid "a0001"
;;                                                :user_name "a0001"
;;                                                :type "q"
;;                                                :status "p"
;;                                                ;; :qid "c2e12091-6d66-428e-a83d-e94bce68a470"
;;                                                ;; :aid "b3927052-adf5-4bd6-8125-c3174611239b"
;;                                                :reply_to_uid "a002"
;;                                                :reply_to_user_name "haha"
;;                                                :reply_to_user_avatar "blala"
;;                                                :meta_tags ["1" "2" "3" "a-meta-tag"]
;;                                                :folder "test2"}))

;; ;; (println "delete draft post >>>"  (posts/delete-draft {:title "test"
;;                                                        :content "alright"
;;                                                        :draft_title "draft..."
;;                                                        :domain "default"
;;                                                        :topic "topic1"
;;                                                        :tags ["a" "b" "c"]
;;                                                        :uid "a0001"
;;                                                        :user_name "a0001"
;;                                                        :type "a"
;;                                                        :status "r"
;;                                                        :pid "79cd2a27-1cae-41f2-a18c-6aacc3e61b0b"
;;                                                        ;; :qid "0e6050d3-b22c-43d3-bd4c-2029a0fa6a26"
;;                                                        ;; :aid "4276d47a-a1e6-4188-9bd0-5cc9cd0c73a5"
;;                                                        :meta_tags ["1" "2" "3"]
;;                                                        :folder "test"}))

;; ;; (println "update post test >>>"  (posts/update-post {:title "modified test"
;;                                                      :draft_title "draft 2..."
;;                                                      :domain "default"
;;                                                      :tags ["a" "b" "c"]
;;                                                      :uid "a0001"
;;                                                      :pid "0e6050d3-b22c-43d3-bd4c-2029a0fa6a26"
;;                                                      :user_name "a0001"
;;                                                      :type "q"
;;                                                      :meta_tags ["1" "2" "3"]
;;                                                      :folder "test"}))

;; (println "publish post test >>>"  (posts/publish-post {:title "published test"
;;                                                        :draft_title "draft 2..."
;;                                                        :domain "default"
;;                                                        :type "q"
;;                                                        :tags ["a" "b" "c"]
;;                                                        :uid "a0001"
;;                                                        :pid "0e6050d3-b22c-43d3-bd4c-2029a0fa6a26"
;;                                                        :topic "topic1"
;;                                                        ;; :qid "d37ab549-6cff-4fa1-8d6a-9d3d0268c167"
;;                                                        :user_name "a0001"
;;                                                        :meta_tags ["1" "2" "3"]
;;                                                        :folder "test"}))

;; (println "update user infor" (user-info/update-user-info {:uid "b3c0e5c9-292f-42f6-aaa5-a0002bc5d979" :user_name "user --- ddd" :domain "default"}))

;; (doseq [ i (range 0 100)]
;; (println "New vote..." (vote/new-vote {:pid "dcbe9f4c-ddc2-4170-b3bb-fa588f83157d" :uid (format  "a00%d" i) :type "d" :domain "default"})))


;; (println "query vote" (query/query-vote {:pid "dcbe9f4c-ddc2-4170-b3bb-fa588f83157d"
;;                                          :domain "default"
;;                                          :from_seq 50
;;                                          ;; :uid "a001"
;;                                          :size 10}))

;; (println "query post" (query/query-post {
;;                                          :domain "default"
;;                                          :size 5
;;                                          :fields [:last_active_seq]
;;                                          :uid "a0001"
;;                                          :type "q"
;;                                          :order_by :seq_id
;;                                          ;; :status "i"
;;                                          ;; :from_last_active_seq 30
;;                                          ;; :tag "a-tag"
;;                                          :meta-tag "a-meta-tag"
;;                                          ;; :order_by :seq_id
;;                                          :pid "71f4e27a-7612-4c20-893f-6694c39919c4"
;;                                          }
;;                                         ))

(println "query stats" (query/query-stats {
                                           :domain "default"
                                           :size 5
                                           :topic "topic1"
                                           :folder "folder2"
                                           }
                                          ))

;; (println "Un vote..." (vote/unvote {:pid "013d116a-53a7-49c1-84a5-71fad93a5800" :uid "a001" :type "d" :domain "default"}))

;; (println "Mark POST deleted >>>"  (posts/mark-post-deleted {:title ""
;;                                                             :draft_title "Draft 2..."
;;                                                             :domain "default"
;;                                                             :tags ["a" "b" "c"]
;;                                                             :uid "a0001"
;;                                                             :pid "d37ab549-6cff-4fa1-8d6a-9d3d0268c167"
;;                                                             :user_name "a0001"
;;                                                             :meta_tags ["1" "2" "3"]
;;                                                             :folder "test"}))





;; (println "Mark POST revising >>>"  (posts/mark-post-revising {:title ""
;;                                                               :draft_title "Draft 2..."
;;                                                               :domain "default"
;;                                                               :tags ["a" "b" "c"]
;;                                                               :uid "a0001"
;;                                                               :pid "79cd2a27-1cae-41f2-a18c-6aacc3e61b0b"
;;                                                               :user_name "a0001"
;;                                                               :meta_tags ["1" "2" "3"]
;;                                                               :folder "test"}))


;; (println "NEW POST test2 >>>"  (posts/new-post {:title "test2" :domain "default" :tags ["a" "b" "c"] :meta_tags ["1" "2" "3"]}))


;; (println "NEW answer POST test >>>"  (posts/new-post {:title "test"
;;                                                       :draft_title "Draft..."
;;                                                       :draft_content "Draft reply..."
;;                                                       :domain "default"
;;                                                       :tags ["a" "b" "c"]
;;                                                       :uid "a0001"
;;                                                       :user_name "a0001"
;;                                                       :type "a"
;;                                                       :status "i"
;;                                                       ;; :aid "cc3d5176-7ef7-4058-9a53-f342d43dac78"
;;                                                       :qid "d37ab549-6cff-4fa1-8d6a-9d3d0268c167"
;;                                                       :meta_tags ["1" "2" "3"]}))

;; (Thread/sleep 100000)
