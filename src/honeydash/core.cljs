(ns honeydash.core
  (:require [cljs-http.client :as http]
            [cljs.core.async :as async]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.walk :as cw]
            [cognitect.transit :as t]
            [inflections.core :as inflect]
            [reagent.core :as reagent :refer [atom]]
            [schema.core :as s :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defonce app-data (atom {}))

(defn tags-template [app {:keys [project-id fault-tags fault-id]}]
  (when-not (empty? fault-tags)
    [:span {:class "ui labels"}
     (for [tag-name fault-tags]
       ^{:key [project-id tag-name fault-id]}
       [:a {:class "ui label tiny"} tag-name])]))

(defn fault-url [{:keys [fault-id project-id]}]
  (str "https://app.honeybadger.io/projects/" project-id "/faults/" fault-id))

(defn fault-message-ellipsis [message]
  (if (>= (count message) 97)
    (str (subs message 0 96) "...")
    message))

(defn fault-template [app fault]
  (let [{:keys [project-name klass message notices-count last-notice-at]} fault]
    [:tr {:class "fault"}
     [:td project-name]
     [:td
      [:div {:class "klass"}
       [:span {:class "name"}
        [:a {:href (fault-url fault) :target "_blank"} klass]]
       [tags-template app fault]]
      [:code (fault-message-ellipsis message)]]
     ;; TODO time ago
     [:td last-notice-at]
     [:td notices-count]]))

(defn faults-template [app]
  [:table {:class "ui table compact striped"}
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
  [faults-template app])

(reagent/render-component [app-layout app-data]
                          (. js/document (getElementById "app")))

(def ^:private honeybadger-request-timeout-ms 1000)

(defn honeybadger-get-faults
  "Requests all project faults from Honeybadger and adds the response onto a channel"
  ([app id]
   (let [result-chan (async/chan)]
     (go (let [auth-token (-> @app :config :auth-token)
               query-params {"auth_token" auth-token "ignored ""f" "resolved" "f" "environment" "production"}
               endpoint (str "/honeybadger/projects/" id "/faults")]

           (go-loop [faults []
                     response {:body {:results [] :current-page 0 :num-pages 1}}]

             (let [{:keys [results current-page num-pages]} (inflect/hyphenate-keys (:body response))]
               (if (>= current-page num-pages)
                 (async/>! result-chan (concat faults results))
                 (let [query-params (assoc query-params :page (inc current-page))
                       response (async/<! (http/get endpoint {:query-params query-params :timeout honeybadger-request-timeout-ms}))]
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
   :project-tags (:tags project)
   :fault-tags (:tags fault)})

(defn fetch-honeybadger-data [app]
  (doseq [{:keys [id] :as project} (:projects @app)]
    (go (let [honeybadger-faults (async/<! (honeybadger-get-faults app id))
              faults (->> honeybadger-faults
                         (filter (partial fault-has-project-tags? project))
                         (map (partial build-fault project)))]

          (swap! app update-in [:faults] into faults)
          (prn @app)))))

(defn parse-decoded-query [decoded-query]
  (let [without-query-symbol (subs decoded-query 1)
        query-parameters (str/split without-query-symbol "&")]
    (->> query-parameters
         (map #(str/split % "="))
         (into {})
         (inflect/hyphenate-keys)
         (cw/keywordize-keys))))

(def UriConfig
  {:auth-token s/Str
   :gist-id s/Str
   :order-by (s/enum "count" "recent")})

(defn initialize-uri-config
  "Creates and validates a config map from the browser location bar."
  []
  (let [raw-query-params (aget js/window "location" "search")
        decoded-query-params (js/decodeURIComponent raw-query-params)
        parsed-query-params (parse-decoded-query decoded-query-params)]
    (s/validate UriConfig parsed-query-params)))

(defn parse-json
  "Parses JSON into Clojure map with keywordized keys"
  [json]
  (let [json-reader (t/reader :json)]
    (->> json
         (t/read json-reader)
         (cw/keywordize-keys))))

(def ProjectConfig {:id s/Int
                    :name s/Str
                    :tags [s/Str]})

(def GistConfig [ProjectConfig])

(def ^:private github-request-timeout-ms 2000)

(defn fetch-gist-data
  "Fetches projects related configuration provided a Gist id.
  The result is added onto a channel.
  The Gist must include a JSON file. "
  [gist-id]
  (let [result-chan (async/chan)]
    (println "Fetching Gist data with id" gist-id)
    (go (let [endpoint (str "/github/gists/" gist-id)
              gist  (async/<! (http/get endpoint {:timeout github-request-timeout-ms}))
              gist-file (-> gist
                            :body
                            :files
                            inflect/hyphenate-keys
                            vals
                            first)
              gist-content (-> gist-file
                               :content
                               parse-json)]
          (s/validate GistConfig gist-content)
          (async/>! result-chan gist-content)))
    result-chan))

(defn make-sorted-set
  "Creates a set sorted by fault count or fault last noticed at."
  [order-by]
  (letfn [(ascending-fault-comparator [x y]
            (compare [(:project-id x) (:fault-id x)]
                     [(:project-id y) (:fault-id y)]))
          (descending-date-comparator [x y]
            (let [result (compare (:last-notice-at y) (:last-notice-at x))]
              (if (= result 0)
                (ascending-fault-comparator x y)
                result)))
          (descending-count-comparator [x y]
            (let [result (compare (:notices-count y) (:notices-count x))]
              (if (= result 0)
                (descending-date-comparator x y)
                result)))]
    (case order-by
      "count" (sorted-set-by descending-count-comparator)
      "recent" (sorted-set-by descending-date-comparator))))

;; TODO set refresh interval
(defn initialize [app]
  (go (let [{:keys [auth-token gist-id order-by] :as config} (initialize-uri-config)
            projects-config (async/<! (fetch-gist-data gist-id))
            faults-sorted-set (make-sorted-set order-by)]
        (swap! app merge (assoc @app :config config :projects projects-config :faults faults-sorted-set))
        (fetch-honeybadger-data app))))

(defn run
  "Honeydash entrypoint"
  []
  (println "Initializing Honeydash")
  (initialize app-data)
  )

(run)
(defn on-js-reload []
  ;; optionally touch your app to force rerendering depending on
  ;; your application
  ;; (swap! app update-in [:__figwheel_counter] inc)
)
