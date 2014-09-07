;; Merels

(ns om-merels.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om-merels.math :as math]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(def width 400)
(def height 450)

(def centre-x (/ width 2))
(def centre-y (/ height 2))

(def piece-radius 15)
(def piece-diameter (* piece-radius 2))

(def spoke-length 150)

(enable-console-print!)

(defn empty-piece [x y]
  {:centre-x x
   :centre-y y
   :piece :empty
   :mouse-over? nil
   :selected? nil})

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
    :players {:red {:remaining 3}
              :blue {:remaining 3}}
    :selected nil
    :pieces (cons (empty-piece centre-x centre-y) outer-pieces)}))

(defn winner [[centre & spokes]]
  (let [winning-combos (map vector spokes (drop 4 spokes) (repeat centre))
        is-victory? (fn [positions]
                    (let [pieces-seen (into #{} (map :piece positions))]
                      (and (= (count pieces-seen) 1) (not (pieces-seen :empty)))))]
    (:piece (first (first (filter is-victory? winning-combos))))))

(def piece-fill {:empty "black"
                 :red "red"
                 :blue "blue"})

(def other-piece {:red :blue :blue :red})

(defn position-view [{:keys [centre-x centre-y piece mouse-over? selected?] :as state} owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [clicks]}]
      (dom/circle #js {:cx centre-x
                       :cy centre-y
                       :r piece-radius
                       :stroke (if mouse-over? "red" "black")
                       :strokeWidth 2
                       :fill (piece-fill piece)
                       :style #js {:cursor "pointer"}
                       :onClick (fn [_] (put! clicks state))}))))

(defn stroke-from-centre [{:keys [x y]}]
  (dom/line #js {:x1 centre-x :y1 centre-y :x2 x :y2 y :stroke "black"}))

(defn transforming-piece [piece f]
  #(map (fn [p] (if (= p piece) (f p) p)) %))

(defn pieces-view [app owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [piece x y]}]
      (let [remaining (:remaining app)
            piece-margin 10
            component-width (+ (* piece-diameter remaining) (* piece-margin (- remaining 1)))
            left-x (- x (/ component-width 2))
            left-x-centre (+ left-x piece-radius)]
        (apply dom/g nil
               (map (fn [n]
                      (dom/circle #js {:cx (+ left-x-centre (* n (+ piece-margin piece-diameter)))
                                       :cy y
                                       :r piece-radius
                                       :stroke "black"
                                       :strokeWidth 2
                                       :fill (piece-fill piece)}))
                    (range remaining)))))))

(defn board-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:clicks (chan)})
    om/IWillMount
    (will-mount [_]
      (do
       (let [clicks (om/get-state owner :clicks)]
         (go (loop []
               (let [position (<! clicks)
                     turn (:turn @app)
                     remaining (get-in @app [:players turn :remaining])]
                 (if (> remaining 0)
                   (if (= (:piece position) :empty)
                     (do
                      (om/update! app [:players turn :remaining] (- remaining 1))
                      (om/update! app :turn (other-piece turn))
                      (om/transact! app :pieces 
                                    (transforming-piece position #(assoc % :piece turn)))))))
               (recur))))))
    om/IRenderState
    (render-state [_ state]
      (apply dom/svg #js {:width width :height height}
        (dom/circle #js {:cx centre-x
                         :cy centre-y
                         :r spoke-length
                         :stroke "black"
                         :strokeWidth 1
                         :fill "white"})
        (om/build pieces-view
                  (get-in app [:players :red])
                  {:init-state {:piece :red :x centre-x :y piece-diameter}})
        (om/build pieces-view
                  (get-in app [:players :blue])
                  {:init-state {:piece :blue :x centre-x :y (- height piece-diameter)}})
        (concat (map stroke-from-centre outer-piece-positions)
                (om/build-all position-view (:pieces app) {:init-state state}))))))

(defn turn-view [app owner]
  (om/component
   (let [turn (:turn app)]
     (dom/p #js {:style #js {:color ({:red "#cc0000" :blue "#0000cc"} turn)}}
            ({:red "Red's turn" :blue "Blue's turn"} turn)))))

(om/root board-view
         game-state
         {:target (.getElementById js/document "merels")})

(om/root turn-view
         game-state
         {:target (.getElementById js/document "turn-text")})
