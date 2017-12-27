(ns dcs.components.singleton.system-rng
  (:require [clojure.spec.alpha :as s]
            [dcs.ecs :as ecs]
            [dcs.random :as r]
            [orchestra.spec.test :as st]))

(def ^:private component-type ::SystemRNG)

(ecs/def-component ::SystemRNGComponent
  (s/keys :req [::system-seed ::current-seed]))

(s/fdef create :ret ::SystemRNGComponent)
(defn- create [seed]
  (ecs/create-component
   component-type
   ::system-seed seed
   ::current-seed seed))

(defn add-singleton [system seed]
  (let [new-entity (ecs/create-entity)]
    (-> system
        (ecs/add-entity new-entity)
        (ecs/add-component new-entity (create seed)))))

(defn- get-system-rng [system]
  (ecs/get-singleton-component system component-type))

(defn- reseed-rng [system seed]
  (let [system-rng (ecs/get-singleton-component system component-type)]
    (->> (assoc system-rng ::current-seed seed)
         (ecs/overwrite-singleton-component system component-type))))

(defn- get-current-seed
  ([system]
   (::current-seed (get-system-rng system)))
  ([system bound]
   (mod (get-current-value system) bound)))

(defn- seeds-differ [{:keys [args ret]}]
  (not= (get-current-seed (:system args)) (get-current-seed (:system ret))))

(s/fdef next-int
  :ret (s/tuple ::ecs/System int?)
  :fn seeds-differ)
(defn next-int
  ([system]
   (let [rng (r/create-rng (get-current-seed system))
         next-seed (.nextInt rng)]
     [(reseed-rng system next-seed) next-seed]))
  ([system bound]
   (let [rng (r/create-rng (get-current-seed system))
         next-seed (.nextInt rng)]
     [(reseed-rng system next-seed) (mod next-seed bound)])))

(s/fdef shuffle
  :args (s/cat :system ::ecs/System :coll coll?)
  :ret (s/tuple ::ecs/System coll?)
  :fn seeds-differ)
(defn shuffle [system coll]
  (let [rng (r/create-rng (get-current-seed system))
        shuffled (r/seeded-shuffle rng coll)
        next-seed (.nextInt rng)]
    [(reseed-rng system next-seed) shuffled]))

(s/fdef rand-item
  :args (s/cat :system ::ecs/System :coll coll?)
  :ret (s/tuple ::ecs/System any?)
  :fn seeds-differ)
(defn rand-item [system coll]
  (let [[shuffled-sys shuffled-coll] (shuffle system coll)]
    [shuffled-sys (first shuffled-coll)]))

(defn print-current-seed
  ([system] (prn (get-current-seed system)) system)
  ([system bound] (prn (get-current-seed system bound)) system))

(do (st/instrument) nil)
