(ns dcs.components.location.is-void
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]))

(def component-type ::IsVoid)

(ecs/def-component ::IsVoid map?)

(s/fdef create :ret ::IsVoid)
(defn create [] (ecs/create-component component-type))
