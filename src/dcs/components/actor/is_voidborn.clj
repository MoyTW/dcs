(ns dcs.components.actor.is-voidborn
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]))

(def ^:private component-type ::IsVoidborn)

(ecs/def-component ::IsVoidborn map?)

(s/fdef create :ret ::IsVoidborn)
(defn create [] (ecs/create-component component-type))
