(ns Domen)
(def games
  [{:id          1
    :name        "Witcher 3"
    :tags        ["story" "open-world" "fantasy"]
    :motivations {:narrative 85 :exploration 70 :strategy 40}}

   {:id          2
    :name        "Skyrim"
    :tags        ["open-world" "fantasy"]
    :motivations {:narrative 60 :exploration 90 :strategy 30}}

   {:id          3
    :name        "Disco Elysium"
    :tags        ["story" "rpg"]
    :motivations {:narrative 95 :exploration 40 :strategy 20}}

   {:id          4
    :name        "Civilization VI"
    :tags        ["strategy" "turn-based"]
    :motivations {:narrative 20 :exploration 40 :strategy 95}}

   {:id          5
    :name        "Elden Ring"
    :tags        ["open-world" "soulslike"]
    :motivations {:narrative 50 :exploration 85 :strategy 60}}

   {:id          6
    :name        "Stellaris"
    :tags        ["strategy" "space"]
    :motivations {:narrative 40 :exploration 70 :strategy 90}}

   {:id          7
    :name        "Red Dead Redemption 2"
    :tags        ["story" "open-world"]
    :motivations {:narrative 90 :exploration 80 :strategy 30}}

   {:id          8
    :name        "Hades"
    :tags        ["action" "roguelike"]
    :motivations {:narrative 60 :exploration 30 :strategy 70}}

   {:id          9
    :name        "Factorio"
    :tags        ["automation" "strategy"]
    :motivations {:narrative 10 :exploration 20 :strategy 95}}

   {:id          10
    :name        "Outer Wilds"
    :tags        ["exploration" "story"]
    :motivations {:narrative 80 :exploration 95 :strategy 20}}])

(def users [{:id          1
             :name        "Ana"
             :preferences {:narrative 90 :exploration 40 :strategy 20}}
            {:id          2
             :name        "Marko"
             :preferences {:narrative 30 :exploration 80 :strategy 50}}

            {:id          3
             :name        "Ivana"
             :preferences {:narrative 70 :exploration 60 :strategy 60}}

            {:id          4
             :name        "Nikola"
             :preferences {:narrative 20 :exploration 30 :strategy 90}}

            {:id          5
             :name        "Lena"
             :preferences {:narrative 85 :exploration 75 :strategy 25}}
            ])




(defn similarity [user game]
  (let [u (:preferences user)
        g (:motivations game)]
    (reduce +
            (map (fn [k]
                   (Math/abs (- (u k) (g k))))
                 (keys u)))))


(defn recommend [user games]
  (take 5
        (sort-by #(similarity user %) games)))


(comment
  (println
               (recommend (first users) games)
               ))

;slicnost mozda moze bolje od samo razlike ocena
;pozivace se mnog0 igrica po useru  mora to grupisanje bolje, k nearest? kako to ovde
;kako izvuci brojeve od 0 do 100 iz tagova i vajbova igrica ili korisnickog testa