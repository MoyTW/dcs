(ns dcs.components.actor.has-inventory
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]))

(def component-type ::HasInventory)

(s/def ::max-size int?)
(s/def ::contents (s/coll-of [::ecs/Entity]))

(ecs/def-component ::HasInventory
  (s/keys :req [::contents ::max-size]))

(s/fdef create
  :args (s/cat :contents ::contents :max-size ::max-size)
  :ret ::HasInventory)
(defn create [contents max-size]
  (ecs/create-component
   component-type
   ::contents contents
   ::max-size max-size))
