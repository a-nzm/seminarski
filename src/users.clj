(ns users
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [cheshire.core :as json]
            [recommender :as recommender]))
(def profiles-file "user-profiles.json")

;;AI za idejnu bazu pitanja
(def questions
  [{:key :narrative      :text "Story and characters"}
   {:key :exploration    :text "Freedom, discovery, open areas"}
   {:key :strategy       :text "Planning, tactics, optimization"}
   {:key :action         :text "Fast combat and action"}
   {:key :challenge      :text "Difficulty and demanding gameplay"}
   {:key :social         :text "Co-op / playing with others"}
   {:key :creativity     :text "Building, crafting, creating"}
   {:key :relaxation     :text "Chill, cozy, low-stress vibe"}
   {:key :immersion      :text "Feeling absorbed in the world"}
   {:key :competition    :text "Ranked, PvP, competitive play"}
   {:key :horror         :text "Fear, tension, horror vibes"}
   {:key :puzzle-solving :text "Mystery, investigation, puzzles"}])

(def set-fields
  [:liked-game-ids :liked-genres :liked-tags :avoid-genres :avoid-tags])

(defn parse-int [s]
  (try
    (Integer/parseInt (str/trim s))
    (catch Exception _ nil)))

(defn parse-double [s]
  (try
    (Double/parseDouble (str/trim s))
    (catch Exception _ nil)))

