(ns features-test
  (:require [midje.sweet :refer :all]
            [features :as features]))

(facts "description text clean"
       (features/clean-description "<p>Story-rich &amp; open world</p>")
       => "Story rich & open world"

       (features/clean-description nil)
       => "")

(facts "genres as a set"
       (features/get-genres {:genres ["action" "adventure" "action"]})
       => #{"action" "adventure"})

(facts "tags are extracted and lowercased"
       (features/get-tags {:tags ["Story Rich" "Open World" "STORY RICH"]})
       => #{"story rich" "open world"})

(facts "empty score map with keys"
       (set (keys (features/empty-scores)))
       => (set features/motivation-keys)

       (every? zero? (vals (features/empty-scores)))
       => true)

(facts "score between 0 and 100"
       (features/clamp-score 150) => 100
       (features/clamp-score -20) => 0
       (features/clamp-score 65.7) => 66)