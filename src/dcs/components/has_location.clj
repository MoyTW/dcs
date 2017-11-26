(ns dcs.components.has-location
  (:require [clojure.spec.alpha :as s]
            [dcs.components.location.is-location :as is-location]
            [dcs.ecs :as ecs]))

(def ^:private component-type ::HasLocation)

(s/def ::location ::ecs/Entity)

;; I do not really like doing the data integrity checks on the system inside of
;; a function def here, but this is the only way I'm aware of to get that really
;; nice printout of "Hey I expected X but Y!".
;;
;; On the other hand, the argument is the *entire state of the universe* so if
;; this gets to any non-trivial size the error report will crash my emacs.
;;
;; Huh. Maybe I should search harder for alternatives?
(s/fdef create
  ;; TODO: ::system and ::entity need to be things
  :args (s/cat :system ::ecs/System :location ::ecs/Entity)
  :fn (fn [{{:keys [system location]} :args}]
        (is-location/location-entity-exists? system location)))
(defn create [system location]
  (ecs/create-component
   component-type
   ::location location))

(s/fdef change-location
  :args (s/cat :system ::ecs/System
               :entity ::ecs/Entity
               :location ::ecs/Entity)
  :fn (fn entity-has-existing-location? [{{:keys [system entity]} :args}]
        (ecs/get-component system entity component-type)))
(defn change-location
  "Sets the entity's location to the new location."
  [system entity location]
  (ecs/add-component system entity (create system location)))

(s/fdef get-location
  :args (s/cat :system ::ecs/System :entity ::ecs/Entity)
  :ret ::ecs/Entity)
(defn get-location [system entity]
  (::location (ecs/get-component system entity component-type)))
