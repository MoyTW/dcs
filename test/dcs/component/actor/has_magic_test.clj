(ns dcs.components.actor.has-magic-test
  (:require [brute.entity :as e]
            [clojure.data :as data]
            [clojure.test :refer :all]
            [dcs.components.actor.has-magic :as has-magic]))

(deftest test-change-proficiency-xp
  (let [entity #uuid "1a689745-43f6-4dd9-99cd-6b4201602b93"
        component (has-magic/create
                   {:fire (has-magic/create-proficiency :fire 1.0 500)
                    :water (has-magic/create-proficiency :water 1.5 100)})
        old-sys (-> (e/create-system)
                    (e/add-entity entity)
                    (e/add-component entity component))
        new-sys (has-magic/change-proficiency-xp old-sys entity :fire 250)]
    (is (= [{:entity-components
             {dcs.components.actor.has_magic.HasMagic
              {entity
               {:proficiencies
                {:fire #:dcs.components.actor.has-magic{:xp 500}}}}}}
            {:entity-components
             {dcs.components.actor.has_magic.HasMagic
              {entity
               {:proficiencies
                {:fire #:dcs.components.actor.has-magic{:xp 750}}}}}}]
           (take 2 (data/diff old-sys new-sys))))))
