(ns dcs.core
  (:gen-class)
  (:require [brute.entity :as e]
            [brute.system :as bs]
            [clojure.spec.alpha :as s]
            [dcs.contract-template :as ct]
            [dcs.components.has-name :as has-name]
            [dcs.components.actor.has-inventory :as has-inventory]
            [dcs.components.actor.has-capacity :as has-capacity]
            [dcs.components.actor.has-magic :as has-magic]
            [dcs.components.actor.is-devil :as is-devil]
            [dcs.components.actor.is-human :as is-human]
            [dcs.components.actor.is-voidborn :as is-voidborn]
            [dcs.random :as r]
            [orchestra.spec.test :as st])
  ;; defrecords are implemented /w Java, so must use import
  (:import [dcs.components.actor.is_devil IsDevil]
           [dcs.components.actor.is_human IsHuman]))

(def c (atom 0))

(defn- next-int [] (swap! c inc))

(defn- generate-summoner [system rng]
  (let [summoner (e/create-entity)]
    (-> system
        (e/add-entity summoner)
        (e/add-component summoner (has-name/create (str "summoner " (next-int))))
        (e/add-component summoner (is-human/create))
        (e/add-component summoner (has-inventory/create [] 10))
        (e/add-component summoner (has-capacity/create []))
        (e/add-component summoner (has-magic/create-rand-has-magic rng 2 3)))))

(defn- generate-devil [system]
  (let [devil (e/create-entity)]
    (-> system
        (e/add-entity devil)
        (e/add-component devil (is-devil/create))
        (e/add-component devil (has-name/create (str "devil " (next-int))))
        (e/add-component devil (is-voidborn/create))
        (e/add-component devil (has-inventory/create [] 50))
        (e/add-component devil (has-capacity/create []))
        (e/add-component devil (has-magic/create [])))))

(defn- seed-world [system rng]
  (reduce (fn [system _]
            (-> system
                (generate-summoner rng)
                generate-devil))
          (e/create-system)
          (take 10 (repeat 1))))

;; We don't actually want to have different systems for summoners/devils; rather
;; we want to group them under a "Actor" or "AI" Component. This is just for
;; test purposes.
(defn- summoners-system [system ticks]
  (let [summoners (e/get-all-entities-with-component system IsHuman)]
    (prn :SUMMONERS :ticks ticks)
    (clojure.pprint/pprint summoners)
    system))

(defn- devils-system [system ticks]
  (let [devils (e/get-all-entities-with-component system IsDevil)]
    (prn :DEVILS :ticks ticks)
    (clojure.pprint/pprint devils)
    system))

(defn- add-systems
  "Add system functions to the system (that's...kinda confusing)"
  [system]
  (-> system
      (bs/add-system-fn summoners-system)
      (bs/add-system-fn devils-system)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [rng (r/create-rng 1)
        sys (-> e/create-system
                (seed-world rng)
                add-systems)]
    (-> sys
        (bs/process-one-game-tick 10)
        (bs/process-one-game-tick 1)
        (bs/process-one-game-tick 25))
    "end"))

(st/instrument)