(defn normalize-name [s]
  (-> (or s "")
      str/lower-case
      (str/replace #"[^a-z0-9\s]" " ")
      (str/replace #"\s+" " ")
      str/trim))

(defn normalize-slug [s]
  (-> (or s "")
      str/lower-case
      (str/replace #"-" " ")
      (str/replace #"[^a-z0-9\s]" " ")
      (str/replace #"\s+" " ")
      str/trim))


(defn game-search-text [game]
  (str (normalize-name (:name game))
       " "
       (normalize-name (:slug game))))

(defn split-comma [s]
  (->> (str/split (or s "") #",")
       (map str/trim)
       (remove str/blank?)
       vec))

(defn title-words [s]
  (->> (str/split (normalize-name s) #"\s+")
       (remove str/blank?)
       set))

;;AI koriscen za nalazenje Jaccard slicnosti koja se koristi za poredjenje preseka skupva reci
(defn title-similarity-score [a b]
  (let [wa (title-words a)
        wb (title-words b)
        all-words (set/union wa wb)]
    (if (empty? all-words)
      0.0
      (/ (double (count (set/intersection wa wb)))
         (double (count all-words))))))

;;AI radi organizcije toka pretrage, tačno - delimicno - jaccard
(defn find-best-game [games title]
  (let [query (normalize-name title)
        exact-match
        (first
          (filter (fn [game]
                    (or (= query (normalize-name (:name game)))
                        (= query (normalize-slug (:slug game)))))
                  games))
        contains-match
        (first
          (filter (fn [game]
                    (str/includes? (game-search-text game) query))
                  games))]
    (or exact-match
        contains-match
        (->> games
             (map (fn [game]
                    (assoc game :match
                                (title-similarity-score query
                                                        (game-search-text game)))))
             (filter #(>= (:match %) 0.25))
             (sort-by (juxt (comp - :match) :name))
             first))))


(defn resolve-liked-games [games titles]
  (->> titles
       (map (fn [title]
              (if-let [game (find-best-game games title)]
                (do
                  (println "Matched:" title "->" (:name game))
                  game)
                (do
                  (println "No match for:" title)
                  nil))))
       (remove nil?)
       (reduce (fn [acc game]
                 (if (some #(= (:id %) (:id game)) acc)
                   acc
                   (conj acc game)))
               [])))

(defn ask-score [label]
  (loop []
    (print label " [0-10, 5 = neutral]: ")
    (flush)
    (let [raw (str/trim (or (read-line) ""))
          n   (if (str/blank? raw) 5 (parse-int raw))]
      (if (and n (<= 0 n 10))
        n
        (do
          (println "Please enter a number from 0 to 10.")
          (recur))))))

(defn ask-preferences []
  (println "\n0 = strongly do not want")
  (println "5 = neutral")
  (println "10 = strongly want\n")
  (into {}
        (for [{:keys [key text]} questions]
          [key (ask-score text)])))



(defn ask-favorite-games [games]
  (println)
  (print "Type a few games you already like, separated by commas (or leave blank): ")
  (flush)
  (let [titles (split-comma (read-line))]
    (resolve-liked-games games titles)))

(defn ask-number-or-blank [prompt parser valid?]
  (loop []
    (print prompt)
    (flush)
    (let [raw (str/trim (or (read-line) ""))]
      (cond
        (empty? raw)
        (do
          (println "Skipped.")
          nil)

        :else
        (let [n (parser raw)]
          (if (and (some? n) (valid? n))
            n
            (do
              (println "Invalid input, try again or press Enter to skip.")
              (recur))))))))

(defn ask-yes-no [prompt]
  (loop []
    (print prompt " [y/n, blank = n]: ")
    (flush)
    (let [raw (str/lower-case (str/trim (or (read-line) "")))]
      (cond
        (str/blank? raw)
        false

        (#{"y" "yes"} raw)
        true

        (#{"n" "no"} raw)
        false

        :else
        (do
          (println "Please answer y or n.")
          (recur))))))

;; AI kao ideja za hard filtere
(defn ask-filters []
  (println "\nFilters. Leave blank to skip.")
  {:min-rating
   (ask-number-or-blank "Minimum RAWG rating [0.0 - 5.0]: "
                        parse-double #(<= 0.0 % 5.0))
   :min-metacritic
   (ask-number-or-blank "Minimum Metacritic [0 - 100]: "
                        parse-int #(<= 0 % 100))
   :release-year-from
   (ask-number-or-blank "Release year from [1975 - 2026]: "
                        parse-int #(<= 1975 % 2026))
   :exclude-early-access?
   (ask-yes-no "Exclude Early Access games?")})


(defn profile-id [name]
  (-> (or name "")
      str
      str/trim
      str/lower-case))
(defn profile-key [name]
  (keyword (profile-id name)))

(defn load-profiles []
  (if (.exists (io/file profiles-file))
    (json/parse-string (slurp profiles-file) true)
    {}))


(defn fix-loaded-profile [profile]
  (when profile
    (reduce (fn [p field]
              (update p field #(set (or % []))))
            profile
            set-fields)))
(defn load-profile [name]
  (fix-loaded-profile
    (get (load-profiles) (profile-key name))))


(defn save-profiles! [profiles]
  (with-open [w (io/writer profiles-file)]
    (.write w (json/generate-string profiles {:pretty true}))))

(defn save-profile! [profile]
  (let [profiles (load-profiles)
        id       (profile-key (:name profile))]
    (save-profiles! (assoc profiles id profile))
    profile))

(defn choose-profile [games]
  (print "Your name [blank = Player]: ")
  (flush)
  (let [name (let [s (str/trim (or (read-line) ""))]
               (if (str/blank? s) "Player" s))
        existing (load-profile name)]
    (if (and existing (ask-yes-no (str "Load saved profile for " name "?")))
      existing
      (let [answers (ask-preferences)
            liked   (ask-favorite-games games)
            filters (ask-filters)
            profile (-> (recommender/make-user-profile name answers liked)
                        (assoc :avoid-genres #{})
                        (assoc :avoid-tags #{})
                        (assoc :filters filters))]
        (save-profile! profile)))))

(defn search-games! [games title]
  (println "\nSearch result for:" title)

  (if-let [game (find-best-game games title)]
    (do
      (println "\nBest match:")
      (println (:name game)
               "| rating:" (:rating game)
               "| released:" (:released game)))
    (println "\nNo match found.")))