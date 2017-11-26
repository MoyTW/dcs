(ns dcs.components.actor.has-magic
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]
            [dcs.random :as r]))

;; Probably belongs somewhere else. Uses D&D levels. Formula is: XP-for-level =
;; L*(L-1)*500
(def level->xp-chart
  {1 0
   2 1000
   3 3000
   4 6000
   5 10000
   6 15000
   7 21000
   8 28000
   9 36000
   10 45000
   11 55000
   12 66000
   13 78000
   14 91000
   15 105000
   16 120000
   17 136000
   18 153000
   19 171000
   20 190000})

;; The equation for XP-to-level is D&D's, n*(n-1)*500
;;
;; This inverts it to n = (1 + sqrt(1 - 4(x/500))) / 2 - we leave out the +- in
;; favor of just + because we're not interested in the left side. I'm glad I
;; remembered how to do this. Like it's not hard, but hey! that high school
;; algebra not *totally* wasted!
;;
;; Probably belongs somewhere else.
(defn xp->level [xp]
  (int (/ (+ 1 (Math/sqrt (- 1 (* 4 (* -1 (/ xp 500)))))) 2)))

(defn xp-for-level [level]
  (level->xp-chart level))

;; *****************************************************************************
;; * HAS MAGIC COMPONENT *
;; *****************************************************************************

(def component-type ::HasMagic)

(def domain-values #{:wood :fire :earth :metal :water})
(s/def ::domain domain-values)

(def aptitude-values #{0.5 0.75 1.0 1.5 2})
(s/def ::aptitude aptitude-values)

(s/def ::xp int?)

(s/def ::proficiency (s/keys :req [::domain ::xp ::aptitude]))

(s/def ::proficiencies (s/map-of ::domain ::proficiency))

(ecs/def-component ::HasMagic
  (s/keys :req [::proficiencies]))

(s/fdef create
  :args (s/cat :proficiencies ::proficiencies)
  :ret ::HasMagic)
(defn create [proficiencies]
  (ecs/create-component
   component-type
   ::proficiencies proficiencies))

(s/fdef create-proficiency
  :args (s/cat :domain ::domain :aptitude ::aptitude :xp ::xp)
  :ret ::proficiency)
(defn create-proficiency [domain aptitude xp]
  {::domain domain
   ::aptitude aptitude
   ::xp xp})

(s/fdef merge-proficiencies
  :ret ::proficiencies)
(defn merge-proficiencies [proficiencies]
  (->> (map #(hash-map (::domain %) %) proficiencies)
       (into {})))

(defn- create-rand-proficiency [^java.util.Random rng max-level domain]
  (create-proficiency domain
                      (r/seeded-rand-item rng aptitude-values)
                      (r/seeded-next-int rng (xp-for-level max-level))))

(defn create-rand-has-magic
  [^java.util.Random rng max-domains max-level]
  (->> (r/seeded-shuffle rng domain-values)
       (take (inc (r/seeded-next-int rng max-domains)))
       (map (partial create-rand-proficiency rng max-level))
       merge-proficiencies
       create))

(s/fdef change-proficiency-xp
  :args (s/cat :system ::ecs/System
               :entity ::ecs/Entity
               :domain ::domain
               :xp-delta int?)
  :ret ::ecs/System)
(defn change-proficiency-xp
  [system entity domain xp-delta]
  (let [updated (update-in (ecs/get-component system entity component-type)
                           [::proficiencies domain ::xp]
                           (partial + xp-delta))]
    (ecs/add-component system entity updated)))

(s/fdef get-proficiencies-domains
  :args (s/cat :system ::ecs/System :entity ::ecs/Entity)
  :ret (s/coll-of ::domain :kind set?))
(defn get-proficiencies-domains [system entity]
  (->> (ecs/get-component system entity component-type)
       ::proficiencies
       keys
       (into #{})))
