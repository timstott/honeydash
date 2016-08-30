(ns honeydash.components)

(defn fault-tags-component [app {:keys [project-id fault-tags fault-id]}]
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

(defn fault-component [app fault]
  (let [{:keys [project-name klass message notices-count last-notice-at]} fault]
    [:tr {:class "fault"}
     [:td project-name]
     [:td
      [:div {:class "klass"}
       [:span {:class "name"}
        [:a {:href (fault-url fault) :target "_blank"} klass]]
       [fault-tags-component app fault]]
      [:code (fault-message-ellipsis message)]]
     ;; TODO time ago
     [:td last-notice-at]
     [:td notices-count]]))

(defn faults-list-component [app]
  [:table {:class "ui table compact striped"}
   [:thead
    [:tr
     [:th "Project"]
     [:th "Error"]
     [:th "Last Seen"]
     [:th "Count"]]]
   [:tbody
    (for [fault (:faults @app)]
      ^{:key (:fault-id fault)} [fault-component app fault])]])
