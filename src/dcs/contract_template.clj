(ns dcs.contract-template
  (:require [clojure.spec.alpha :as s]))

(defonce last-id (atom 0))

(defn- next-id []
  (swap! last-id inc))

(s/def ::id int?)

(s/def ::party keyword?)

(s/def ::parties
  (s/and
   (s/coll-of ::party :kind set? :min-count 2)
   (fn has-multiple-parties [parties] (> (count parties) 1))))

(s/def ::days int?)
(s/def ::item-id int?)
(s/def ::quantity int?)
(s/def ::good (s/keys :req [::item-id ::quantity]))
(s/def ::service (s/keys :req [::service-id]
                         :opt [::target ::duration]))

(s/def ::products (s/coll-of (s/or :good ::good :service ::service)))

(s/def ::provider-products ::products)
(s/def ::beneficiary-products ::products)

(s/def ::clause
  (s/and
   (s/keys :req [::provider
                 ::provider-products
                 ::beneficiary
                 ::opens-trigger
                 ::closes-trigger]
           :opt [::beneficiary-products])
   (fn provider-is-not-beneficiary [{:keys [::provider ::beneficiary]}]
     (not= provider beneficiary))))

(s/def ::clauses (s/+ ::clause))

(s/def ::contract-template
  (s/and
   (s/keys :req [::id ::parties ::clauses])
   (fn providers-are-parties [{:keys [::parties ::clauses]}]
     (every? #(contains? parties %) (map ::provider clauses)))
   (fn beneficiaries-are-parties [{:keys [::parties ::clauses]}]
     (every? #(contains? parties %) (map ::beneficiary clauses)))))

(s/fdef generate-contract-template
  :ret ::contract-template)

(defn generate-contract-template []
  {::id (next-id)
   ::parties #{:purchaser :seller}
   ::clauses [{::provider :seller
               ::beneficiary :purchaser
               ::provider-products [{::item-id 3 ::quantity 3}]
               ::beneficiary-products [{::item-id 1 ::quantity 90}]
               ::opens-trigger {::trigger-type :signed
                                ::delay 3}
               ::closes-trigger {::trigger-type :signed
                                 ::delay 5}}]})
