(ns honeydash.core
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [cljs-http.client :as http]
   [cljs.core.async :as async]
   [clojure.walk :as w]
   [inflections.core :as inflect]
   [schema.core :as s :include-macros true]
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defonce app-data (atom {}))

(defn fault-template [app fault]
  (let [{:keys [project-name klass message notices-count last-notice-at]} fault]
    [:tr
     [:td project-name]
     [:td
      [:p klass]
      [:p message]]
     [:td last-notice-at]
     [:td notices-count]]))

(defn faults-template [app]
  [:table
   [:thead
    [:tr
     [:th "Project"]
     [:th "Error"]
     [:th "Last Seen"]
     [:th "Count"]]]
   [:tbody
    (for [fault (:faults @app)]
      ^{:key (:fault-id fault)} [fault-template app fault])]])

(defn app-layout [app]
  [:div {:class "container-fluid"}
   [faults-template app]])

(reagent/render-component [app-layout app-data]
                          (. js/document (getElementById "app")))

(def Config
  {:auth-token s/Str
   :projects [{:id s/Int
               :tags [s/Str]}]})

(defn initialize-config [edn-string]
  (let [config (cljs.reader/read-string edn-string)]
    (s/validate Config config)))

(defn honeybadger-get-faults
  "Requests all project id faults from Honeybadger and adds the response onto a channel"
  ([app id]
   (let [result-chan (async/chan)]
     (go (let [auth-token (-> @app :config :auth-token)
               query-params {"auth_token" auth-token "ignored ""f" "resolved" "f" "environment" "production"}
               endpoint (str "/api/v1/projects/" id "/faults")]

           (go-loop [faults []
                     response {:body {:results [] :current-page 0 :num-pages 1}}]

             (let [{:keys [results current-page num-pages]} (inflect/hyphenate-keys (:body response))]
               (count (prn faults))
               (if (>= current-page num-pages)
                 (async/>! result-chan (concat faults results))
                 (let [query-params (assoc query-params :page (inc current-page))
                       response (async/<! (http/get endpoint {:query-params query-params}))]
                   (prn "GET" endpoint (:status response))
                   (recur (concat faults results) response)))))))
     result-chan)))

(defn fault-has-project-tags?
  "When the fault tags intersect project tags return true. Unless project tags is empty"
  [project fault]
  (let [project-tags (:tags project)
        fault-tags (:tags fault)]
    (or
     (empty? project-tags)
     (not (empty? (set/intersection fault-tags project-tags))))))

(defn build-fault [project fault]
  {:fault-id (:id fault)
   :klass (:klass fault)
   :last-notice-at (:last-notice-at fault)
   :message (:message fault)
   :notices-count (:notices-count fault)
   :project-id (:id project)
   :project-name (:name project)
   :project-tags (:tags project)})

(defn fetch-honeybadger-data [app]

  (doseq [{:keys [id] :as project} (:projects @app)]
    (go (let [honeybadger-faults (async/<! (honeybadger-get-faults app id))
              faults (->> honeybadger-faults
                         (filter (partial fault-has-project-tags? project))
                         (map (partial build-fault project)))]

          (swap! app update-in [:faults] concat faults)
          (prn @app)))))

(defn initialize [app]
  (let [raw-query (aget js/window "location" "search")
        edn-string (-> raw-query
                       (js/decodeURIComponent)
                       (subs 1))
        config (initialize-config edn-string)
        projects (:projects config)
        config-no-projects (dissoc config :projects)]
    (swap! app merge (-> @app
                         (assoc :faults [])
                         (assoc :projects projects)
                         (assoc :config config-no-projects)))
    (fetch-honeybadger-data app)))

(initialize app-data)

(defn on-js-reload []
  ;; optionally touch your app to force rerendering depending on
  ;; your application
  ;; (swap! app update-in [:__figwheel_counter] inc)
)
