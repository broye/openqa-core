(ns io.oqa.core.bootstrap.server
  (:import (io.vertx.core Vertx Handler)
           (io.vertx.core.net NetSocket)
           (io.vertx.core.http HttpMethod)
           (io.vertx.ext.web Router)
           (io.vertx.ext.web.handler BodyHandler))
  (:require [io.oqa.core.bootstrap.config :as config]
            [io.oqa.core.service.db :as db]
            [io.oqa.core.service.rest.handlers :as handlers]))

(def httpRequestHandler
  (reify Handler
    (handle [this request]
      (.. request response (end "hello world")))))

(defn init-sub-router [vertx]
  ;; init sub router
  (let [router (Router/router vertx)]
    (.. router route (handler (BodyHandler/create)))
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
    (println "endpint-prefix is >>>>>>>>>> " endpoint-prefix)
    (if (:disabled REST)
      (println "REST server disabled...")
      (let [server (. vertx createHttpServer )
            main-router (Router/router vertx)
            sub-router (init-sub-router vertx)]

        ;; Test route
        (.. sub-router (get "/test") (handler httpRequestHandler))

        ;; New post route
        (.. sub-router (post "/core/post") (handler handlers/new-post-handler))

        ;; Update post route
        (.. sub-router (put "/core/post") (handler handlers/update-post-handler))

        ;; Delete post route
        (.. sub-router (delete "/core/post/:pid/:domain/:status") (handler handlers/delete-post-handler))

        ;; Mount sub route
        (. main-router mountSubRouter endpoint-prefix sub-router)
        (doto server
          (.requestHandler main-router))

        (doseq [{:keys [host port]} listens]
          (. server listen (or port 8080) (or host "localhost"))
          (println "REST server started @ host " (or host "localhost") " port " (or port 8080)))))))
