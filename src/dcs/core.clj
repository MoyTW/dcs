(ns dcs.core
  (:gen-class)
  (:require [brute.entity :as e]
            [brute.system :as bs]
            [clojure.spec.alpha :as s]
            [dcs.contract-template :as ct]
            [dcs.components.has-name :as has-name]
            [dcs.components.has-location :as has-location]
            [dcs.components.actor.has-inventory :as has-inventory]
            [dcs.components.actor.has-capacity :as has-capacity]
            [dcs.components.actor.has-magic :as has-magic]
            [dcs.components.actor.is-devil :as is-devil]
            [dcs.components.actor.is-human :as is-human]
            [dcs.components.actor.is-voidborn :as is-voidborn]
            [dcs.components.location.is-location :as is-location]
            [dcs.components.location.is-void :as is-void]
            [dcs.random :as r]
            [orchestra.spec.test :as st])
  ;; defrecords are implemented /w Java, so must use import
  (:import [dcs.components.actor.is_devil IsDevil]
           [dcs.components.actor.is_human IsHuman]
           [dcs.components.location.is_location IsLocation]
           [dcs.components.location.is_void IsVoid]
           [dcs.components.has_location HasLocation]
           [dcs.components.has_name HasName]))

(def c (atom 0))

(defn- next-int [] (swap! c inc))

(defn- generate-town
  [system & components]
  (let [town (e/create-entity)
        system-with-new-town
        (-> system
            (e/add-entity town)
            (e/add-component town (has-name/create (str "town " (next-int))))
            (e/add-component town (is-location/create)))]
    (reduce #(e/add-component %1 town %2) system-with-new-town components)))

(defn- generate-summoner [system rng]
  (let [void (first (e/get-all-entities-with-component system IsVoid))
        hometown (->> (e/get-all-entities-with-component system IsLocation)
                      (remove #{void})
                      (r/seeded-rand-item rng))
        summoner (e/create-entity)]
    (-> system
        (e/add-entity summoner)
        (e/add-component summoner (has-location/create system hometown))
        (e/add-component summoner (has-name/create (str "summoner " (next-int))))
        (e/add-component summoner (is-human/create))
        (e/add-component summoner (has-inventory/create [] 10))
        (e/add-component summoner (has-capacity/create []))
        (e/add-component summoner (has-magic/create-rand-has-magic rng 2 3)))))

(defn- generate-devil [system]
  (let [devil (e/create-entity)
        void (first (e/get-all-entities-with-component system IsVoid))]
    (-> system
        (e/add-entity devil)
        (e/add-component devil (has-location/create system void))
        (e/add-component devil (is-devil/create))
        (e/add-component devil (has-name/create (str "devil " (next-int))))
        (e/add-component devil (is-voidborn/create))
        (e/add-component devil (has-inventory/create [] 50))
        (e/add-component devil (has-capacity/create []))
        (e/add-component devil (has-magic/create [])))))

(defn- build-locations [system rng]
  (-> system
      ;; void
      (generate-town (is-void/create) (has-name/create "THE VOID (spoooooky)"))
      ;; some towns
      (generate-town)
      (generate-town)
      (generate-town)))

(defn- seed-world [system rng]
  (let [new-system (-> (e/create-system)
                       (build-locations rng))
        seeded (reduce (fn [sys _]
                         (-> sys
                             (generate-summoner rng)
                             generate-devil))
                       new-system
                       (take 10 (repeat 1)))]
    (clojure.pprint/pprint
     (for [human (e/get-all-entities-with-component seeded IsHuman)]
       (name-and-location seeded human)))
    (clojure.pprint/pprint
     (for [devil (e/get-all-entities-with-component seeded IsDevil)]
       (name-and-location seeded devil)))
    seeded))

(defn- land-system [system ticks]
  (let [l (e/get-all-entities-with-component system IsLocation)]
    system))

(defn- name-and-location [system entity]
  (let [get-component #(e/get-component system %1 %2)]
    [(get-component entity HasName)
     (-> (get-component entity HasLocation)
         :location
         (get-component HasName))]))

;; We don't actually want to have different systems for summoners/devils; rather
;; we want to group them under a "Actor" or "AI" Component. This is just for
;; test purposes.
(defn- summoners-system [system ticks]
  (let [summoners (e/get-all-entities-with-component system IsHuman)]
    system))

(defn- devils-system [system ticks]
  (let [devils (e/get-all-entities-with-component system IsDevil)]
    system))

(defn- add-systems
  "Add system functions to the system (that's...kinda confusing)"
  [system]
  (-> system
      #_(bs/add-system-fn land-system)
      (bs/add-system-fn summoners-system)
      (bs/add-system-fn devils-system)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [rng (r/create-rng 2)
        sys (-> e/create-system
                (seed-world rng)
                add-systems)]
    (-> sys
        (bs/process-one-game-tick 10)
        (bs/process-one-game-tick 1)
        (bs/process-one-game-tick 25))
    "end"))

(st/instrument)
