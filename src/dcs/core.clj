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
            [dcs.components.provides-action :as provides-action]
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
  [system new-entity town-name & components]
  (let [system-with-new-town
        (-> system
            (e/add-entity new-entity)
            (e/add-component new-entity (has-name/create town-name))
            (e/add-component new-entity (is-location/create)))]
    (reduce #(e/add-component %1 new-entity %2) system-with-new-town components)))

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

(defn- add-travel-action-provider
  "Adds one-way travel link"
  [system origin destination]
  (let [travel-action (provides-action/create-travel-action system
                                                            origin
                                                            destination)
        ;; Extremely ugly. Don't handle this here!
        component (if-let [c (e/get-component system
                                              origin
                                              provides-action/record)]
                    (update c :actions conj travel-action)
                    (provides-action/create [travel-action]))]
    (-> system
        (e/add-component origin component))))

(defn- build-locations [system rng]
  (let [void (e/create-entity)
        berlin (e/create-entity)
        london (e/create-entity)
        moscow (e/create-entity)]
    (-> system
        (generate-town void "THE VOID (spoooooky)" (is-void/create))
        (generate-town berlin "Berlin")
        (generate-town london "London")
        (generate-town moscow "Moscow")
        (add-travel-action-provider berlin london)
        (add-travel-action-provider london berlin)
        (add-travel-action-provider moscow berlin)
        (add-travel-action-provider berlin moscow))))

(defn- component-map [system entity]
  (->> (e/get-all-components-on-entity system entity)
       (map (fn [c] [(last (clojure.string/split (str (type c)) #"\.")) c]))
       (into {})))

(defn- seed-world [system rng]
  (let [new-system (-> (e/create-system)
                       (build-locations rng))
        seeded (reduce (fn [sys _]
                         (-> sys
                             (generate-summoner rng)
                             generate-devil))
                       new-system
                       (take 2 (repeat 1)))]
    (prn "World was seeded.")
    (prn "Locations")
    (clojure.pprint/pprint
     (for [location (e/get-all-entities-with-component seeded IsLocation)]
       (component-map seeded location)))
    (prn "Humans")
    (clojure.pprint/pprint
     (for [human (e/get-all-entities-with-component seeded IsHuman)]
       (component-map seeded human)))
    (prn "Devils")
    (clojure.pprint/pprint
     (for [devil (e/get-all-entities-with-component seeded IsDevil)]
       (component-map seeded devil)))
    seeded))

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
