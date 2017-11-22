(ns dcs.core
  (:gen-class)
  (:require [brute.entity :as e]
            [clojure.spec.alpha :as s]
            [dcs.contract-template :as ct]
            [dcs.components.actor.has-inventory :as has-inventory]
            [dcs.components.actor.has-capacity :as has-capacity]
            [dcs.components.actor.has-magic :as has-magic]
            [dcs.components.actor.is-devil :as is-devil]
            [dcs.components.actor.is-human :as is-human]
            [dcs.components.actor.is-voidborn :as is-voidborn]
            [dcs.random :as r]
            [orchestra.spec.test :as st]))

(defn- generate-summoner [system rng]
  (let [summoner (e/create-entity)]
    (-> system
        (e/add-entity summoner)
        (e/add-component summoner (is-human/create))
        (e/add-component summoner (has-inventory/create [] 10))
        (e/add-component summoner (has-capacity/create []))
        (e/add-component summoner (has-magic/create-rand-has-magic rng 2 3)))))

(defn- generate-devil [system]
  (let [devil (e/create-entity)]
    (-> system
        (e/add-entity devil)
        (e/add-component devil (is-devil/create))
        (e/add-component devil (is-voidborn/create))
        (e/add-component devil (has-inventory/create [] 50))
        (e/add-component devil (has-capacity/create []))
        (e/add-component devil (has-magic/create [])))))

(defn- seed-world [system]
  (reduce (fn [system _] (generate-devil system))
          (e/create-system)
          (take 10 (repeat 1))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [rng (r/create-rng 1)
        sys (seed-world (e/create-system))]
    (clojure.pprint/pprint sys)))

(st/instrument)
