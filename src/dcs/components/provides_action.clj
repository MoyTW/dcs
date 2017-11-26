(ns dcs.components.provides-action
  (:require [clojure.set :as cset]
            [clojure.spec.alpha :as s]
            [dcs.components.actor.has-magic :as has-magic]
            [dcs.components.has-location :as has-location]
            [dcs.components.location.is-location :as is-location]
            [dcs.ecs :as ecs]))

;; REQUIREMENTS

(defn- create-domain-requirement [available-domains]
  {::requirement-type ::domain
   ::requires-one available-domains})

(defn- domain-fn [system {:keys [::requires-one] :as requirement} entity]
  (let [entity-domains (has-magic/get-proficiencies-domains system entity)]
    (boolean (seq (cset/intersection requires-one entity-domains)))))

(def requirement-types->fns
  {::domain domain-fn})

(s/def ::requirement-type (set (keys requirement-types->fns)))
(s/def ::requirement (s/keys :req [::requirement-type]))

;; Really for provides-action, not requirements
(s/def ::requirements (s/coll-of ::requirement))

(defn- get-requirement-fn-by-type [requirement-type]
  (get requirement-types->fns requirement-type))

(defn- meets-requirement?
  [system {:keys [::requirement-type] :as requirement} entity]
  (let [requirement-fn (get-requirement-fn-by-type requirement-type)]
    (requirement-fn system requirement entity)))

(defn meets-requirements? [system requirements entity]
  (every? #(meets-requirement? system % entity) requirements))

;; COSTS

(s/def ::costs any?)

;; travel stuff

(s/def ::travel-action
  (s/and
   ::action
   (s/keys :req [::origin ::destination])))

(s/fdef create-travel-action
  :args (s/cat :system ::ecs/System
               :origin ::ecs/Entity
               :destination ::ecs/Entity)
  :ret ::travel-action
  :fn (fn [{{:keys [origin destination]} :ret {:keys [system]} :args}]
        (s/and (is-location/location-entity-exists? system origin)
               (is-location/location-entity-exists? system destination)
               (not= origin destination))))
(defn create-travel-action [system origin destination]
  {::action-type ::travel
   ::payoff {::intrinsic-value 0}
   ::origin origin
   ::destination destination
   ::requirements []
   ::costs []})

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
   ::requirements [(create-domain-requirement available-domains)]
   ::costs []
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

(def ^:private action-types->fns
  {::travel travel-fn
   ::train-proficiency train-proficiency-fn})

;; It's worth leaving this here as a warning to future selves.
;;
;; What I was attempting to do here was to say "When you call
;; get-action-by-type, you expect some function back whose arguments are [system
;; action entity] of specs [::ecs/System ::action ::ecs/Entity] and which
;; returns a ::ecs/System. What I was expecting it to do was to go and check and
;; see if the function had been spec'd, and if the spec matched that it would
;; say okay and pass. So I wrote the following:
;;
;; (s/fdef get-action-by-type
;;   :ret (s/fspec
;;         :args (s/cat :system ::ecs/System :action ::action :entity ::ecs/Entity)
;;         :ret ::ecs/System))
;;
;; I also augmented travel-fn with a corresponding spec, assuming that would do
;; the job.
;;
;; However what that snippet does is actually *generatively tests* the returned
;; function from get-action-by-type. This is completely categorically impossible
;; to reconcile with how I am using the lookup, as the actual action send into
;; the function must correspond to its own action, and it simply won't make
;; sense to throw a "travel" action into a "fight" function - it won't resolve
;; properly and might not conform. There's no way to sanely (that is, without
;; building some insane spec parallel mapping edifice) match those together than
;; I can think of.
;;
;; tl;dr: clojure.spec can't do all of the things you'd expect in terms of
;; type-checking! when you fspec something it *doesn't* try to match the specs
;; defined elsewhere, it tries to generatively test the function and it's very
;; picky!

(defn- get-action-by-type [action-type]
  (get action-types->fns action-type))

;; action component

(def ^:private component-type ::ProvidesAction)

(s/def ::action-type (set (keys action-types->fns)))

(s/def ::intrinsic-value int?)
(s/def ::payoff (s/keys :req [::intrinsic-value]))

(s/def ::action (s/keys :req [::action-type
                              ::payoff
                              ::requirements
                              ::costs]))

(s/def ::actions (s/coll-of ::action))

(ecs/def-component ::ProvidesAction
  (s/keys :req [::actions]))

(s/fdef create :ret ::ProvidesAction)
(defn create [actions]
  (ecs/create-component
   component-type
   ::actions actions))

(s/fdef get-actions
  :args (s/cat :system ::ecs/System :entity ::ecs/Entity)
  :ret (s/or :nil nil? :actions ::actions))
(defn- get-actions [system entity]
  (::actions (ecs/get-component system entity component-type)))

(s/fdef get-allowed-actions
  :args (s/cat :system ::ecs/System
               :executor ::ecs/Entity
               :provider ::ecs/Entity)
  :ret (s/or :nil nil? :actions ::actions))
(defn get-allowed-actions [system executor provider]
  (->> (get-actions system provider)
       (filter #(meets-requirements? system (::requirements %) executor))))

(s/fdef add-provided-action
  :args (s/cat :system ::ecs/System :entity ::ecs/Entity :action ::action)
  :ret ::ecs/System
  :fn (fn [{{:keys [system entity action]} :args sys :ret}]
        (contains? (set (get-actions sys entity)) action)))
(defn add-provided-action [system entity action]
  (let [updated (if-let [c (ecs/get-component system entity component-type)]
                  (update c ::actions conj action)
                  (create [action]))]
    (ecs/add-component system entity updated)))

(s/fdef execute-action
  :args (s/cat :system ::ecs/System :action ::action :entity ::ecs/Entity)
  :ret ::ecs/System)
(defn execute-action [system {:keys [::action-type] :as action} entity]
  (let [action-fn (get-action-by-type action-type)]
    (action-fn system action entity)))
