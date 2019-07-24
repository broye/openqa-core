(ns io.oqa.core.main
  (:gen-class)
  (:require [io.oqa.core.bootstrap.server :as server]
            [io.oqa.core.bootstrap.config :as config]))

(defn -main
  "Boot strap server..."
  [& args]
  (let [config-file (first args)]
    (let [config (config/load-config config-file)]
      (server/start config))))
