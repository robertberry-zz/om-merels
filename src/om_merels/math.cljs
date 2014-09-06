(ns om-merels.math)

(defn sin [a] (.sin js/Math a))
(defn cos [a] (.cos js/Math a))
(def pi (aget js/Math "PI"))

(defn circle-position [angle radius]
  {:x (* radius (cos angle))
   :y (* radius (sin angle))})
