(ns dcs.components.actor.has-magic
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]
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

(def domain-values #{:wood :fire :earth :metal :water})
(def aptitude-values #{0.5 0.75 1.0 1.5 2})

(s/def ::domain domain-values)
(s/def ::aptitude aptitude-values)
(s/def ::xp int?)
(s/def ::proficiency (s/keys :req [::domain ::xp ::aptitude]))

(s/def ::proficiencies (s/map-of ::domain ::proficiency))

(s/fdef ->HasMagic
  :args (s/cat :proficiencies ::proficiencies))

(defrecord HasMagic [proficiencies])

(defn create [proficiencies]
  (->HasMagic proficiencies))

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
                      (first (r/seeded-shuffle rng aptitude))
                      (r/seeded-next-int rng (xp-for-level max-level))))

(defn create-rand-has-magic
  [^java.util.Random rng max-domains max-level]
  (->> (r/seeded-shuffle rng domain)
       (take (inc (r/seeded-next-int rng max-domains)))
       (map (partial create-rand-proficiency rng max-level))
       merge-proficiencies
       create))

(s/fdef change-proficiency-xp
  :args (s/cat :system map?
               :entity (fn [_] true)
               :domain ::domain
               :xp-delta int?))
(defn change-proficiency-xp
  [system entity domain xp-delta]
  (let [updated (update-in (e/get-component system entity HasMagic)
                           [:proficiencies domain ::xp]
                           (partial + xp-delta))]
    (e/add-component system entity updated)))
