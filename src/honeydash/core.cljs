(ns honeydash.core
  (:require
   [clojure.string :as str]
   [clojure.walk :as w]
   [inflections.core :as inflect]
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn hello-world []
  [:h1 (:text @app-state)])

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))

(def raw-query (aget js/window "location" "search"))
(def decoded-query (js/decodeURIComponent raw-query))

(prn raw-query)
(prn decoded-query)

(defn parse-decoded-query [decoded-query]
  (let [without-query-symbol (subs decoded-query 1)
        query-parameters (str/split without-query-symbol "&")]
    (->> query-parameters
         (map #(str/split % "="))
         (into {})
         (inflect/hyphenate-keys)
         (w/keywordize-keys))))

(comment
  (def raw-query-example "?auth_token=xxx123zzz&projects=[98322,1371]&tags=[%22ORDERS%22,%20%22SIGNUPS%22]")
  (def decoded-query-example (js/decodeURIComponent raw-query-example))
  (parse-decoded-query decoded-query-example))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
