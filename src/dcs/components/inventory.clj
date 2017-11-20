(ns dcs.components.inventory
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]))

(s/def ::max-size int?)
(s/def ::contents (s/coll-of [::item-id]))

(s/def ::inventory (s/keys :req-un [::contents ::max-size]))

(s/fdef ->Inventory
  :args (s/cat :contents ::contents :max-size ::max-size)
  :ret ::inventory)

(defrecord Inventory [contents max-size])

(defn create [contents max-size]
  (->Inventory contents max-size))
