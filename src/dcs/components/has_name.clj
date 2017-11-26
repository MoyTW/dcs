(ns dcs.components.has-name
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]))

(def component-type ::HasName)

(s/def ::name string?)

(ecs/def-component ::HasNameComponent
  (s/keys :req [::name]))

(s/fdef create :ret ::HasNameComponent)
(defn create [name]
  (ecs/create-component
   component-type
   ::name name))
