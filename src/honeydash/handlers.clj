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

(defn honeybadger-api-handler [{:keys [uri query-string] :as request}]
  (let [requested-path (clojure.string/replace-first uri "/honeybadger" "")
        url (str honeybadger-api-endpoint requested-path "?" query-string)
        honeybadger-response (client/get url {:accept :json :as :json})]
    (response/response (:body honeybadger-response))))

(defroutes app-routes
  (GET "/" [] (response/resource-response "index.html" {:root "public"}))
  (GET "/honeybadger/*" [] honeybadger-api-handler)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-keyword-params
      middleware/wrap-json-params
      middleware/wrap-json-response))
