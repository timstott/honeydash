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
