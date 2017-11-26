(ns dcs.ecs
  (:require [brute.entity :as e]
            [brute.system :as bs]
            [clojure.spec.alpha :as s])
  (:import (java.util UUID)
           (clojure.lang PersistentArrayMap)))

(defmethod e/get-component-type PersistentArrayMap
  [component]
  (::component-type component))

(s/def ::component-type keyword?)

(s/def ::Component (s/keys :req [::component-type]))

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
