(ns dcs.components.actor.is-human
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]))

(def component-type ::IsHuman)

(ecs/def-component ::IsHuman map?)

(s/fdef create :ret ::IsHuman)
(defn create [] (ecs/create-component component-type))
