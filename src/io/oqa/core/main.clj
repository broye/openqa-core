(ns io.oqa.core.main
  (:gen-class)
  (:require [io.oqa.core.bootstrap.server :as server]
            [io.oqa.core.bootstrap.config :as config]
            [io.oqa.core.service.db :as db]
            ))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [config-file (first args)]
    (let [config (config/load-config config-file)]
      (let [{:keys [REST Default Shards]}  config]
        (println REST Default Shards)
        (db/init-db-service Default Shards)
        (if (:disabled REST)
          (println "OQA rest server disabled...")
          (server/start REST))
        ))))
