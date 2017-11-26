(ns dcs.components.actor.has-capacity
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]))

(def ^:private component-type ::HasCapacity)

(s/def ::contracts (s/coll-of [::item-id]))

(s/def ::HasCapacity (s/keys :req [::contracts]))

(s/fdef create :ret ::HasCapacity)
(defn create [contracts]
  (ecs/create-component
   component-type
   ::contracts contracts))
