;; Merels

(ns om-merels.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om-merels.math :as math]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

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

(defn position-view [{:keys [centre-x centre-y piece mouse-over? selected?] :as state} owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [highlight]}]
      (dom/circle #js {:cx centre-x
                       :cy centre-y
                       :r piece-radius
                       :stroke (if mouse-over? "red" "black")
                       :strokeWidth 2
                       :fill (piece-fill piece)
                       :onMouseOver (fn [_] (put! highlight state))
                       :onMouseOut (fn [_] (put! highlight :clear))
                       }))))

(defn stroke-from-centre [{:keys [x y]}]
  (dom/line #js {:x1 centre-x :y1 centre-y :x2 x :y2 y :stroke "black"}))

(defn transforming-piece [piece f]
  #(map (fn [p] (if (= p piece) (f p) p)) %))

(defn board-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:highlight (chan)})
    om/IWillMount
    (will-mount [_]
      (let [highlight (om/get-state owner :highlight)]
        (go (loop []
              (let [position (<! highlight)]
                (om/transact! app :pieces
                  (if (= :clear position)
                    #(map (fn [piece] (assoc piece :mouse-over? nil)) %)
                    (transforming-piece position #(assoc position :mouse-over? true)))))
              (recur)))))
    om/IRenderState
    (render-state [_ state]
      (apply dom/svg #js {:width width :height height}
        (dom/circle #js {:cx centre-x
                         :cy centre-y
                         :r spoke-length
                         :stroke "black"
                         :strokeWidth 1
                         :fill "white"})
        (concat (map stroke-from-centre outer-piece-positions)
                (om/build-all position-view (:pieces app) {:init-state state}))))))


(om/root board-view
         game-state
         {:target (.getElementById js/document "merels")})
