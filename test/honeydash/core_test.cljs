(ns ^:figwheel-load honeydash.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [honeydash.core :as core]))

(deftest fault-has-project-tags?
  (testing "when the project has no tags"
    (let [fault {:tags #{}}
          project {:tags #{}}]
      (is (true? (core/fault-has-project-tags? project fault)))))
  (testing "when a fault tags is included in project tags"
    (let [fault {:tags #{"ABC"}}
          project {:tags #{"ABC" "DEF"}}]
      (is (true? (core/fault-has-project-tags? project fault)))))
  (testing "when a fault tags is not included in project tags"
    (let [fault {:tags #{"XYZ"}}
          project {:tags #{"ABC" "DEF"}}]
      (is (false? (core/fault-has-project-tags? project fault))))))

(deftest parse-decoded-query-test
  (testing "when query params are present"
    (let [decoded-query "?auth_token=xxx123zzz&gist_id=98421"
          {:keys [auth-token gist-id]} (core/parse-decoded-query decoded-query)]
      (is (= auth-token "xxx123zzz"))
      (is (= gist-id "98421"))))
  (testing "when no query params"
    (let [decoded-query ""
          parsed-query (core/parse-decoded-query decoded-query)]
      (is (= parsed-query {})))))

(deftest coerced-query-params-test
  (testing "when a param is a number represented as a string"
    (let [query-params {:auth-token "abcd"
                        :gist-id "abcd"
                        :order-by "count"
                        :refresh-interval "60"}
          {:keys [refresh-interval]} (core/coerce-query-params query-params)]
        (is (= refresh-interval 60)))))

(deftest initialize-uri-config-test
  (testing "when query params are valid"
    (let [raw-query-params "?auth_token=abcd&gist_id=abcd&order_by=recent&refresh_interval=30"
          expected-result {:auth-token "abcd"
                           :gist-id "abcd"
                           :order-by "recent"
                           :refresh-interval 30}]
      (is (= (core/initialize-uri-config raw-query-params) expected-result))))
  (testing "when query params are invalid"
    (let [raw-query-params "?auth_token=abcd&gist_id=abcd"]
      (is (thrown-with-msg? js/Error #"Value does not match schema"
                            (core/initialize-uri-config raw-query-params))))))

(def honeybadger-project-0
  {:id 0
   :name "Marketplace"
   :tags []})

(def honeybadger-fault-0
  {:id 0
   :klass "SocketError"
   :last-notice-at "2016-05-13T09:50:16.000Z"
   :message "getaddrinfo: Temporary failure in name resolution"
   :notices-count 1
   :tags ["MKRT"]})

(def honeybadger-fault-1
  {:id 1
   :klass "Sequel::DatabaseDisconnectError"
   :last-notice-at "2016-05-13T10:00:00.000Z"
   :message "PG::ConnectionBad: PQconsumeInput()"
   :notices-count 2
   :tags ["MKRT"]})

(def honeybadger-fault-2
  {:id 2
   :klass "RecordNotFound"
   :last-notice-at "2016-05-13T16:00:45.000Z"
   :message ""
   :notices-count 25
   :tags []})

(deftest build-fault-test
  (let [expected-fault {:fault-id 0
                        :klass "SocketError"
                        :last-notice-at "2016-05-13T09:50:16.000Z"
                        :message "getaddrinfo: Temporary failure in name resolution"
                        :notices-count 1
                        :project-id 0
                        :project-name "Marketplace"
                        :project-tags []
                        :fault-tags ["MKRT"]}]
    (is (= (core/build-fault honeybadger-project-0 honeybadger-fault-0) expected-fault))))

(deftest make-sorted-set-test
  (let [fault-0 (core/build-fault honeybadger-project-0 honeybadger-fault-0)
        fault-1 (core/build-fault honeybadger-project-0 honeybadger-fault-1)
        fault-2 (core/build-fault honeybadger-project-0 honeybadger-fault-2)]
    (testing "when sorted by most count"
      (let [sorted-set-by-count (-> (core/make-sorted-set "count") (conj fault-0 fault-2 fault-1))]
        (is (= (first sorted-set-by-count) fault-2))
        (is (= (last sorted-set-by-count) fault-0))))
    (testing "when sorted by recent occurrence"
      (let [sorted-set-by-count (-> (core/make-sorted-set "recent") (conj fault-1 fault-0 fault-2))]
        (is (= (first sorted-set-by-count) fault-2))
        (is (= (last sorted-set-by-count) fault-0))))
    (testing "when duplicate faults are added"
      (let [sorted-set-by-count (-> (core/make-sorted-set "recent") (conj fault-1 fault-1 fault-1))]
        (is (= (first sorted-set-by-count) fault-1))
        (is (= (count sorted-set-by-count) 1))))))
