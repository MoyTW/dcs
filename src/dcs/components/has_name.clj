(ns dcs.components.has-name
  (:require [clojure.spec.alpha :as s]))

(s/def ::has-name string?)

(s/fdef ->HasCapacity
  :ret ::has-name)

(defrecord HasName [name])

(defn create [name]
  (->HasName name))
