(ns dcs.components.has-location
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s])
  (:import [dcs.components.location.is_location IsLocation]))

(defn- location-entity-exists? [system location]
  (e/get-component system location IsLocation))

(defrecord HasLocation [location])

;; I do not really like doing the data integrity checks on the system inside of
;; a function def here, but this is the only way I'm aware of to get that really
;; nice printout of "Hey I expected X but Y!".
;;
;; On the other hand, the argument is the *entire state of the universe* so if
;; this gets to any non-trivial size the error report will crash my emacs.
;;
;; Huh. Maybe I should search harder for alternatives?
(s/fdef create
  :args (s/cat :system map? :location identity)
  :fn (fn [{{:keys [system location]} :args}]
        (location-entity-exists? system location)))

(defn create [system location]
  (->HasLocation location))
