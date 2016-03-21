(ns ^:figwheel-load honeydash.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [honeydash.core :as core]))

(deftest initialize-config-test
  (testing "edn string with complete configuration"
    (let [edn-string "{:auth-token \"abcdef\" :projects [{:id 45 :tags [\"USERS\" \"ORDERS\"]}]}"
          {:keys [auth-token projects]} (core/initialize-config edn-string)]
      (is (= auth-token "abcdef"))
      (is (= (first projects) {:id 45 :tags ["USERS" "ORDERS"]}))))
  (testing "malformed edn string"
    (let [edn-string "{:auth-token abcdef :projects [{:id 45 :tags [\"USERS\" \"ORDERS\"]}]}"]
      ;(is (thrown? (js/Error.) (core/initialize-config edn-string)))
      )))

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
