(ns dcs.components.provides-action
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]
            [dcs.components.actor.has-magic :as has-magic]
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

;; train-proficiency action
(s/def ::available-domains
  (s/coll-of ::has-magic/domain :kind set? :min-count 1))
(s/def ::min-xp int?)
(s/def ::max-xp int?)
(s/def ::train-proficiency-action
  (s/and
   ::action
   (s/keys :req [::available-domains ::min-xp ::max-xp])))

(s/fdef create-train-proficiency-action
  :args (s/cat :available-domains ::available-domains
               :min-xp ::min-xp
               :max-xp ::max-xp)
  :ret ::train-proficiency-action
  :fn (fn [{{:keys [::min-xp ::max-xp]} :ret}]
        (<= min-xp max-xp)))
(defn create-train-proficiency-action [available-domains min-xp max-xp]
  {::action-type ::train-proficiency
    ;; TODO: find out what you want 'value' to be baseline'd on
   ::payoff {::intrinsic-value (int (/ (+ min-xp max-xp) 2))}
   ::available-domains available-domains
   ::min-xp min-xp
   ::max-xp max-xp})

;; TODO: Put the RNG in here somewhere!
(defn train-proficiency-fn
  [system {:keys [::available-domains ::min-xp ::max-xp]} entity]
  (let [domain (first available-domains) ;; TODO: Randomness!
        xp (int (/ (+ min-xp max-xp) 2))]
    (has-magic/change-proficiency-xp system entity domain xp)))

;; action stuff

(s/fdef ::action-fn
  :args (s/cat :system map? :action ::action :entity identity) ;; TODO: UUID
  :ret map?) ;; TODO: System!

(def action-types->fns
  {::travel travel-fn
   ::train-proficiency train-proficiency-fn})

(s/def ::action-type (set (keys action-types->fns)))

(s/def ::intrinsic-value int?)
(s/def ::payoff (s/keys :req [::intrinsic-value]))

(s/def ::action (s/keys :req [::action-type ::payoff]
                        :opt [::costs ::requirements]))

(s/def ::actions (s/coll-of ::action))

(defrecord ProvidesAction [actions])

(def record ProvidesAction)

(defn create [actions] (->ProvidesAction actions))

(defn add-provided-action [system entity action]
  (let [updated (if-let [c (e/get-component system entity ProvidesAction)]
                  (update c :actions conj action)
                  (create [action]))]
    (e/add-component system entity updated)))

(defn execute-action [system {:keys [::action-type] :as action} entity]
  (let [action-fn (action-type action-types->fns)]
    (action-fn system action entity)))

(orchestra.spec.test/instrument)
