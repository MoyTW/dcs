(ns dcs.contract-template-test
  (:require [clojure.test :refer :all]
            [dcs.contract-template :as ct]
            [clojure.spec.alpha :as s]))



;; In 3-5 days, seller will provide buyer with 3 #3s for 90 dollars
(def simple-purchase-clause
  {::ct/provider :seller
   ::ct/beneficiary :buyer
   ::ct/provider-products [{::ct/item-id 3 ::ct/quantity 3}]
   ::ct/beneficiary-products [{::ct/item-id 1 ::ct/quantity 90}]
   ::ct/opens-trigger {::ct/trigger-type :signed
                       ::ct/delay 3}
   ::ct/closes-trigger {::ct/trigger-type :signed
                        ::ct/delay 5}})

;; In 0-2 days after I arrive in London, seller will provide associate with 90
;; #3s for 500 dollars
(def moderate-purchase-clause
  {::ct/provider :seller
   ::ct/beneficiary :associate
   ::ct/provider-products [{::ct/item-id 90 ::ct/quantity 3}]
   ::ct/beneficiary-products [{::ct/item-id 1 ::ct/quantity 500}]
   ::ct/opens-trigger {::ct/trigger-type :arrived
                    ::ct/target :client
                    ::ct/location :london}
   ::ct/closes-trigger {::ct/trigger-type :arrived
                     ::ct/target :client
                     ::ct/location :london
                     ::ct/delay 2}})

;; Starting when I arrive in London, until my brother leaves London or 30 days
;; have elapsed, allow my brother to buy 1 #48 item at a cost of $5
(def moderate-purchase-clause-2
  {::ct/provider :seller
   ::ct/beneficiary :brother
   ::ct/provider-products [{::ct/item-id 48 ::ct/quantity 1}]
   ::ct/beneficiary-products [{::ct/item-id 1 ::ct/quantity 5}]
   ::ct/opens-trigger {::ct/trigger-type :arrived
                    ::ct/target :client
                    ::ct/location :london}
   ::ct/closes-trigger [::ct/or
                     {::ct/trigger-type :arrived
                      ::ct/target :client
                      ::ct/location :london
                      ::ct/delay 30}
                     {::ct/trigger-type :left
                      ::ct/target :brother
                      ::ct/location :london}]})

;; Starting exactly in 1 year, bodyguard will begin guarding client for 2 years,
;; with 500 paid at that time (other payments will be in other clauses)
(def simple-bodyguard-clause
  {::ct/provider :bodyguard
   ::ct/beneficiary :client
   ::ct/provider-products [{::ct/service-id 39 ::ct/target :client ::ct/duration 730}]
   ::ct/beneficiary-products [{::ct/item-id 1 ::ct/quantity 300}]
   ::ct/opens-trigger {::ct/trigger-type :signed
                    ::ct/delay 365}
   ::ct/closes-trigger {::ct/trigger-type :signed
                     ::ct/delay 365}})

;; When I die, bodyguard will guard my daughter for 25 years, effective
;; *immediately* (like, within the hour, I guess? better hang around that
;; daughter if you take this one, or you could breach it real fast!)
(def moderate-bodyguard-clause
  {::ct/provider :bodyguard
   ::ct/beneficiary :client
   ::ct/provider-products [{::ct/service-id 39 ;; Obviously we need to work this out.
                         ::ct/target :daughter
                         ::ct/duration (* 25 365)}]
   ::ct/opens-trigger {:trigger-type :died :target :client}
   ::ct/closes-trigger {:trigger-type :died :target :client}})

(deftest test-conforms
  (testing "Various contracts conform"
    (doseq [clause [simple-purchase-clause
                    moderate-purchase-clause
                    moderate-purchase-clause-2
                    simple-bodyguard-clause
                    moderate-bodyguard-clause]]
      (is (nil? (s/explain-data ::ct/clause clause))))))
