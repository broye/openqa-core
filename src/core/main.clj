(ns core.main
  (:gen-class)
  (:require [core.bootstrap.server :as server]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ( server/start))
