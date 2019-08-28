(ns io.openqa.core.main
  (:gen-class)
  (:require [io.openqa.core.bootstrap.server :as server]
            [io.openqa.core.bootstrap.config :as config]))

(defn -main
  "Boot strap server..."
  [& args]
  (let [config-file (first args)
        _ (config/load-config config-file)
        config @config/config]
    (server/start)))
