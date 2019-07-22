(ns io.oqa.core.bootstrap.config
  (:refer-clojure :exclude [load])
  (:require [yaml.core :as yaml]))

(defn load-config
  ([] (load-config "./config/config.yaml"))
  ([file-name] (yaml/from-file file-name)))
