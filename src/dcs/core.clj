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

(defn- generate-devil [system]
  (let [devil (e/create-entity)]
    (-> system
        (e/add-entity devil)
        (e/add-component devil (inventory/create [] 5))
        (e/add-component devil (capacity/create []))
        (e/add-component devil (magic/create [])))))

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
