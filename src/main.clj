(ns main
  (:require [clojure.java.io :as io]
            [rawg :as rawg]
            [users :as users]
            [features :as features]
            [recommender :as recommender]))

(def rawg-api-key
  (or (System/getenv "RAWG_API_KEY")
      "ff65b9540b4e4571ab3e32559a631cb7"))

(def games-file "rawg-games.json")
(def default-pages 30)

(defn download-games!
  ([] (download-games! default-pages))
  ([pages]
   (let [raw-games (rawg/download-games rawg-api-key pages)
         games     (mapv features/normalize-game raw-games)]
     (rawg/save-json! games-file games)
     (println "Saved" (count games) "games"))))

(defn load-or-download-games! []
  (if (.exists (io/file games-file))
    (do
      (println "Loading games from local JSON...")
      (rawg/load-json games-file))
    (do
      (println "Local JSON not found. Downloading from RAWG API...")
      (download-games!))))

(defn show-recommendations!
  ([] (show-recommendations! 10))
  ([n]
   (let [games (load-or-download-games!)
         user  (users/choose-profile games)
         recs  (recommender/recommend user games n)]
     (println)
     (if (empty? recs)
       (println "No recommendations found. Relax some filters.")
       (do
         (println "Top recommendations:\n")
         (doseq [g recs]
           (println (:name g) "| score =" (format "%.2f" (:score g)))
           (println "motivations:" (:motivations g))
           (println "genres:" (:genres g))
           (println "tags:" (:tags g))
           (println "released:" (:released g)
                    "| rating:" (:rating g)
                    "| metacritic:" (:metacritic g))
           (println "why:" (:why g))
           (println)))))))

;;AI koriscen kako bi se rešio problem zastarelog json kroz različite iteracije koda.
;;Prommena težina i načina računjana vrednosti dovodi do neophodnosti za novim veliki API pozivom
;;ograničenost kreditia zahteva korekcije na već postojecem fajlu
(defn recalculate-game [game]
  (assoc game
    :genres (features/get-genres game)
    :tags (features/get-tags game)
    :description (features/clean-description
                   (features/get-description game))
    :motivations (features/build-motivations game)))

(defn recalculate-games-json! []
  (let [games (rawg/load-json games-file)
        recalculated-games (mapv recalculate-game games)]
    (rawg/save-json! games-file recalculated-games)
    (println "Recalculated games:" (count recalculated-games))
    {:recalculated-games (count recalculated-games)
     :file games-file}))

(defn -main [& _]
  (show-recommendations!))