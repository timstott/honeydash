(ns honeydash.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as str]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<! >! chan timeout]]
   [clojure.walk :as w]
   [inflections.core :as inflect]
   [schema.core :as s :include-macros true]
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defonce app-data (atom {}))

(defn projects-template [app]
  [:div
   (for [project (-> @app :projects)]
     ^{:key (:id project)} [:p (:name project)])])

(defn app-layout [app]
  [:div
   [projects-template app]

   ]
  )

(reagent/render-component [app-layout app-data]
                          (. js/document (getElementById "app")))

(defn parse-decoded-query [decoded-query]
  (let [without-query-symbol (subs decoded-query 1)
        query-parameters (str/split without-query-symbol "&")]
    (->> query-parameters
         (map #(str/split % "="))
         (into {})
         (inflect/hyphenate-keys)
         (w/keywordize-keys))))

(defn initialize-config [parsed-query]
  (letfn [(json-array-to-set [array] (when array
                               (-> array
                                   js/JSON.parse
                                   set)))]
    (-> parsed-query
        (update :project-ids json-array-to-set)
        (update :tags json-array-to-set))))

(def config-schema
  {:auth-token s/Str
   :project-ids (s/constrained #{s/Int} #(not (empty? %)))
   (s/optional-key :tags) #{s/Str}})

(prn "atom" @app-data)

(def faults-base-params {:ignored "f" :resolved "f" :environment "production"})

(defn fetch-projects-handler
  "Takes a project from the projects channel and adds it to the app projects sequence"
  [app]
  (let [channel (-> @app :channels :projects-chan)]
    (go-loop []
      (let [raw-project (<! channel)
            project (select-keys raw-project [:name :id])]
        (swap! app update :projects #(conj % project))
        (prn "Project handler updated app" @app))
      (recur))))

(defn fetch-projects [app]
  (let [auth-params {"auth_token" (-> @app :config :auth-token)}
        channel (-> @app :channels :projects-chan)
        project-ids (-> @app :config :project-ids)]
    (doseq [project-id project-ids]
      (go (let [projects-endpoint (str "/api/v1/projects/" project-id)
                raw-response (<! (http/get projects-endpoint {:query-params auth-params}))
                response-body (inflect/hyphenate-keys (:body raw-response))]
            (prn "Fetching project" project-id)
            (>! channel response-body))))))

(defn fetch-faults-handler
  "Takes a fault from the faults channel and adds them to the app faults set."
  [app]
  (let [channel (-> @app :channels :faults-chan)]
    (go-loop []
      (let [raw-fault (<! channel)
            fault (select-keys raw-fault [:id :klass :last-notice-at :message :notices-count :project-id :tags])]
        (swap! app update :faults #(conj % fault))
        (prn "Fault handler updated app" @app))
      (recur))))

(defn fetch-faults [app]
  (let [auth-params {"auth_token" (-> @app :config :auth-token)}
        params (merge auth-params {"environment" "production" "resolved" "f" "ignored" "f"})
        channel (-> @app :channels :faults-chan)
        projects-ids (-> @app :config :project-ids)]
    (doseq [project-id projects-ids]
      (go (let [faults-endpoint (str "/api/v1/projects/" project-id "/faults/")
                raw-response (<! (http/get faults-endpoint {:query-params params}))
                response-body (inflect/hyphenate-keys (:body raw-response))]
            (prn "Fetching faults for project" project-id)
            (doseq [fault (:results response-body)]
              (>! channel fault)))))))

(defn initialize [app]
  (let [raw-query (aget js/window "location" "search")
        decoded-query (js/decodeURIComponent raw-query)
        parsed-query (parse-decoded-query decoded-query)
        config (initialize-config parsed-query)
        channels {:projects-chan (chan)
                  :faults-chan (chan)}]
    (s/validate config-schema config)
    (swap! app merge (-> @app
                         (assoc :projects [])
                         (assoc :config config)
                         (assoc :channels channels)))
    (fetch-projects-handler app)
    (fetch-faults-handler app)
    (fetch-faults app)
    (fetch-projects app)))

(initialize app-data)

(defn on-js-reload []
  ;; optionally touch your app to force rerendering depending on
  ;; your application
  ;; (swap! app update-in [:__figwheel_counter] inc)
)
