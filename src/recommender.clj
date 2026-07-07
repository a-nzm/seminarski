(ns recommender
  (:require [clojure.set :as set]
            [features :as features]))

(def default-target 50.0)

(defn clamp-score [x]
  (-> x double (max 0.0) (min 100.0)))

(defn parse-year [released]
  (try
    (Integer/parseInt (subs (or released "") 0 4))
    (catch Exception _ nil)))


(defn overlaps? [a b]
  (not (empty? (set/intersection (set (or a #{}))
                                 (set (or b #{}))))))

;; AI korišćen za pronalazenje i implementaciju JACCARD slicnosti kao efikasnog algoritma
(defn set-similarity [a b]
  (let [a (set (or a #{}))
        b (set (or b #{}))
        all-items (set/union a b)]
    (if (empty? all-items)
      0.0
      (/ (double (count (set/intersection a b)))
         (double (count all-items))))))

(defn valid-liked-game? [game]
  (and (map? game)
       (number? (:id game))
       (string? (:name game))
       (map? (:motivations game))))

(defn valid-liked-games [games]
  (vec (filter valid-liked-game? games)))

(defn  average-motivations [games]
  (let [games (valid-liked-games games)]
    (if (empty? games)
      (zipmap features/motivation-keys (repeat default-target))
      (let [n (double (count games))]
        (into {}
              (for [k features/motivation-keys]
                [k (/ (reduce + (map #(double (get-in % [:motivations k] 0)) games))
                      n)]))))))

(defn top-labels [games field n]
  (->> games
       (mapcat #(or (field %) #{}))
       frequencies
       (sort-by (fn [[label count]] [(- count) label]))
       (take n)
       (map first)
       set))

(defn answer->target [answer]
  (* 10.0 (double answer)))

;;AI koriscen kroz iteracije kako bi se usvrsile slobodne numericke vrednosti
(defn answer->importance [answer]
  (let [a (double answer)
        e (Math/abs (- a 5.0))
        base (+ 0.12 (* 0.16 e))]
    (if (< a 5.0)
      (+ base 0.10)
      base)))

;;AI koriscen kroz iteracije kako bi se usvrsile slobodne numericke vrednosti
(defn answer->tolerance [answer]
  (let [a (double answer)
        e (Math/abs (- a 5.0))
        base (max 10.0 (- 44.0 (* 6.5 e)))]
    (if (< a 5.0)
      (max 8.0 (- base 4.0))
      base)))

(defn answers->targets [answers]
  (into {}
        (for [k features/motivation-keys]
          [k (answer->target (get answers k 5))])))

(defn answers->importance-map [answers]
  (into {}
        (for [k features/motivation-keys]
          [k (answer->importance (get answers k 5))])))

(defn answers->tolerance-map [answers]
  (into {}
        (for [k features/motivation-keys]
          [k (answer->tolerance (get answers k 5))])))

(defn blend-targets [manual-targets derived-targets]
  (into {}
        (for [k features/motivation-keys]
          [k (+ (* 0.65 (double (get manual-targets k 50.0)))
                (* 0.35 (double (get derived-targets k 50.0))))])))

(defn distance-from-range [value low high]
  (cond
    (< value low) (- low value)
    (> value high) (- value high)
    :else 0.0))

;;AI koriscen za procenu tolerancije od tačne ciljane vrednosti
;;Vrednosti za značaj razlicitih odgovora, kalkulacije u razlicitim slucajevima
(defn dimension-match-score [answer target importance tolerance game-value]
  (let [gv      (double game-value)
        low     (max 0.0 (- target tolerance))
        high    (min 100.0 (+ target tolerance))
        dist    (distance-from-range gv low high)
        a       (double answer)
        directional-mult
        (cond
          (and (< a 5.0) (> gv high)) (+ 1.45 (* 0.10 (- 5.0 a)))
          (and (> a 5.0) (< gv low))  (+ 1.15 (* 0.06 (- a 5.0)))
          :else 1.0)
        penalty (* 1.35 dist importance directional-mult)]
    (clamp-score (- 100.0 penalty))))

(defn dimension-rows [user game]
  (for [k features/motivation-keys
        :let [answer     (double (get-in user [:answers k] 5.0))
              target     (double (get-in user [:targets k] 50.0))
              importance (double (get-in user [:importance k] 0.12))
              tolerance  (double (get-in user [:tolerance k] 44.0))
              game-value (double (get-in game [:motivations k] 0.0))
              match      (dimension-match-score answer target importance tolerance game-value)
              weight     (+ 0.18 importance)]]
    {:key k
     :answer answer
     :target target
     :importance importance
     :tolerance tolerance
     :game-value game-value
     :match match
     :weight weight}))

(defn motivation-match-score [user game]
  (let [rows (dimension-rows user game)
        weighted-sum (reduce + (map #(* (:match %) (:weight %)) rows))
        total-weight (reduce + (map :weight rows))]
    (if (zero? total-weight)
      0.0
      (/ weighted-sum total-weight))))

(defn quality-score [game]
  ( clamp-score
    (+ (* 12.0 (double (or (:rating game) 0.0)))
       (* 0.35 (double (or (:metacritic game) 0.0)))
       (* 0.01 (min 1500.0 (double (or (:ratings-count game) 0.0)))))))


(defn make-user-profile [name answers liked-games]
  (let [liked-games     (valid-liked-games liked-games)
        manual-targets  (answers->targets answers)
        derived-targets (average-motivations liked-games)
        final-targets   (if (seq liked-games)
                          (blend-targets manual-targets derived-targets)
                          manual-targets)]
    {:name           name
     :answers        answers
     :targets        final-targets
     :importance     (answers->importance-map answers)
     :tolerance      (answers->tolerance-map answers)
     :liked-games    (mapv :name liked-games)
     :liked-game-ids (set (map :id liked-games))
     :liked-genres   (top-labels liked-games :genres 6)
     :liked-tags     (top-labels liked-games :tags 12)}))

(defn passes-filters? [user game]
  (let [{:keys [min-rating
                min-metacritic
                release-year-from
                exclude-early-access?]} (:filters user)
        rating     (double (or (:rating game) 0.0))
        metacritic (double (or (:metacritic game) 0.0))
        year       (parse-year (:released game))
        tags       (set (or (:tags game) #{}))]
    (and
      (or (nil? min-rating) (>= rating (double min-rating)))
      (or (nil? min-metacritic) (>= metacritic (double min-metacritic)))
      (or (nil? release-year-from)
          (and year (>= year (int release-year-from))))
      (or (not exclude-early-access?)
          (not (contains? tags "early access"))))))


(defn violates-avoid? [user game]
  (or (overlaps? (:avoid-genres user) (:genres game))
      (overlaps? (:avoid-tags user) (:tags game))))

(defn allowed-game? [user game]
  (and (passes-filters? user game)
       (not (violates-avoid? user game))))

;;AI koriscen za dolazenje do balansa izmedju faktora koji se porede
(defn score-game [user game]
  (let [motivation-score (motivation-match-score user game)
        genre-score      (* 100.0 (set-similarity (:liked-genres user) (:genres game)))
        tag-score        (* 100.0 (set-similarity (:liked-tags user) (:tags game)))
        q-score          (quality-score game)]
    (+ (* 0.82 motivation-score)
       (* 0.06 genre-score)
       (* 0.04 tag-score)
       (* 0.08 q-score))))


(defn format-dimension-row [{:keys [key game-value target]}]
  (str (name key)
       "="
       (int (Math/round game-value))
       " vs target "
       (int (Math/round target))))

;;AI koriscen za format objasnjenja igre
(defn explain-game [user game]
  (let [rows (dimension-rows user game)
        best-dimensions (->> rows
                             (sort-by (juxt (comp - :match) (comp - :importance)))
                             (take 3)
                             (map format-dimension-row))
        weak-dimensions (->> rows
                             (sort-by (juxt :match (comp - :importance)))
                             (take 2)
                             (map format-dimension-row))
        overlapping-tags (->> (set/intersection (set (or (:liked-tags user) #{}))
                                                (set (or (:tags game) #{})))
                              (take 3))
        overlapping-genres (->> (set/intersection (set (or (:liked-genres user) #{}))
                                                  (set (or (:genres game) #{})))
                                (take 2))]
    {:top-dimensions best-dimensions
     :weak-dimensions weak-dimensions
     :tag-overlap overlapping-tags
     :genre-overlap overlapping-genres}))

(defn recommend
  ([user games]
   (recommend user games 10))
  ([user games n]
   (let [seen-ids (set (or (:liked-game-ids user) #{}))]
     (->> games
          (filter #(allowed-game? user %))
          (remove #(contains? seen-ids (:id %)))
          (map (fn [game]
                 (let [score (score-game user game)
                       why   (explain-game user game)]
                   (assoc game :score score :why why))))
          (sort-by (juxt (comp - :score) :id))
          (take n)))))