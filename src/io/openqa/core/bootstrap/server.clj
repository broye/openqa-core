(ns io.openqa.core.bootstrap.server
  (:import (io.vertx.core Vertx Handler)
           (io.vertx.core.net NetSocket)
           (io.vertx.core.http HttpMethod)
           (io.vertx.ext.web Router)
           (io.vertx.ext.web.handler BodyHandler))
  (:require [io.openqa.core.bootstrap.config :as config]
            [io.openqa.core.service.db :as db]
            [io.openqa.core.service.rest.handlers :as handlers]))

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

        ;; New vote
        (.. sub-router (post "/core/vote") (handler handlers/new-vote-handler))

        ;; Unvote
        (.. sub-router (post "/core/unvote") (handler handlers/unvote-handler))

        ;; Update post route
        (.. sub-router (put "/core/post") (handler handlers/update-post-handler))

        ;; Update user info accross posts
        (.. sub-router (put "/core/user") (handler handlers/update-user-handler))

        ;; Delete post route
        (.. sub-router (delete "/core/post/:pid/:domain/:status") (handler handlers/delete-post-handler))

        ;; Query post route
        (.. sub-router (post "/core/query/post") (handler handlers/query-post-handler))

        ;; Query vote route
        (.. sub-router (post "/core/query/vote") (handler handlers/query-vote-handler))

        ;; Query stats
        (.. sub-router (post "/core/query/stats") (handler handlers/query-stats-handler))

        ;; New domain route
        (.. sub-router (post "/core/domain") (handler handlers/new-domain-handler))

        ;; Update domain route
        (.. sub-router (put "/core/domain") (handler handlers/update-domain-handler))

        ;; Delete domain route
        (.. sub-router (delete "/core/domain/:domain") (handler handlers/delete-domain-handler))

        ;; Query domains
        (.. sub-router (get "/core/query/domain") (handler handlers/query-domain-handler))

        ;; Mount sub route
        (. main-router mountSubRouter endpoint-prefix sub-router)
        (doto server
          (.requestHandler main-router))

        (doseq [{:keys [host port]} listens]
          (. server listen (or port 8080) (or host "localhost"))
          (println "REST server started @ host " (or host "localhost") " port " (or port 8080)))))))
