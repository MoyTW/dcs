(ns dcs.components.actor.has-magic-test
  (:require [clojure.data :as data]
            [clojure.test :refer :all]
            [dcs.components.actor.has-magic :as has-magic]
            [dcs.ecs :as ecs]
            [orchestra.spec.test :as st]))

(defn with-specs [f]
  (st/instrument)
  (f))

(use-fixtures :each with-specs)

(deftest test-change-proficiency-xp
  (let [entity (ecs/define-entity "1a689745-43f6-4dd9-99cd-6b4201602b93")
        component (has-magic/create
                   (has-magic/merge-proficiencies
                    [(has-magic/create-proficiency :fire 1.0 500)
                     (has-magic/create-proficiency :water 1.5 100)]))
        old-sys (-> (ecs/create-system)
                    (ecs/add-entity entity)
                    (ecs/add-component entity component))
        new-sys (has-magic/change-proficiency-xp old-sys entity :fire 250)]
    ;; TODO: lol look at that formatting...
    (is (= [{:entity-components
             #:dcs.components.actor.has-magic
             {:HasMagic
              {#uuid "1a689745-43f6-4dd9-99cd-6b4201602b93"
               #:dcs.components.actor.has-magic
               {:proficiencies
                {:fire
                 #:dcs.components.actor.has-magic{:xp 500}}}}}}
            {:entity-components
             #:dcs.components.actor.has-magic
             {:HasMagic
              {#uuid "1a689745-43f6-4dd9-99cd-6b4201602b93"
               #:dcs.components.actor.has-magic
               {:proficiencies
                {:fire
                 #:dcs.components.actor.has-magic{:xp 750}}}}}}]
           (take 2 (data/diff old-sys new-sys))))))
