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

(def domain #{:wood :fire :earth :metal :water})
(def aptitude #{0.5 0.75 1.0 1.5 2})

(s/def ::domain domain)
(s/def ::aptitude aptitude)
(s/def ::xp int?)
(s/def ::proficiency (s/keys :req [::domain ::xp ::aptitude]))

(s/def ::proficiencies (s/coll-of ::proficiency))

(s/fdef ->HasMagic
  :args (s/cat :proficiencies ::proficiencies))

(defrecord HasMagic [proficiencies])

(defn create [proficiencies]
  (->HasMagic proficiencies))

(defn- create-proficiency [^java.util.Random rng max-level domain]
  {::domain domain
   ::aptitude (first (r/seeded-shuffle rng aptitude))
   ::xp (r/seeded-next-int rng (xp-for-level max-level))})

(defn create-rand-has-magic
  [^java.util.Random rng max-domains max-level]
  (->> (r/seeded-shuffle rng domain)
       (take (inc (r/seeded-next-int rng max-domains)))
       (map (partial create-proficiency rng max-level))
       create))
