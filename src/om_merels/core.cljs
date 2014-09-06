;; Merels

(ns om-merels.core
  (:require [om-merels.math :as math]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def width 400)
(def height 400)

(def centre-x (/ width 2))
(def centre-y (/ height 2))

(def piece-radius 15)

(def spoke-length 150)

(enable-console-print!)

(defn empty-piece [x y]
  {:centre-x x
   :centre-y y
   :piece :empty})

(def segment-angle (/ (* 2 math/pi) 8))

(defn offset-from-centre [{:keys [x y]}]
  {:x (+ centre-x x)
   :y (+ centre-y y)})

(def outer-piece-angles
  (map #(* segment-angle %) (range 8)))

(def outer-piece-positions
  (map #(offset-from-centre (math/circle-position % spoke-length)) outer-piece-angles))

(def outer-pieces
  (map (fn [{:keys [x y]}] (empty-piece x y)) outer-piece-positions))

(def game-state
  (atom
   {:turn :red
    :pieces (cons (empty-piece centre-x centre-y) outer-pieces)}))

(defn winner [[centre & spokes]]
  (let [winning-combos (map vector spokes (drop 4 spokes) (repeat centre))
        is-victory? (fn [positions]
                    (let [pieces-seen (into #{} (map :piece positions))]
                      (and (= (count pieces-seen) 1) (not (pieces-seen :empty)))))]
    (:piece (first (first (filter is-victory? winning-combos))))))

(winner (:pieces @game-state))

(def piece-fill {:empty "black"
                 :red "red"
                 :blue "blue"})

(defn position-view [{:keys [centre-x centre-y piece]} owner]
  (om/component
    (dom/circle #js {:cx centre-x
                     :cy centre-y
                     :r piece-radius
                     :stroke "black"
                     :strokeWidth 1
                     :fill (piece-fill piece)})))

(defn stroke-from-centre [{:keys [x y]}]
  (dom/line #js {:x1 centre-x :y1 centre-y :x2 x :y2 y :stroke "black"}))

(defn board-view [app owner]
  (om/component
    (apply dom/svg #js {:width width :height height}
      (dom/circle #js {:cx centre-x
                       :cy centre-y
                       :r spoke-length
                       :stroke "black"
                       :strokeWidth 1
                       :fill "white"})
      (concat (map stroke-from-centre outer-piece-positions)
              (om/build-all position-view (:pieces app))))))


(om/root board-view
         game-state
         {:target (.getElementById js/document "merels")})
