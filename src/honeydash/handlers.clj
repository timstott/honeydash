;; A Compojure application which serves static assets and
;; proxies requests to Honeybadger and GitHub to overcome
;; browser same origin policy constraints.
;; The application is mounted by figwheel
(ns honeydash.handlers
  (:require [clj-http.client :as client]
            [compojure
             [core :refer :all]
             [route :as route]]
            [ring.middleware
             [json :as middleware]
             [keyword-params :refer [wrap-keyword-params]]]
            [ring.util.response :as response]))

(def ^:private honeybadger-api-endpoint "https://app.honeybadger.io/v1")
(def ^:private github-api-endpoint "https://api.github.com/")

(defn honeybadger-api-handler
  "Proxies requests to Honeybadger API"
  [{:keys [uri query-string] :as request}]
  (let [requested-path (clojure.string/replace-first uri "/honeybadger" "")
        url (str honeybadger-api-endpoint requested-path "?" query-string)
        honeybadger-response (client/get url {:accept :json :as :json})]
    (response/response (:body honeybadger-response))))

(defn github-api-handler
  "Proxies requests to GitHub API"
  [{:keys [uri query-string] :as request}]
  (let [requested-path (clojure.string/replace-first uri "/github" "")
        url (str github-api-endpoint requested-path "?" query-string)
        github-response (client/get url {:accept :json :as :json})]
    (response/response (:body github-response))))

(defroutes app-routes
  (GET "/" [] (response/resource-response "index.html" {:root "public"}))
  (GET "/honeybadger/*" [] honeybadger-api-handler)
  (GET "/github/*" [] github-api-handler)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-keyword-params
      middleware/wrap-json-params
      middleware/wrap-json-response))
