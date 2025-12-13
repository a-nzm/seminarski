(ns Domen)
{:id 1
 :name "Witcher 3"
 :tags ["story" "open-world" "fantasy"]
 :motivations {:narrative 85 :exploration 70 :strategy 40}}

{:id 1
 :name "Ana"
 :preferences {:narrative 90 :exploration 40 :strategy 20}}

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

;slicnost mozda moze bolje od samo razlike ocena
;pozivace se mnog0 igrica po useru  mora to grupisanje bolje, k nearest? kako to ovde
;kako izvuci brojeve od 0 do 100 iz tagova i vajbova igrica ili korisnickog testa