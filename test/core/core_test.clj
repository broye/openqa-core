(ns core.core-test
  (:import (io.vertx.core Vertx Handler))
  (:require [clojure.test :refer :all]
            [io.oqa.core.service.db.helpers :as helpers]
            [io.oqa.core.service.db :as db]
            [io.oqa.core.service.db.posts :as posts]
            [io.oqa.core.service.db.vote :as vote]
            [io.oqa.core.service.db.user-info :as user-info]
            [io.oqa.core.bootstrap.config :as config]))

(def config-file "/home/fqye/projects/oqa/core/config/config.yaml")

(def config (config/load-config config-file))

(let [vertx (Vertx/vertx)
      {:keys [REST Default Shards]} config]
  (db/init-db-service Default Shards))

(println "NEW POST test >>>"  (posts/new-post {:title "test"
                                               :content "alright"
                                               :draft_title "Draft..."
                                               :domain "default"
                                               :topic "topic1"
                                               :tags ["a" "b" "c"]
                                               :uid "a0001"
                                               :user_name "a0001"
                                               :type "c"
                                               :status "p"
                                               :qid "0e6050d3-b22c-43d3-bd4c-2029a0fa6a26"
                                               ;; :aid "4276d47a-a1e6-4188-9bd0-5cc9cd0c73a5"
                                               :meta_tags ["1" "2" "3"]
                                               :folder "test"}))

;; (println "Update POST test >>>"  (posts/update-post {:title "modified test"
;;                                                      :draft_title "Draft 2..."
;;                                                      :domain "default"
;;                                                      :tags ["a" "b" "c"]
;;                                                      :uid "a0001"
;;                                                      :pid "d37ab549-6cff-4fa1-8d6a-9d3d0268c167"
;;                                                      :user_name "a0001"
;;                                                      :meta_tags ["1" "2" "3"]
;;                                                      :folder "test"}))

;; (println "Publish POST test >>>"  (posts/publish-post {:title "published test"
;;                                                        :draft_title "Draft 2..."
;;                                                        :domain "default"
;;                                                        :type "q"
;;                                                        :tags ["a" "b" "c"]
;;                                                        :uid "a0001"
;;                                                        :pid "3571d1b2-b8f8-4dba-b022-13833bd7277f"
;;                                                        :topic "topic1"
;;                                                        ;; :qid "d37ab549-6cff-4fa1-8d6a-9d3d0268c167"
;;                                                        :user_name "a0001"
;;                                                        :meta_tags ["1" "2" "3"]
;;                                                        :folder "test"}))

;; (println "Update user infor" (user-info/update-user-info {:uid "a0001" :user_name "User 001" :domain "default"}))

;; (println "New vote..." (vote/new-vote {:pid "013d116a-53a7-49c1-84a5-71fad93a5800" :uid "a001" :type "d" :domain "default"}))

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
;;                                                               :pid "2bef8357-9b81-494f-9140-6fe0865d3a63"
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
