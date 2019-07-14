(ns core.bootstrap.server
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

(defn start []
  (let [vertx (Vertx/vertx)
        server (. vertx createHttpServer )]

    (. server requestHandler httpRequestHandler2)
    (. server listen 8080 )
    (println "Server started")))
