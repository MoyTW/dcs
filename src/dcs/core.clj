(ns dcs.core
  (:gen-class)
  (:require [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]))

(defonce last-id (atom 0))

(defn- next-id []
  (swap! last-id inc))

(s/def ::id int?)
(s/def ::name string?)

(s/def ::devil (s/keys :req [::id ::name]))

(s/fdef generate-devil
  :ret ::devil)

(defn- generate-devil []
  {::id (next-id)
   ::name (str (dec (next-id)))})

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (st/instrument)
  (clojure.pprint/pprint (generate-devil)))
