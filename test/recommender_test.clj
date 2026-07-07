(ns recommender-test
  (:require [midje.sweet :refer :all]
            [recommender :as r]
            [features :as f]))

(defn motivations [& kvs]
  (merge
    (zipmap f/motivation-keys (repeat 50.0))
    (apply hash-map kvs)))

(defn close-to [expected]
  (fn [actual]
    (< (Math/abs (- (double actual) (double expected))) 0.0001)))

;;AI koriscen za generisanje igara i korisnika
(def liked-game-1
  {:id 1
   :name "Fast Quest"
   :motivations (motivations
                  :action 90
                  :horror 10
                  :narrative 70
                  :strategy 40)
   :genres #{"action" "adventure"}
   :tags #{"combat" "story rich"}
   :rating 4.5
   :metacritic 85
   :ratings-count 900
   :released "2020-01-01"})

(def liked-game-2
  {:id 2
   :name "Story Blade"
   :motivations (motivations
                  :action 70
                  :horror 20
                  :narrative 90
                  :strategy 50)
   :genres #{"role-playing-games-rpg" "adventure"}
   :tags #{"story rich" "choices matter"}
   :rating 4.3
   :metacritic 82
   :ratings-count 700
   :released "2019-01-01"})

(def answers
  (merge
    (zipmap f/motivation-keys (repeat 5))
    {:action 8
     :horror 0
     :narrative 7
     :strategy 5}))

(def user
  (assoc
    (r/make-user-profile "Test User" answers [liked-game-1 liked-game-2])
    :filters {:min-rating 4.0
              :min-metacritic 80
              :release-year-from 2018
              :exclude-early-access? true}
    :avoid-genres #{}
    :avoid-tags #{}))

(def good-game
  {:id 10
   :name "Cyber Sword"
   :motivations (motivations
                  :action 82
                  :horror 8
                  :narrative 65
                  :strategy 40)
   :genres #{"action" "adventure"}
   :tags #{"combat" "open world"}
   :rating 4.4
   :metacritic 84
   :ratings-count 1200
   :released "2020-01-01"})

(def horror-game
  {:id 11
   :name "Haunted House"
   :motivations (motivations
                  :action 50
                  :horror 90
                  :narrative 60
                  :strategy 30)
   :genres #{"adventure"}
   :tags #{"horror" "survival horror"}
   :rating 4.2
   :metacritic 80
   :ratings-count 500
   :released "2021-06-10"})

(def low-rated-game
  {:id 12
   :name "Bad Game"
   :motivations (motivations
                  :action 90
                  :horror 0
                  :narrative 80
                  :strategy 40)
   :genres #{"action"}
   :tags #{"combat"}
   :rating 3.0
   :metacritic 70
   :ratings-count 100
   :released "2022-01-01"})

(facts "clamp and parse year"
       (r/clamp-score 150) => 100.0
       (r/clamp-score -20) => 0.0
       (r/clamp-score 45) => 45.0

       (r/parse-year "2020-01-01") => 2020
       (r/parse-year nil) => nil
       (r/parse-year "") => nil)

(facts "set overlap"
       (r/set-similarity #{"action" "adventure"}
                         #{"action" "rpg"})
       => (close-to 0.3333333333333333)

       (r/set-similarity #{} #{}) => 0.0

       (r/set-similarity #{"horror"}
                         #{"horror"})
       => 1.0)
;;AI koriscen za racunaje odgovora
(facts "0 to 10 - recommendation values"
       (r/answer->target 8) => 80.0
       (r/answer->target 5) => 50.0
       (r/answer->target 0) => 0.0

       (r/answer->importance 5) => 0.12
       (r/answer->importance 8) => 0.6
       (r/answer->importance 0) => 1.02

       (r/answer->tolerance 5) => 44.0
       (r/answer->tolerance 8) => 24.5
       (r/answer->tolerance 0) => 8.0)

(facts "validitz check"
       (r/valid-liked-game? liked-game-1) => true
       (r/valid-liked-game? {:name "Broken Game"}) => false

       (r/valid-liked-games [liked-game-1 {:name "Broken Game"}])
       => [liked-game-1])

(facts "motivation from liked games + labels"
       (:action (r/average-motivations [liked-game-1 liked-game-2])) => 80.0
       (:horror (r/average-motivations [liked-game-1 liked-game-2])) => 15.0
       (:narrative (r/average-motivations [liked-game-1 liked-game-2])) => 80.0

       (r/top-labels [liked-game-1 liked-game-2] :genres 6)
       => #{"adventure" "action" "role-playing-games-rpg"}

       (r/top-labels [liked-game-1 liked-game-2] :tags 12)
       => #{"story rich" "combat" "choices matter"})

(facts "build profile"
       (:name user) => "Test User"
       (:liked-games user) => ["Fast Quest" "Story Blade"]
       (:liked-game-ids user) => #{1 2}

       (:liked-genres user)
       => #{"adventure" "action" "role-playing-games-rpg"}

       (:liked-tags user)
       => #{"story rich" "combat" "choices matter"}

       (:action (:targets user)) => 80.0
       (:horror (:targets user)) => 5.25)

(facts "filters remove games before scoring"
       (r/passes-filters? user good-game) => true
       (r/passes-filters? user low-rated-game) => false

       (r/violates-avoid? (assoc user :avoid-tags #{"horror"})
                          horror-game)
       => true

       (r/allowed-game? user good-game) => true
       (r/allowed-game? user low-rated-game) => false

       (r/allowed-game? (assoc user :avoid-tags #{"horror"})
                        horror-game)
       => false)

(facts "motivation dimension can be rewarded or punished"
       (r/dimension-match-score 8 80.0 0.60 24.5 82)
       => 100.0

       (r/dimension-match-score 0 5.25 1.02 8.0 90)
       => 0.0)


(facts "recommendations also include explanations"
       (let [why (r/explain-game user good-game)]
         (keys why) => (contains :top-dimensions)
         (keys why) => (contains :weak-dimensions)
         (keys why) => (contains :tag-overlap)
         (keys why) => (contains :genre-overlap)))

(facts "recommend returns only valid games"
       (let [recs (r/recommend user
                               [good-game horror-game low-rated-game liked-game-1]
                               3)]
         (vec (map :name recs))
         => ["Cyber Sword" "Haunted House"]

         (boolean (some #(= "Bad Game" (:name %)) recs))
         => false

         (boolean (some #(= "Fast Quest" (:name %)) recs))
         => false))