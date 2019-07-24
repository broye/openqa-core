(ns core.core-test
  (:require [clojure.test :refer :all]
            [io.oqa.core.service.db.helpers :as helpers]))

(deftest build-db-query
  (testing "build positions."
    (let [field-values {:a 12 :b "alright" :c nil}
          result (helpers/build-insert "test_table" field-values "returning pid")]
      (println (:query-string result) (:tuple result))
      (is (not (nil? (:query-string result)))))))
