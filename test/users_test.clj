(ns users-test
  (:require [midje.sweet :refer :all]
            [users :as users]))

(def games
  [{:id 1
    :name "The Witcher 3: Wild Hunt"
    :slug "the-witcher-3-wild-hunt"}

   {:id 2
    :name "Minecraft"
    :slug "minecraft"}

   {:id 3
    :name "The Elder Scrolls V: Skyrim"
    :slug "the-elder-scrolls-v-skyrim"}])

(facts "comma separated input is turned into game titles"
       (users/split-comma "Witcher 3, Minecraft, Skyrim")
       => ["Witcher 3" "Minecraft" "Skyrim"]

       (users/split-comma "")
       => [])

(facts "game names normalized"
       (users/normalize-name "The Witcher 3: Wild Hunt!")
       => "the witcher 3 wild hunt"

       (users/normalize-slug "the-witcher-3-wild-hunt")
       => "the witcher 3 wild hunt")

(facts "find-best-game finds games"
       (:name (users/find-best-game games "minecraft"))
       => "Minecraft"

       (:name (users/find-best-game games "witcher"))
       => "The Witcher 3: Wild Hunt"

       (:name (users/find-best-game games "skyrim"))
       => "The Elder Scrolls V: Skyrim")

(facts "title-similarity gives higher score to closer titles"
       (users/title-similarity-score "The Witcher 3"
                                     "The Witcher 3: Wild Hunt")
       => #(> % 0.0)

       (users/title-similarity-score "Minecraft"
                                     "Minecraft")
       => 1.0)