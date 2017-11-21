(ns dcs.components.has-capacity
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]))

(s/def ::contracts (s/coll-of [::item-id]))

(s/def ::has-capacity (s/keys :req-un [::contracts]))

(s/fdef ->HasCapacity
  :args (s/cat :contracts ::contracts)
  :ret ::has-capacity)

(defrecord HasCapacity [contracts])

(defn create [contracts]
  (->HasCapacity contracts))
