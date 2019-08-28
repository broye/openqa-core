(ns io.oqa.core.service.rest.handlers
  (:import (io.vertx.core Vertx Handler)
           (io.vertx.core.net NetSocket)
           (io.vertx.core.http HttpMethod)
           (io.vertx.ext.web Router))
  (:require [io.oqa.core.bootstrap.config :as config]
            [cheshire.core :as cheshire]
            [io.oqa.core.service.db.posts :as posts]
            [io.oqa.core.service.db.query :as query]
            [io.oqa.core.service.db.vote :as vote]
            [io.oqa.core.service.db.user-info :as user-info]
            [io.oqa.core.service.db.domain :as domain]
            [io.oqa.core.service.db :as db]))


(def new-post-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            result (posts/new-post data) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def update-post-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            _ (println "data is >>>>> " data)
            result (posts/update-post data) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def update-user-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            _ (println "data is >>>>> " data)
            result (user-info/update-user-info data) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def delete-post-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            request (. context request)
            pid (. request getParam "pid")
            domain (. request getParam "domain")
            status (. request getParam "status")
            body (. context getBodyAsString)
            data {:pid pid :domain domain :status status}
            _ (println "data is >>>>> " data)
            status (:status data)
            result (if (or (= status "i") (= status "r"))
                     (posts/delete-draft data)
                     (posts/mark-post-deleted data)) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def query-post-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            _ (println "data is >>>>> " data)
            result (query/query-post data) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def new-vote-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            result (vote/new-vote data) ]
        (.. context response (end (cheshire/generate-string result)))))))


(def unvote-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            result (vote/unvote data) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def query-vote-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            _ (println "data is >>>>> " data)
            result (query/query-vote data) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def query-stats-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            _ (println "data is >>>>> " data)
            result (query/query-stats data) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def new-domain-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            _ (println "domain data is >>>>>>> " data)
            result (domain/new-domain data) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def update-domain-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            body (. context getBodyAsString)
            data (cheshire/parse-string body true)
            result (domain/update-domain data) ]
        (.. context response (end (cheshire/generate-string result)))))))

(def delete-domain-handler
  (reify Handler
    (handle [this context]
      (let [response (. context response)
            request (. context request)
            domain (. request getParam "domain")
            result (domain/delete-domain domain) ]
        (.. context response (end (cheshire/generate-string result)))))))
