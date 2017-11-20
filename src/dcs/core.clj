(ns dcs.core
  (:gen-class)
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]
            [dcs.contract-template :as ct]
            [orchestra.spec.test :as st]))

;; *****************************************************************************
;; * INVENTORY COMPONENT *
;; *****************************************************************************

(s/def ::max-size int?)
(s/def ::contents (s/coll-of [::item-id]))

(s/def ::inventory (s/keys :req-un [::contents ::max-size]))

(s/fdef ->Inventory
  :args (s/cat :contents ::contents :max-size ::max-size)
  :ret ::inventory)

(defrecord Inventory [contents max-size])

;; *****************************************************************************
;; * CAPACITY COMPONENT *
;; *****************************************************************************

(s/def ::contracts (s/coll-of [::item-id]))

(s/def ::capacity (s/keys :req-un [::contracts]))

(s/fdef ->Capacity
  :args (s/cat :contracts ::contracts)
  :ret ::capacity)

(defrecord Capacity [contracts])

;; *****************************************************************************
;; * MAGIC COMPONENT *
;; *****************************************************************************

(s/def ::domain #{:wood :fire :earth :metal :water})
(s/def ::potential #{0.5 0.75 1.0 1.5 2})
(s/def ::xp int?)
(s/def ::proficiency (s/keys :req-un [::domain ::xp ::aptitude]))

(s/def ::proficiencies (s/coll-of ::proficiency))

(s/fdef ->Magic
  :args (s/cat :domains ::proficiencies))

(defrecord Magic [proficiencies])

(defn- generate-devil [system]
  (let [devil (e/create-entity)]
    (-> system
        (e/add-entity devil)
        (e/add-component devil (->Inventory [] 5))
        (e/add-component devil (->Capacity []))
        (e/add-component devil (->Magic [])))))

(defn- seed-world [system]
  (reduce (fn [system _] (generate-devil system))
          (e/create-system)
          (take 10 (repeat 1))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [sys (seed-world (e/create-system))]
    (clojure.pprint/pprint sys)))

(st/instrument)
