(ns dcs.components.singleton.template-dictionary
  (:require [clojure.spec.alpha :as s]
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
            [dcs.ecs :as ecs]
            [dcs.random :as r]
            [orchestra.spec.test :as st]))

(def counter (atom 0))

(defn- next-int [] (swap! counter inc))

(def ^:private component-type ::TemplateDictionary)

(defn throw-no-non-void-locations! []
  (throw (Exception. (str "Could not stamp ::HasLocationNonVoid;"
                          " there are no non-void locations!"))))

(defn has-location-non-void [system rng]
  (if-let [locations (seq (is-location/get-non-void-locations system))]
    (->> (r/seeded-rand-item rng)
         (has-location/create system))
    (throw-no-non-void-locations!)))

(defn has-magic-human-summoner [_ rng]
  (has-magic/create-rand-has-magic rng 2 3))

(def ^:private fixed-component-keywords->templates
  { ;; Inventory
   ::HasInventorySize10 (fn [_ _] (has-inventory/create 10))
   
   ;; Humans
   ::IsHuman (fn [_ _] (is-human/create))
   ::HasNameHuman (fn [_ _] (has-name/create (str "human " (next-int))))
   ::HasCapacity (fn [_ _] (has-capacity/create []))
   ;; Requires that there by non-void locations available!
   ::HasLocationNonVoid has-location-non-void
   ::HasMagicHumanSummoner has-magic-human-summoner})

(def ^:private fixed-entity-keywords->templates
  {::Summoner [::IsHuman
               ::HasNameHuman
               ::HasLocationNonVoid
               ::HasMagicHumanSummoner
               ::HasInventorySize10]})

(s/def ::component-template fn?)
(s/def ::component-ids->templates (s/map-of keyword? ::component-template))

(s/def ::entity-template (s/coll-of keyword?))
(s/def ::entity-keywords->templates (s/map-of keyword? ::entity-template))

(ecs/def-component ::TemplateDictionaryComponent
  (s/and
   (s/keys :req [::component-keywords->templates
                 ::entity-keywords->templates])
   (fn entity-templates-consistent-with-components
     [{:keys [::component-keywords->templates ::entity-keywords->templates]}]
     (->> (mapcat second entity-keywords->templates)
          (every? #(contains? component-keywords->templates %))))))

(s/fdef create :ret ::TemplateDictionaryComponent)
(defn create- []
  (ecs/create-component
   component-type
   ::component-keywords->templates fixed-component-keywords->templates
   ::entity-keywords->templates fixed-entity-keywords->templates))

(defn add-singleton [system]
  (let [new-entity (ecs/create-entity)]
    (-> system
        (ecs/add-entity new-entity)
        (ecs/add-component new-entity (create)))))

(defn throw-invalid-entity-keyword! [entity-keyword]
  (throw (IllegalArgumentException.
          (format "Could not stamp %1$s; %1$s was not a valid entity-keyword!"
                  entity-keyword)))  )

(s/fdef stamp
  :args (s/cat :system ::ecs/System :rng any? :entity-keyword keyword?)
  :ret ::ecs/System)
(defn stamp [system rng entity-keyword]
  (let [dictionary (ecs/get-singleton-component system component-type)
        template (entity-keyword (::entity-keywords->templates dictionary))
        new-entity (ecs/create-entity)]
    (when (nil? template) (throw-invalid-entity-keyword! entity-keyword))
    (reduce (fn [sys component-keyword]
              (let [f (-> (::component-keywords->templates dictionary)
                          (get component-keyword))]
                (ecs/add-component system new-entity (f sys rng))))
            (ecs/add-entity system new-entity)
            template)))

(do (st/instrument) nil)
#_(stamp (add-singleton (ecs/create-system)) (r/create-rng 3) ::Summonera)
#_(stamp (add-singleton (ecs/create-system)) (r/create-rng 3) ::Summoner)
