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

(s/def ::Entity #(instance? UUID %))

(s/def ::component-type qualified-keyword?)
(s/def ::Component (s/keys :req [::component-type]))

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

(defmethod e/get-component-type PersistentArrayMap
  [component]
  (::component-type component))

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
  :ret ::Component)
(defn get-component [system entity component-type]
  (e/get-component system entity component-type))
