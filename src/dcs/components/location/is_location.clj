(ns dcs.components.location.is-location
  (:require [brute.entity :as e]))

(defrecord IsLocation [])

(defn create [] (->IsLocation))

(defn location-entity-exists? [system location]
  (e/get-component system location IsLocation))
