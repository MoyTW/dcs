(ns dcs.core
  (:gen-class)
  (:require [brute.entity :as e]
            [brute.system :as bs]
            [clojure.spec.alpha :as s]
            [clojure.data :as data]
            [clojure.walk :as walk]
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
        (e/add-component devil (has-magic/create {})))))

(defn- add-travel-action
  "Adds one-way travel link"
  [system origin destination]
  (->> (provides-action/create-travel-action system origin destination)
       (provides-action/add-provided-action system origin)))

(defn- add-train-proficiency-action
  [system entity available-domains min-xp max-xp]
  (->> (provides-action/create-train-proficiency-action available-domains
                                                        min-xp
                                                        max-xp)
       (provides-action/add-provided-action system entity)))

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

        (add-travel-action berlin london)
        (add-travel-action berlin moscow)
        (add-train-proficiency-action berlin #{:earth :water} 100 500)

        (add-travel-action london berlin)
        (add-train-proficiency-action berlin #{:metal :wood} 100 500)

        (add-travel-action moscow berlin)
        (add-train-proficiency-action berlin #{:fire :metal} 100 500))))

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
(defn- summoners-system [rng system ticks]
  (let [summoners (e/get-all-entities-with-component system IsHuman)
        get-component #(e/get-component system %1 %2)]
    (reduce (fn [sys summoner]
              (let [location-actions (-> (get-component summoner has-location/record)
                                         :location
                                         (get-component provides-action/record)
                                         :actions)
                    summoner-actions (-> (get-component summoner provides-action/record)
                                         :actions)
                    actions (concat location-actions summoner-actions)
                    action (r/seeded-rand-item rng actions)]
                (provides-action/execute-action sys action summoner)))
            system
            summoners)))

(defn- devils-system [system ticks]
  (let [devils (e/get-all-entities-with-component system IsDevil)]
    system))

(defn- add-systems
  "Add system functions to the system (that's...kinda confusing)"
  [system rng]
  (-> system
      (bs/add-system-fn (partial summoners-system rng))
      (bs/add-system-fn devils-system)))

(defn- entity? [e]
  (= (type e) java.util.UUID))

(defn obj->name-if-uuid [sys o]
  (mapv #(if (entity? %) (e/get-component sys % has-name/record) %) o))

(defn uuids->names
  "Recursively transforms all map UUID values to names"
  [sys m]
  (let [transform #(obj->name-if-uuid sys %)]
    (walk/postwalk
     (fn [obj]
       (if (map? obj)
         (into {} (map transform obj))
         obj))
     m)))

(defn- advance [sys delta]
  (let [next-sys (bs/process-one-game-tick sys delta)]
    (clojure.pprint/pprint (uuids->names next-sys (take 2 (data/diff sys next-sys))))
    next-sys))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [rng (r/create-rng 2)
        sys (-> e/create-system
                (seed-world rng)
                (add-systems rng))]
    (-> sys
        (advance 10)
        (advance 1)
        (advance 25))
    "end"))

(st/instrument)
