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

(println "NEW POST test >>>"  (posts/new-post {:title "test" :domain "default"}))
(println "NEW POST test2 >>>"  (posts/new-post {:title "test" :domain "default"}))

(deftest build-db-query
  (testing "build positions."
    (let [field-values {:a 12 :b "alright" :c nil}
          result (helpers/build-insert "test_table" field-values "returning pid")]
      (println (:query-string result) (:tuple result))
      (is (not (nil? (:query-string result)))))))

;; (Thread/sleep 100000)
