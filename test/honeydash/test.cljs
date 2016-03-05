(ns ^:figwheel-always honeydash.test
  (:require [honeydash.core-test]
            [cljs.test :refer-macros [run-all-tests]]))

(enable-console-print!)

(defn ^:export run
  []
  (run-all-tests #"honeydash.*-test"))
