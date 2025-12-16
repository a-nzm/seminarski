(ns funkcije_test
  (:require [midje.sweet :refer :all]
            [Domen :as sut]))



(facts "similarity"
       (sut/similarity {:preferences {:a 1 :b 2 :c 3}}
                       {:motivations {:a 1 :b 2 :c 3}})
       => 0

       (sut/similarity {:preferences {:a 1 :b 2 :c 3}}
                       {:motivations {:a 4 :b 0 :c 10}})
       => 12)
(facts "recommend"
       (let [user {:preferences {:a 1 :b 2}}
             g1   {:id :perfect :motivations {:a 1 :b 2}}
             g2   {:id :close   :motivations {:a 2 :b 2}}
             g3   {:id :mid     :motivations {:a 1 :b 5}}
             g4   {:id :far     :motivations {:a 10 :b 10}}
             g5   {:id :alsofar :motivations {:a 0 :b 20}}
             g6   {:id :worst   :motivations {:a 100 :b 100}}]

         (count (sut/recommend user [g1 g2 g3 g4 g5 g6])) => 5

         (map :id (sut/recommend user [g6 g3 g2 g5 g4 g1]))
         => [:perfect :close :mid :far :alsofar]))