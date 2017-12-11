(ns dcs.components.location.is-location
  (:require [clojure.spec.alpha :as s]
            [dcs.components.location.is-void :as is-void]
            [dcs.ecs :as ecs]))

(def component-type ::IsLocation)

(ecs/def-component ::IsLocation map?)

(s/fdef create :ret ::IsLocation)
(defn create [] (ecs/create-component component-type))

(defn location-entity-exists? [system location]
  (ecs/get-component system location component-type))

(defn get-non-void-locations [system]
  (->> (ecs/get-entities-with-component system component-type)
       (remove #(ecs/get-component system % is-void/component-type))))
