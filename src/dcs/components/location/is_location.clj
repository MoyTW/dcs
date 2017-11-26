(ns dcs.components.location.is-location
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]))

(def component-type ::IsLocation)

(ecs/def-component ::IsLocation map?)

(s/fdef create :ret ::IsLocation)
(defn create [] (ecs/create-component component-type))

(defn location-entity-exists? [system location]
  (ecs/get-component system location component-type))
