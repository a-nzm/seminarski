(ns rawg
  (:require [cheshire.core :as json]
            [clojure.java.io :as io])
  (:import [java.net URLEncoder]))

(def api-root "https://api.rawg.io/api")
(def default-page-size 40)
(def max-detail-calls 9000)

;; AI koriscen za nalazenje razlicitih tipova imenovanja dlc i dodataka
(def dlc-name-pattern
  #"(?i)\b(dlc|expansion|add-?on|season pass|soundtrack|demo|beta|alpha|test server|starter pack|cosmetic pack)\b")

(def dlc-description-pattern
  #"(?i)(requires the base game|downloadable content|expansion for|add-?on content|season pass)")

(defn url-encode [value]
  (URLEncoder/encode (str value) "UTF-8"))

(defn read-json-from-url [url]
  (json/parse-string (slurp url) true))

(defn games-url [api-key page page-size ordering]
  (str api-root
       "/games?key=" (url-encode api-key)
       "&page=" page
       "&page_size=" page-size
       "&ordering=" (url-encode ordering)))

(defn game-details-url [api-key game-id]
  (str api-root "/games/" game-id "?key=" (url-encode api-key)))

(defn get-games-page [api-key page page-size ordering]
  (read-json-from-url (games-url api-key page page-size ordering)))

(defn get-game-details [api-key game-id]
  (read-json-from-url (game-details-url api-key game-id)))

(defn get-games-from-pages [api-key pages page-size ordering]
  (->> (range 1 (inc pages))
       (map #(get-games-page api-key % page-size ordering))
       (mapcat :results)
       vec))

(defn base-game? [game]
  (let [name (or (:name game) "")
        description (str (or (:description_raw game)
                             (:description game)
                             ""))]
    (and (not (re-find dlc-name-pattern name))
         (not (re-find dlc-description-pattern description)))))

;; AI koriscen za bolju strukturu i nosenje sa mogucim null vrednostima
(defn add-details [api-key game-summary]
  (try
    (let [details (get-game-details api-key (:id game-summary))
          full-game (merge game-summary details)]
      (when (base-game? full-game)
        full-game))
    (catch Exception e
      (println "Skipping" (:name game-summary) "-" (.getMessage e))
      nil)))

(defn unique-by-id [games]
  (->> games
       (reduce (fn [acc game]
                 (assoc acc (:id game) game))
               {})
       vals
       vec))

;;AI koriscen za ideju preuzimanja igrica, ne samo najpopularnijih vec i top rated i vremena dodavanja
;;AI korsicen za max detail calls jer se prekoracenje kredita desilo zbog lose racunice stranica
(defn download-games
  ([api-key pages]
   (download-games api-key pages default-page-size))
  ([api-key pages page-size]
   (let [top-rated  (get-games-from-pages api-key pages page-size "-rating")
         popular    (get-games-from-pages api-key pages page-size "-added")
         recent     (get-games-from-pages api-key (max 5 (quot pages 2)) page-size "-released")
         alphabetic (get-games-from-pages api-key (max 4 (quot pages 3)) page-size "name")
         all-games  (unique-by-id (concat top-rated popular recent alphabetic))]
     (println "Fetched" (count all-games) "unique games. Loading details...")
     (->> all-games
          (take max-detail-calls)
          (keep #(add-details api-key %))
          vec))))

(defn save-json! [path data]
  (let [file (io/file path)]
    (with-open [writer (io/writer file)]
      (.write writer (json/generate-string data {:pretty true})))
    (println "Saved JSON to:" (.getAbsolutePath file))))

(defn load-json [path]
  (json/parse-string (slurp path) true))