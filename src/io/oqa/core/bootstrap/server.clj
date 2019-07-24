(ns io.oqa.core.bootstrap.server
  (:import (io.vertx.core Vertx Handler)
           (io.vertx.core.net NetSocket))
  (:require [io.oqa.core.bootstrap.config :as config]
            [io.oqa.core.service.db :as db]))

(def httpRequestHandler
  (reify Handler
    (handle [this request]
      (.. request response (end "hello world")))))

(defn start [config]
  "Initiate db connection and start server..."
  (let [vertx (Vertx/vertx)
        server (. vertx createHttpServer )
        {:keys [REST Default Shards]} config
        {:keys [endpointPrefix listens]} REST]
    (db/init-db-service Default Shards vertx)
    (. server requestHandler httpRequestHandler)
    (if (:disabled REST)
      (println "REST server disabled...")
      (doseq [{:keys [host port]} listens]
        (. server listen (or port 8080) (or host "localhost"))
        (println "REST server started @ host " (or host "localhost") " port " (or port 8080))))))
