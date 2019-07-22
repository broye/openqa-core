(ns io.oqa.core.bootstrap.server
  (:import (io.vertx.core Vertx Handler)
           (io.vertx.core.net NetSocket)))

(def httpRequestHandler
  (proxy [Handler] []
    (handle [request]
      (.. request response (end "hello world")))))

(def httpRequestHandler2
  (reify Handler
    (handle [this request]
      (.. request response (end "hello world")))))

(defn start [{:keys [endpointPrefix listens]}]
  (let [vertx (Vertx/vertx)
        server (. vertx createHttpServer )]
    (println endpointPrefix listens)
    (. server requestHandler httpRequestHandler2)
    (doseq [{:keys [host port]} listens]
      (. server listen (or port 8080) (or host "localhost")))
    (println "Server started")))
