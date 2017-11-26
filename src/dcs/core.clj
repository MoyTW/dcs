(ns dcs.core
  (:gen-class)
  (:require [brute.system :as bs]
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
            [dcs.ecs :as ecs]
            [dcs.random :as r]
            [orchestra.spec.test :as st]))

(def c (atom 0))

(defn- next-int [] (swap! c inc))

(defn- generate-town
  [system new-entity town-name & components]
  (let [system-with-new-town
        (-> system
            (ecs/add-entity new-entity)
            (ecs/add-component new-entity (has-name/create town-name))
            (ecs/add-component new-entity (is-location/create)))]
    (reduce #(ecs/add-component %1 new-entity %2) system-with-new-town components)))

(defn- generate-summoner [system rng]
  (let [void (first (ecs/get-entities-with-component
                     system
                     is-void/component-type))
        hometown (->> (ecs/get-entities-with-component
                       system
                       is-location/component-type)
                      (remove #{void})
                      (r/seeded-rand-item rng))
        summoner (ecs/create-entity)]
    (-> system
        (ecs/add-entity summoner)
        (ecs/add-component summoner (has-location/create system hometown))
        (ecs/add-component summoner (has-name/create (str "summoner " (next-int))))
        (ecs/add-component summoner (is-human/create))
        (ecs/add-component summoner (has-inventory/create [] 10))
        (ecs/add-component summoner (has-capacity/create []))
        (ecs/add-component summoner (has-magic/create-rand-has-magic rng 2 3)))))

(defn- generate-devil [system]
  (let [devil (ecs/create-entity)
        void (first (ecs/get-entities-with-component
                     system
                     is-void/component-type))]
    (-> system
        (ecs/add-entity devil)
        (ecs/add-component devil (has-location/create system void))
        (ecs/add-component devil (is-devil/create))
        (ecs/add-component devil (has-name/create (str "devil " (next-int))))
        (ecs/add-component devil (is-voidborn/create))
        (ecs/add-component devil (has-inventory/create [] 50))
        (ecs/add-component devil (has-capacity/create []))
        (ecs/add-component devil (has-magic/create {})))))

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
  (let [void (ecs/create-entity)
        berlin (ecs/create-entity)
        london (ecs/create-entity)
        moscow (ecs/create-entity)]
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
  (->> (ecs/get-components-on-entity system entity)
       (map (fn [c] [(last (clojure.string/split (str (type c)) #"\.")) c]))
       (into {})))

(defn- seed-world [system rng]
  (let [new-system (-> (ecs/create-system)
                       (build-locations rng))
        seeded (reduce (fn [sys _]
                         (-> sys
                             (generate-summoner rng)
                             generate-devil))
                       new-system
                       (take 1 (repeat 1)))]
    (prn "World was seeded.")
    (prn "Locations")
    (clojure.pprint/pprint
     (for [location (ecs/get-entities-with-component
                     seeded
                     is-location/component-type)]
       (component-map seeded location)))
    (prn "Humans")
    (clojure.pprint/pprint
     (for [human (ecs/get-entities-with-component
                  seeded
                  is-human/component-type)]
       (component-map seeded human)))
    (prn "Devils")
    (clojure.pprint/pprint
     (for [devil (ecs/get-entities-with-component
                  seeded
                  is-devil/component-type)]
       (component-map seeded devil)))
    seeded))

;; We don't actually want to have different systems for summoners/devils; rather
;; we want to group them under a "Actor" or "AI" Component. This is just for
;; test purposes.
(defn- summoners-system [rng system ticks]
  (let [summoners (ecs/get-entities-with-component
                   system
                   is-human/component-type)
        get-component #(ecs/get-component system %1 %2)]
    (reduce (fn [sys summoner]
              (let [location-actions (->> (has-location/get-location sys summoner)
                                          (provides-action/get-actions sys))
                    summoner-actions (provides-action/get-actions sys summoner)
                    actions (concat location-actions summoner-actions)
                    action (r/seeded-rand-item rng actions)]
                (provides-action/execute-action sys action summoner)))
            system
            summoners)))

(defn- devils-system [system ticks]
  (let [devils (ecs/get-entities-with-component
                system
                is-devil/component-type)]
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
  (mapv #(if (entity? %) (has-name/get-name sys %) %) o))

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
  (let [next-sys (bs/process-one-game-tick sys delta)
        [only-sys only-next-sys _] (take 2 (data/diff sys next-sys))]
    (clojure.pprint/pprint (uuids->names sys only-sys))
    (clojure.pprint/pprint (uuids->names next-sys only-next-sys))
    (println)
    next-sys))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [rng (r/create-rng 3)
        sys (-> ecs/create-system
                (seed-world rng)
                (add-systems rng))]
    (-> sys
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10)
        (advance 10))
    "end"))

(st/instrument)
