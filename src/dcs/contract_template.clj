(ns dcs.contract-template
  (:require [clojure.spec.alpha :as s]))

(defonce last-id (atom 0))

(defn- next-id []
  (swap! last-id inc))

(s/def ::id int?)


;; *****************************************************************************
;; * CLAUSE EXAMPLES *
;; *****************************************************************************
;;
;; Test with test-examples

;; In 3-5 days, seller will provide buyer with 3 #3s for 90 dollars
(def simple-purchase-clause
  {::provider :seller
   ::beneficiary :buyer
   ::provider-products [{::item-id 3 ::quantity 3}]
   ::beneficiary-products [{::item-id 1 ::quantity 90}]
   ::opens-trigger {::trigger-type :signed
                    ::delay 3}
   ::closes-trigger {::trigger-type :signed
                     ::delay 5}})

;; In 0-2 days after I arrive in London, seller will provide associate with 90
;; #3s for 500 dollars
(def moderate-purchase-clause
  {::provider :seller
   ::beneficiary :associate
   ::provider-products [{::item-id 90 ::quantity 3}]
   ::beneficiary-products [{::item-id 1 ::quantity 500}]
   ::opens-trigger {::trigger-type :arrived
                    ::target :client
                    ::location :london}
   ::closes-trigger {::trigger-type :arrived
                     ::target :client
                     ::location :london
                     ::delay 2}})

;; Starting when I arrive in London, until my brother leaves London or 30 days
;; have elapsed, allow my brother to buy 1 #48 item at a cost of $5
(def moderate-purchase-clause-2
  {::provider :seller
   ::beneficiary :brother
   ::provider-products [{::item-id 48 ::quantity 1}]
   ::beneficiary-products [{::item-id 1 ::quantity 5}]
   ::opens-trigger {::trigger-type :arrived
                    ::target :client
                    ::location :london}
   ::closes-trigger [::or
                     {::trigger-type :arrived
                      ::target :client
                      ::location :london
                      ::delay 30}
                     {::trigger-type :left
                      ::target :brother
                      ::location :london}]})

;; Starting exactly in 1 year, bodyguard will begin guarding client for 2 years,
;; with 500 paid at that time (other payments will be in other clauses)
(def simple-bodyguard-clause
  {::provider :bodyguard
   ::beneficiary :client
   ::provider-products [{::service-id 39 ::target :client ::duration 730}]
   ::beneficiary-products [{::item-id 1 ::quantity 300}]
   ::opens-trigger {::trigger-type :signed
                    ::delay 365}
   ::closes-trigger {::trigger-type :signed
                     ::delay 365}})

;; When I die, bodyguard will guard my daughter for 25 years, effective
;; *immediately* (like, within the hour, I guess? better hang around that
;; daughter if you take this one, or you could breach it real fast!)
(def moderate-bodyguard-clause
  {::provider :bodyguard
   ::beneficiary :client
   ::provider-products [{::service-id 39 ;; Obviously we need to work this out.
                         ::target :daughter
                         ::duration (* 25 365)}]
   ::opens-trigger {:trigger-type :died :target :client}
   ::closes-trigger {:trigger-type :died :target :client}})

(defn- test-examples []
  (clojure.pprint/pprint
   (map (partial s/explain-data ::clause)
        [simple-purchase-clause
         moderate-purchase-clause
         moderate-purchase-clause-2
         simple-bodyguard-clause
         moderate-bodyguard-clause])))

;; *****************************************************************************
;; * CLAUSE IMPLEMENTATION *
;; *****************************************************************************

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
