(ns dcs.components.magic
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]))

(s/def ::domain #{:wood :fire :earth :metal :water})
(s/def ::potential #{0.5 0.75 1.0 1.5 2})
(s/def ::xp int?)
(s/def ::proficiency (s/keys :req-un [::domain ::xp ::aptitude]))

(s/def ::proficiencies (s/coll-of ::proficiency))

(s/fdef ->Magic
  :args (s/cat :domains ::proficiencies))

(defrecord Magic [proficiencies])

(defn create [proficiencies]
  (->Magic proficiencies))
