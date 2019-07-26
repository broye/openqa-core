(ns core.core-test
  (:import (io.vertx.core Vertx Handler))
  (:require [clojure.test :refer :all]
            [io.oqa.core.service.db.helpers :as helpers]
            [io.oqa.core.service.db :as db]
            [io.oqa.core.service.db.posts :as posts]
            [io.oqa.core.bootstrap.config :as config]))

(def config-file "/home/fqye/projects/oqa/core/config/config.yaml")

(def config (config/load-config config-file))

(let [vertx (Vertx/vertx)
      {:keys [REST Default Shards]} config]
  (db/init-db-service Default Shards))

;; (println "NEW POST test >>>"  (posts/new-post {:title "test"
;;                                                :draft_title "Draft..."
;;                                                :domain "default"
;;                                                :tags ["a" "b" "c"]
;;                                                :uid "a0001"
;;                                                :user_name "a0001"
;;                                                :type "q"
;;                                                :status "i"
;;                                                :meta_tags ["1" "2" "3"]}))
;; (println "NEW POST test2 >>>"  (posts/new-post {:title "test2" :domain "default" :tags ["a" "b" "c"] :meta_tags ["1" "2" "3"]}))

(println "NEW answer POST test >>>"  (posts/new-post {:title "test"
                                                      :draft_title "Draft..."
                                                      :draft_content "Draft reply..."
                                                      :domain "default"
                                                      :tags ["a" "b" "c"]
                                                      :uid "a0001"
                                                      :user_name "a0001"
                                                      :type "c"
                                                      :status "i"
                                                      :aid "cc3d5176-7ef7-4058-9a53-f342d43dac78"
                                                      :qid "44404377-4e7b-47b9-9dd9-92dfbf57aafe"
                                                      :meta_tags ["1" "2" "3"]}))

(deftest build-db-query
  (testing "build positions."
    (let [field-values {:a 12 :b "alright" :c nil}
          result (helpers/build-insert "test_table" field-values "returning pid")]
      (println (:query-string result) (:tuple result))
      (is (not (nil? (:query-string result)))))))

;; (Thread/sleep 100000)
