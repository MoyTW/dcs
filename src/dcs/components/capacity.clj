(ns dcs.components.capacity
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]))

(s/def ::contracts (s/coll-of [::item-id]))

(s/def ::capacity (s/keys :req-un [::contracts]))

(s/fdef ->Capacity
  :args (s/cat :contracts ::contracts)
  :ret ::capacity)

(defrecord Capacity [contracts])

(defn create [contracts]
  (->Capacity contracts))
