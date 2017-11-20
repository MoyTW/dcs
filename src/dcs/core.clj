(ns dcs.core
  (:gen-class)
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]
            [dcs.contract-template :as ct]
            [dcs.components.inventory :as inventory]
            [dcs.components.capacity :as capacity]
            [dcs.components.magic :as magic]
            [orchestra.spec.test :as st]))

;; *****************************************************************************
;; * MAGIC COMPONENT *
;; *****************************************************************************

(defn create-rng [seed]
  (java.util.Random. seed))

(defn seeded-shuffle [^java.util.Random rng ^java.util.Collection coll]
  (let [al (java.util.ArrayList. coll)]
    (java.util.Collections/shuffle al rng)
    (clojure.lang.RT/vector (.toArray al))))

(defn seeded-next-int
  ([^java.util.Random rng]
   (.nextInt rng))
  ([^java.util.Random rng bound]
   (.nextInt rng bound)))

(defn- create-proficiency [^java.util.Random rng max-level domain]
  {::magic/domain domain
   ::magic/aptitude (first (seeded-shuffle rng magic/aptitude))
   ::magic/xp (seeded-next-int rng (magic/xp-for-level max-level))})

(defn- create-rand-magic
  [^java.util.Random rng max-domains max-level]
  (->> (seeded-shuffle rng magic/domain)
       (take (inc (seeded-next-int rng max-domains)))
       (map (partial create-proficiency rng max-level))
       magic/create))

(defn- generate-summoner [system rng]
  (let [summoner (e/create-entity)]
    (-> system
        (e/add-entity summoner)
        (e/add-component summoner (inventory/create [] 10))
        (e/add-component summoner (capacity/create []))
        (e/add-component summoner (create-rand-magic rng 2 3)))))

(defn- generate-devil [system]
  (let [devil (e/create-entity)]
    (-> system
        (e/add-entity devil)
        (e/add-component devil (inventory/create [] 50))
        (e/add-component devil (capacity/create []))
        (e/add-component devil (magic/create [])))))

(defn- seed-world [system]
  (reduce (fn [system _] (generate-devil system))
          (e/create-system)
          (take 10 (repeat 1))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [rng (java.util.Random. 1)
        sys (seed-world (e/create-system))]
    (clojure.pprint/pprint sys)))

(st/instrument)
