(ns minimalism.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [minimalism.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'minimalism.core-test))
    0
    1))
