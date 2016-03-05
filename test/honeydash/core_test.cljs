(ns ^:figwheel-load honeydash.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [honeydash.core :as core]))

(deftest parse-decoded-query-test
  (testing "query with complete configuration"
    (let [decoded-query "?auth_token=xxx123zzz&project_ids=[98322, 1371]&tags=[\"ORDERS\"]"
          {:keys [auth-token project-ids tags]} (core/parse-decoded-query decoded-query)]
      (is (= auth-token "xxx123zzz"))
      (is (= project-ids "[98322, 1371]"))
      (is (= tags "[\"ORDERS\"]"))))
  (testing "query without configuration"
    (let [decoded-query ""
          parsed-query (core/parse-decoded-query decoded-query)]
      (is (= parsed-query {})))))

(deftest initialize-config-test
  (testing "parsed query with complete configuration"
    (let [parsed-query {:auth-token "xxx123zzz"
                        :project-ids "[98322, 1371]"
                        :tags "[\"ORDERS\"]"}
          {:keys [auth-token project-ids tags]} (core/initialize-config parsed-query)]
      (is (= auth-token "xxx123zzz"))
      (is (= project-ids #{98322 1371}))
      (is (= tags #{"ORDERS"}))))
  (testing "parsed query with incomplete configuration"
    (let [parsed-query {:auth-token "xxx123zzz"
                        :tags "[\"ORDERS\"]"}
          {:keys [auth-token project-ids tags]} (core/initialize-config parsed-query)]
      (is (= auth-token "xxx123zzz"))
      (is (= project-ids nil))
      (is (= tags #{"ORDERS"})))))
