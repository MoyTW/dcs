(ns dcs.debug
  (:require [clojure.data :as data]
            [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [dcs.components.has-name :as has-name]))

(defn- entity? [e]
  (= (type e) java.util.UUID))

(defn- prettify-obj [sys o]
  (mapv #(cond
           (entity? %)
           (has-name/get-name sys %)

           (qualified-keyword? %)
           (name %)

           :else
           %)
        o))

(defn- prettify-diff
  "Walks the diff and prettifies it for easy printing"
  [sys m]
  (let [transform #(prettify-obj sys %)]
    (walk/postwalk
     (fn [obj]
       (if (map? obj)
         (into {} (map transform obj))
         obj))
     m)))

(defn print-pretty-diff! [old-sys new-sys]
  (let [[only-old-sys only-new-sys _] (take 2 (data/diff old-sys new-sys))]
    (pp/pprint (prettify-diff old-sys only-old-sys))
    (pp/pprint (prettify-diff new-sys only-new-sys))
    (println)))
