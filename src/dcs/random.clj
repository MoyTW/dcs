(ns dcs.random)

(defn create-rng [seed]
  (java.util.Random. seed))

(defn seeded-shuffle [^java.util.Random rng ^java.util.Collection coll]
  (let [al (java.util.ArrayList. coll)]
    (java.util.Collections/shuffle al rng)
    (clojure.lang.RT/vector (.toArray al))))

(defn seeded-next-int
  ([^java.util.Random rng]
   (.nextInt rng))
  ([^java.util.Random rng bound]
   (.nextInt rng bound)))

(defn seeded-rand-item
  "Shuffles the collection and takes the first item. Somewhat inefficient on data
  structures which already have indexed access, but if it becomes a problem it's
  easy to optimize out!"
  [^java.util.Random rng ^java.util.Collection coll]
  (first (seeded-shuffle rng coll)))
