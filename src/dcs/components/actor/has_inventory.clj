(ns dcs.components.actor.has-inventory
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]))

(s/def ::max-size int?)
(s/def ::contents (s/coll-of [::item-id]))

(s/def ::has-inventory (s/keys :req-un [::contents ::max-size]))

(s/fdef ->HasInventory
  :args (s/cat :contents ::contents :max-size ::max-size)
  :ret ::has-inventory)

(defrecord HasInventory [contents max-size])

(defn create [contents max-size]
  (->HasInventory contents max-size))
