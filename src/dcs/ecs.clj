(ns dcs.ecs
  (:require [brute.entity :as e]
            [brute.system :as bs]
            [clojure.spec.alpha :as s])
  (:import (java.util UUID)
           (clojure.lang PersistentArrayMap)))

;; Define Entity, Component, System
;;
;; They're capitalized because I'm having a hell of a time reading specs, and
;; it's just freaking hard to read them. Capitalizing the Entity, Component, and
;; System specs is just so I can find them easily.

(s/def ::Entity uuid?)

(s/def ::component-type qualified-keyword?)
(s/def ::Component (s/keys :req [::component-type]))

(s/def ::entity-components map?)
(s/def ::entity-component-types map?)
(s/def ::System (s/keys :req-un [::entity-components ::entity-component-types]))

(defmacro def-component
  "Adds the Component spec to the spec-form"
  [k spec-form]
  `(s/def ~k (s/and ::Component ~spec-form)))

(s/fdef create-component :ret ::Component)
(defn create-component [component-type & kvs]
  (->> (partition 2 kvs)
       (map vec)
       (into {})
       (merge {::component-type component-type})))

(s/fdef define-entity :args (s/cat :entity string?) :ret ::Entity)
(defn define-entity [entity]
  (UUID/fromString entity))

;; #############################################################################
;; # Brute Entity Wrappers                                                     #
;; #############################################################################

(defmethod e/get-component-type PersistentArrayMap
  [component]
  (::component-type component))

(s/fdef create-system :ret ::System)
(defn create-system [] (e/create-system))

(s/fdef create-entity :ret ::Entity)
(defn create-entity [] (e/create-entity))

(s/fdef add-entity
  :args (s/cat :system ::System :entity ::Entity)
  :ret ::System)
(defn add-entity [system entity] (e/add-entity system entity))

(s/fdef add-component
  :args (s/cat :system ::System
               :entity ::Entity
               :component ::Component)
  :ret ::System)
(defn add-component [system entity component]
  (e/add-component system entity component))

(s/fdef get-component
  :args (s/cat :system ::System
               :entity ::Entity
               :component-type ::component-type)
  :ret (s/or :nil nil? :component ::Component))
(defn get-component [system entity component-type]
  (e/get-component system entity component-type))

(s/fdef get-all-entities-with-component
  :args (s/cat :system ::System
               :component-type ::component-type)
  :ret (s/coll-of ::Entity))
(defn get-entities-with-component [system component-type]
  (e/get-all-entities-with-component system component-type))

(s/fdef get-components-on-entity
  :args (s/cat :system ::System
               :entity ::Entity)
  :ret (s/coll-of ::Component))
(defn get-components-on-entity [system entity]
  (e/get-all-components-on-entity system entity))

(defn get-singleton-component [system component-type]
  (->> (get-entities-with-component system component-type)
       first
       (get-components-on-entity system)
       first))

(defn overwrite-singleton-component [system component-type component]
  (let [entity (first (get-entities-with-component system component-type))]
    (add-component system entity component)))

;; #############################################################################
;; # Brute System Wrappers                                                     #
;; #############################################################################

(defn add-system-fn [system fn] (bs/add-system-fn system fn))

(defn process-one-game-tick [system delta]
  (bs/process-one-game-tick system delta))
