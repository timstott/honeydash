(ns honeydash.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [clojure.string :as str]
   [cljs-http.client :as http]
   [cljs.core.async :as async]
   [clojure.walk :as w]
   [inflections.core :as inflect]
   [schema.core :as s :include-macros true]
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defonce app-data (atom {}))

(defn fault-template [app project fault]
  (let [{:keys [klass message notices-count last-notice-at]} fault]
    [:tr
     [:td
      [:p klass]
      [:p message]]
     [:td last-notice-at]
     [:td notices-count]]))

(defn faults-template [app project]
  [:table {:class "table"}
   [:thead
    [:tr
     [:th "Error"]
     [:th "Last Seen "
      ;; TODO only display icon when order by last-seen in config
      [:span {:class "glyphicon glyphicon-sort-by-attributes-alt"}]]
     [:th "Count"]]]
   [:tbody
    (for [fault (:faults project)]
      ^{:key (:id fault)} [fault-template app project fault])]])

(defn project-tags-template [app project]
  (let [{:keys [tags]} project]
    (if (empty? tags)
      [:span {:class "label label-default"} "All"]
      [:span
       (for [tag tags]
         ^{:key tag} [:span {:class "label label-primary"} tag])])))

(defn project-template [app project]
  [:div {:class "col-md-6"}
   [:div {:class "panel panel-default"}
    [:div {:class "panel-heading"}
     [:h2 (:name project)
      [project-tags-template app project]]]
    [:div {:class "panel-body"}
     [:p "Unresolved Faults" (count (:faults project))]
     ;; TODO display last seen error
     [:p "Last fault occured"]]
    [faults-template app project]]])

(defn projects-template [app]
  [:div {:class "row"}
   (for [project (-> @app :projects)]
     ^{:key (:id project)} [project-template app project])])

(defn app-layout [app]
  [:div {:class "container-fluid"}
   [projects-template app]])

(reagent/render-component [app-layout app-data]
                          (. js/document (getElementById "app")))

(def Config
  {:auth-token s/Str
   :projects [{:id s/Int
               :tags [s/Str]}]})

(defn initialize-config [edn-string]
  (let [config (cljs.reader/read-string edn-string)]
    (s/validate Config config)))

(def faults-query-params {:ignored "f" :resolved "f" :environment "production"})

(defn make-get
  "Makes a GET request to Honeybadger and adds the response onto a channel"
  ([app path] (make-get app path {}))
  ([app path query-params]
   (let [result-chan (async/chan)]
     (go (let [auth-params {"auth_token" (-> @app :config :auth-token)}
               query-params-with-auth (merge auth-params query-params)
               endpoint (str "/api" path)
               response (async/<! (http/get endpoint {:query-params query-params-with-auth}))
               body (inflect/hyphenate-keys (:body response))]
           (prn "GET" endpoint (:status response))
           (async/>! result-chan body)))
     result-chan)))

(defn update-project
  "Updates a project in the app projects seq, provided its id and a map"
  [app project-id kv]
  (let [projects (map
                  (fn [project]
                    (if (= (:id project) project-id)
                      (merge project kv)
                      project))
                  (:projects @app))]
    (swap! app assoc :projects projects)
    (prn "Updated app-data" @app)))

(defn update-project-with-honeybadger-data
  [app honeybadger-project]
  (let [{:keys [id name]} honeybadger-project]
    (update-project app id {:name name})))

(def fault-keys
  [:id
   :tags
   :klass
   :last-notice-at
   :message
   :notices-count
   :project-id])

(defn update-project-faults-with-honeybadger-data
  [app project honeybadger-faults]
  (let [honeybadger-faults (:results honeybadger-faults)]
    (let [project-id (:id project)
          project-tags (:tags project)
          select-fault-keys-fn #(select-keys % fault-keys)
          filter-fault-fn (fn [fault]
                            (if (empty? project-tags)
                              true
                              (not (empty? (clojure.set/intersection (set (:tags fault)) (set project-tags))))))
          faults (->> honeybadger-faults
                      (filter filter-fault-fn)
                      (map select-fault-keys-fn))]

      (update-project app project-id {:faults faults
                                      :faults-count (count faults)}))))

(defn fetch-honeybadger-data [app]
  (let [projects (:projects @app)]
    (doseq [{:keys [id tags] :as project} projects]
      (let [project-path (str "/v1/projects/" id)
            project-faults-path (str project-path "/faults")]
        (async/take!
          (make-get app project-path)
          (partial update-project-with-honeybadger-data app))
        (async/take!
          (make-get app project-faults-path faults-query-params)
          (partial update-project-faults-with-honeybadger-data app project))))))

(defn initialize [app]
  (let [raw-query (aget js/window "location" "search")
        edn-string (-> raw-query
                       (js/decodeURIComponent)
                       (subs 1))
        config (initialize-config edn-string)
        projects (:projects config)
        config-no-projects (dissoc config :projects)]
    (swap! app merge (-> @app
                         (assoc :projects projects)
                         (assoc :config config-no-projects)))
    (fetch-honeybadger-data app)))

(initialize app-data)

(defn on-js-reload []
  ;; optionally touch your app to force rerendering depending on
  ;; your application
  ;; (swap! app update-in [:__figwheel_counter] inc)
)
