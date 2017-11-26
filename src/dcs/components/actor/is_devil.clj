(ns dcs.components.actor.is-devil
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]))

(def component-type ::IsDevil)

(ecs/def-component ::IsDevil map?)

(s/fdef create :ret ::IsDevil)
(defn create [] (ecs/create-component component-type))
