(ns io.openqa.core.bootstrap.config
  (:refer-clojure :exclude [load])
  (:require [yaml.core :as yaml]))

;; global config
(def config (ref {}))

;; load config

(defn load-config
  ([] (load-config "./config/config.yaml"))
  ([file-name] (dosync (ref-set config (yaml/from-file file-name)))))
