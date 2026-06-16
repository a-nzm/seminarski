(ns features
  (:require [clojure.string :as str]))

(def motivation-keys
  [:narrative
   :exploration
   :strategy
   :action
   :challenge
   :social
   :creativity
   :relaxation
   :immersion
   :competition
   :horror
   :puzzle-solving])

(def genre->weights
  {"role-playing-games-rpg" {:narrative 23 :exploration 19 :strategy 11 :immersion 17}
   "adventure"              {:narrative 14 :exploration 27 :immersion 13 :puzzle-solving 9}
   "strategy"               {:strategy 31 :puzzle-solving 12 :challenge 8}
   "action"                 {:action 29 :challenge 11 :immersion 6}
   "shooter"                {:action 27 :competition 10 :challenge 9}
   "simulation"             {:strategy 13 :creativity 24 :relaxation 16 :immersion 11}
   "puzzle"                 {:puzzle-solving 34 :strategy 12 :challenge 9}
   "sports"                 {:action 16 :social 12 :competition 28}
   "platformer"             {:action 17 :challenge 14 :puzzle-solving 7}
   "indie"                  {:creativity 11 :immersion 8}
   "casual"                 {:relaxation 26 :social 7}
   "massively-multiplayer"  {:social 28 :exploration 9 :competition 12 :immersion 10}
   "fighting"               {:action 24 :competition 21 :challenge 13}
   "family"                 {:social 14 :relaxation 16}
   "roguelike"              {:challenge 26 :strategy 13 :action 9}
   "racing"                 {:action 21 :competition 18}
   "arcade"                 {:action 22 :challenge 10 :relaxation 4}})

(def tag->weights
  {"choices matter"       {:narrative 21 :strategy 13 :immersion 9}
   "great dialogue"       {:narrative 24 :immersion 8}
   "multiple endings"     {:narrative 17 :strategy 11}
   "open world"           {:exploration 33 :immersion 14 :relaxation 5}
   "exploration"          {:exploration 29 :immersion 8}
   "sandbox"              {:exploration 22 :creativity 19 :immersion 9 :relaxation 14}
   "atmospheric"          {:exploration 13 :relaxation 17 :immersion 21}
   "turn-based"           {:strategy 18 :puzzle-solving 9}
   "turn-based tactics"   {:strategy 27 :challenge 12}
   "management"           {:strategy 28 :creativity 8}
   "resource management"  {:strategy 31 :challenge 11}
   "base building"        {:strategy 16 :creativity 24 :immersion 7}
   "action-adventure"     {:action 19 :exploration 13 :immersion 7}
   "fast-paced"           {:action 23 :challenge 9}
   "hack and slash"       {:action 22 :challenge 10}
   "difficult"            {:challenge 32 :competition 11}
   "souls-like"           {:challenge 27 :action 14 :immersion 9 :relaxation -20 }
   "multiplayer"          {:social 16 :competition 5}
   "co-op"                {:social 19 :immersion 5 :relaxation 4}
   "team-based"           {:social 11 :competition 7 :action 4}
   "building"             {:creativity 23 :strategy 8 :relaxation 11}
   "competitive"          {:competition 9 :challenge 8 :social 5 :action 3}
   "crafting"             {:creativity 21 :strategy 13 :immersion 6 :relaxation 8}
   "moddable"             {:creativity 18 :immersion 7}
   "relaxing"             {:relaxation 34 :immersion 10}
   "cozy"                 {:relaxation 31 :social 7}
   "funny"                {:relaxation 11 :social 8}
   "mystery"              {:narrative 14 :puzzle-solving 16 :immersion 12}
   "survival"             {:challenge 18 :strategy 14 :horror 9}
   "immersive sim"        {:narrative 16 :strategy 18 :exploration 14 :immersion 27}
   "horror"               {:horror 26 :immersion 9 :challenge 6}
   "psychological horror" {:horror 31 :immersion 11}
   "survival horror"      {:horror 30 :challenge 10 :immersion 7}
   "stealth"              {:strategy 15 :challenge 11 :immersion 8}
   "investigation"        {:narrative 14 :puzzle-solving 22 :immersion 12}
   "detective"            {:narrative 17 :puzzle-solving 25}
   "combat"               {:action 22 :challenge 8}
   "pvp"                  {:competition 7 :social 3 :challenge 4}
   "fps"                  {:action 18 :competition 6}
   "first-person"         {:action 10 :immersion 8}
   "class-based"          {:action 9 :strategy 10 :social 6}
   "tactical"             {:strategy 11 :action 6}
   "free to play"         {}
   "online co-op"         {:social 18 :action 5 :relaxation 4}
   "multiplayer co-op"    {:social 18 :action 4 :relaxation 4}
   "cooperative"          {:social 14 :action 4 :relaxation 4}
   "modding"              {:creativity 18 :immersion 8 :relaxation 4}
   "mod"                  {:creativity 12 :immersion 5}
   "procedural-generation" {:exploration 8 :creativity 6 :relaxation 3}
   "lore-rich"            {:narrative 16 :immersion 10}
   "third-person"         {:action 4 :immersion 5}
   "third person"         {:action 4 :immersion 5}
   "action rpg"           {:action 12 :narrative 7 :immersion 5}
   "narrative"            {:narrative 18 :immersion 8}
   "story rich"           {:narrative 37 :immersion 18}
   "singleplayer"         {:competition -8 :social -5 :narrative 6 :immersion 8}})

