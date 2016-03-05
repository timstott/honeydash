(ns honeydash.core
  (:require
   [clojure.string :as str]
   [clojure.walk :as w]
   [inflections.core :as inflect]
   [schema.core :as s :include-macros true]
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn hello-world []
  [:h1 (:text @app-state)])

(reagent/render-component [hello-world]
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

(defn initialize []
  (let [raw-query (aget js/window "location" "search")
        decoded-query (js/decodeURIComponent raw-query)
        parsed-query (parse-decoded-query decoded-query)
        config (initialize-config parsed-query)]
    (s/validate config-schema config)))

(prn "CONFIG" (initialize))

(comment
  (def raw-query-example "?auth_token=xxx123zzz&projects=[98322,1371]&tags=[%22ORDERS%22,%20%22SIGNUPS%22]")
  (def decoded-query-example (js/decodeURIComponent raw-query-example))
  (def parsed-query-example (parse-decoded-query decoded-query-example))
  (s/validate config-schema {:auth-token "f" :project-ids #{1}})
  )

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
