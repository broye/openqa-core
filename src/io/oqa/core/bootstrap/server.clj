(ns io.oqa.core.bootstrap.server
  (:import (io.vertx.core Vertx Handler)
           (io.vertx.core.net NetSocket)
           (io.vertx.core.http HttpMethod)
           (io.vertx.ext.web Router))
  (:require [io.oqa.core.bootstrap.config :as config]
            [io.oqa.core.service.db :as db]))

(def httpRequestHandler
  (reify Handler
    (handle [this request]
      (.. request response (end "hello world")))))

(defn init-sub-router [vertx end-point http-method rest-handler]
  ;; init sub router

  (let [router (Router/router vertx)]
    (.. router (route http-method end-point) (handler rest-handler))
    router))

(defn start []
  "Initiate db connection and start server..."
  (let [vertx (Vertx/vertx)
        config @config/config
        REST (:REST config)
        endpoint-prefix (:endpointPrefix REST)
        {:keys [REST Default Shards]} config
        {:keys [endpointPrefix listens]} REST]
    (db/init-db-service Default Shards vertx)

    (if (:disabled REST)
      (println "REST server disabled...")
      (let [server (. vertx createHttpServer )
            main-router (Router/router vertx)
            test-sub-router (init-sub-router vertx "/test" HttpMethod/GET httpRequestHandler)]
        (. main-router mountSubRouter endpoint-prefix test-sub-router)
        (. server requestHandler main-router)
        (doseq [{:keys [host port]} listens]
          (. server listen (or port 8080) (or host "localhost"))
          (println "REST server started @ host " (or host "localhost") " port " (or port 8080)))))))
