(ns dcs.components.provides-action
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]
            [dcs.components.has-location :as has-location]
            [dcs.components.location.is-location :as is-location])
  (:import [dcs.components.has_name HasName]))

;; travel stuff

(s/def ::travel-action
  (s/and
   ::action
   (s/keys :req [::origin ::destination])))

(defn t? [_] true) ;; TODO: ::Entity

(s/fdef create-travel-action
  :args (s/cat :system map? :origin t? :destination t?)
  :ret ::travel-action
  :fn (fn [{{:keys [origin destination]} :ret {:keys [system]} :args}]
        (s/and (is-location/location-entity-exists? system origin)
               (is-location/location-entity-exists? system destination)
               (not= origin destination))))
(defn create-travel-action [system origin destination]
  {::action-type ::travel
   ::payoff {::intrinsic-value 0}
   ::origin origin
   ::destination destination})

(defn travel-fn [system {:keys [::origin ::destination]} entity]
  (has-location/change-location system entity destination))

;; action stuff

(s/fdef ::action-fn
  :args (s/cat :system map? :action ::action :entity identity) ;; TODO: UUID
  :ret map?) ;; TODO: System!

(def action-types->fns
  {::travel travel-fn})

(s/def ::action-type (set (keys action-types->fns)))

(s/def ::intrinsic-value int?)
(s/def ::payoff (s/keys :req [::intrinsic-value]))

(s/def ::action (s/keys :req [::action-type ::payoff]
                        :opt [::costs ::requirements]))

(s/def ::actions (s/coll-of ::action))

(defrecord ProvidesAction [actions])

(def record ProvidesAction)

(defn create [actions] (->ProvidesAction actions))

(defn execute-action [system {:keys [::action-type] :as action} entity]
  (let [action-fn (action-type action-types->fns)]
    (action-fn system action entity)))

(orchestra.spec.test/instrument)
