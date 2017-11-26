(ns dcs.components.has-name
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]))

(def ^:private component-type ::HasName)

(s/def ::name string?)

(ecs/def-component ::HasNameComponent
  (s/keys :req [::name]))

(s/fdef create :ret ::HasNameComponent)
(defn create [name]
  (ecs/create-component
   component-type
   ::name name))

(s/fdef get-nane
  :args (s/cat :system ::ecs/System :entity ::ecs/Entity)
  :ret ::name)
(defn get-name [system entity]
  (::name (ecs/get-component system entity component-type)))
