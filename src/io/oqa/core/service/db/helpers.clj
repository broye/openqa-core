(ns io.oqa.core.service.db.helpers
  (:import (io.vertx.reactivex.sqlclient Tuple))
  (:require [clojure.string :as str]))

(defn build-insert-fields
  "Build values part. args: count: count of key -values, field-value: [{:key val}] return: result: {:positions :tuple}"
  [count field-values result]
  (if (nil? field-values)
    result
    (let [[k v] (first field-values)
          {:keys [positions fields tuple]} result
          [new-count new-result] (if (nil? v)
                                   [count result]
                                   [(inc count) {:positions (conj positions (format "$%d" (+ 1 count)))
                                                 :fields (conj fields (name k) )
                                                 :tuple (. tuple addValue v)}])]
      (recur new-count (next field-values) new-result)))
  )

(defn build-insert
  "Build insert query. args: table : table name, field-value: map of field and value, return: return clause"
  [table field-values return]
  (println "field-values" field-values)
  (let [field-values-seq (seq field-values)
        {:keys [positions fields tuple]} (build-insert-fields 0 field-values-seq {:positions [] :fields [] :tuple (Tuple/tuple)})]
    (println positions)
    {:query-string (format "insert into %s (%s) values (%s) %s" table (str/join "," fields) (str/join "," positions) return)
     :tuple tuple}))
