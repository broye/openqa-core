(ns io.oqa.core.service.rest.handlers
  (:import (io.vertx.core Vertx Handler)
           (io.vertx.core.net NetSocket)
           (io.vertx.core.http HttpMethod)
           (io.vertx.ext.web Router))
  (:require [io.oqa.core.bootstrap.config :as config]
            [cheshire.core :as cheshire]
            [io.oqa.core.service.db.posts :as posts]
            [io.oqa.core.service.db :as db]))


(def new-post-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            result (posts/new-post data) ]
        (println "body is >>>>> >>>>>>>>>>>>" data "result is >>> " result)
        (.. context response (end (cheshire/generate-string result)))))))