(def description-rules
  [["story"         {:narrative 9 :immersion 4}]
   ["narrative"     {:narrative 11 :immersion 5}]
   ["dialogue"      {:narrative 8 :immersion 4}]
   ["character"     {:narrative 7 :immersion 5}]
   ["choice"        {:narrative 8 :strategy 6}]
   ["branching"     {:narrative 7 :strategy 5}]
   ["open world"    {:exploration 14 :immersion 6}]
   ["explore"       {:exploration 9 :immersion 4}]
   ["discovery"     {:exploration 8 :immersion 4}]
   ["craft"         {:creativity 9 :strategy 6}]
   ["build"         {:creativity 9 :strategy 5}]
   ["manage"        {:strategy 9 :challenge 4}]
   ["strategy"      {:strategy 11 :puzzle-solving 4}]
   ["tactics"       {:strategy 12 :challenge 5}]
   ["turn-based"    {:strategy 10 :puzzle-solving 5}]
   ["combat"        {:action 8 :challenge 4}]
   ["fast-paced"    {:action 11 :challenge 5}]
   ["boss"          {:action 6 :challenge 8}]
   ["difficult"     {:challenge 13}]
   ["challenging"   {:challenge 11}]
   ["multiplayer"   {:social 12 :competition 6}]
   ["co-op"         {:social 13 :immersion 3}]
   ["competitive"   {:competition 12 :social 5}]
   ["pvp"           {:competition 15 :challenge 4}]
   ["immersive"     {:immersion 14}]
   ["atmospheric"   {:immersion 11 :relaxation 5}]
   ["cinematic"     {:immersion 9 :narrative 4}]
   ["horror"        {:horror 10 :immersion 4}]
   ["terror"        {:horror 7}]
   ["fear"          {:horror 6}]
   ["haunted"       {:horror 8 :immersion 3}]
   ["mystery"       {:puzzle-solving 8 :narrative 5}]
   ["detective"     {:puzzle-solving 13 :narrative 6}]
   ["investigate"   {:puzzle-solving 10 :narrative 4}]
   ["puzzle"        {:puzzle-solving 14}]
   ["riddle"        {:puzzle-solving 11}]
   ["logic"         {:puzzle-solving 9 :strategy 4}]
   ["relaxing"      {:relaxation 14}]
   ["cozy"          {:relaxation 12 :social 4}]
   ["party-based"   {:social 7 :strategy 8}]
   ["party based"   {:social 7 :strategy 8}]
   ["choices matter" {:narrative 9 :strategy 5}]
   ["single-player" {:competition -14 :social -8 :narrative 4 :immersion 5}]
   ["single player" {:competition -14 :social -8 :narrative 4 :immersion 5}]
   ["story-driven"  {:competition -5 :narrative 10 :immersion 6}]
   ["story driven"  {:competition -5 :narrative 10 :immersion 6}]
   ["lore-rich"     {:narrative 10 :immersion 7}]
   ["lore rich" {:narrative 10 :immersion 7}]
   ["sandbox"       {:creativity 7 :relaxation 8 :exploration 5}]])

(defn empty-scores []
  (zipmap motivation-keys (repeat 0)))

(defn add-scores [a b]
  (merge-with + a b))

(defn clamp-score [x]
  (-> x double Math/round int (max 0) (min 100)))

;dodato zbog funkcije za rekalkulisanje jsona
(defn genre-value [genre]
  (cond
    (string? genre) genre
    (map? genre) (:slug genre)
    :else nil))
(defn tag-value [tag]
  (cond
    (string? tag) tag
    (map? tag) (:name tag)
    :else nil))
(defn get-genres [game]
  (->> (:genres game)
       (map genre-value)
       (remove nil?)
       (map str/lower-case)
       set))
(defn get-tags [game]
  (->> (:tags game)
       (map tag-value)
       (remove nil?)
       (map str/lower-case)
       set))

(defn get-description [game]
  (or (:description_raw game)
      (:description game)
      ""))


(defn clean-description [text]
  (-> (or text "")
      (str/replace #"<[^>]+>" " ")
      (str/replace "&quot;" "\"")
      (str/replace "&amp;" "&")
      (str/replace "&nbsp;" " ")
      (str/replace #"[-–—]" " ")
      (str/replace #"\s+" " ")
      str/trim))

(defn score-labels [labels weights-map]
  (reduce
    (fn [acc label]
      (add-scores acc (get weights-map label {})))
    (empty-scores)
    labels))

(defn score-description [description-text]
  (reduce
    (fn [acc [phrase weights]]
      (if (str/includes? description-text phrase)
        (add-scores acc weights)
        acc))
    (empty-scores)
    description-rules))

(defn playtime-bonus [hours]
  (cond
    (>= hours 80) {:exploration 11 :immersion 13 :challenge 9}
    (>= hours 35) {:exploration 7 :immersion 9 :strategy 4}
    (>= hours 15) {:exploration 4 :immersion 5}
    :else {}))

(defn build-motivations [game]
  (let [genres (get-genres game)
        tags (get-tags game)
        description (-> game get-description clean-description str/lower-case)
        genre-score (score-labels genres genre->weights)
        tag-score (score-labels tags tag->weights)
        description-score (score-description description)
        playtime-score (playtime-bonus (or (:playtime game) 0))]
    (->> [genre-score tag-score description-score playtime-score]
         (reduce add-scores)
         (map (fn [[k v]] [k (clamp-score v)]))
         (into {}))))

(defn normalize-game [game]
  {:id            (:id game)
   :name          (:name game)
   :slug          (:slug game)
   :genres        (get-genres game)
   :tags          (get-tags game)
   :rating        (double (or (:rating game) 0.0))
   :metacritic    (or (:metacritic game) 0)
   :ratings-count (or (:ratings_count game) 0)
   :added         (or (:added game) 0)
   :playtime      (or (:playtime game) 0)
   :released      (:released game)
   :description   (clean-description (get-description game))
   :motivations   (build-motivations game)})