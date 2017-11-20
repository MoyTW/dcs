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
