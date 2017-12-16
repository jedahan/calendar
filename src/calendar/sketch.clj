(ns calendar.sketch
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [clj-time.predicates :as pred]))

(def first-of-year (t/date-time 2017 1 1))
(def days (take 365 (p/periodic-seq first-of-year (t/days 1))))
(def month-days (partition-by pred/last-day-of-month? days))

(def ratio [31 12])
(def size (min
            (q/floor (/ (q/screen-width) (first ratio)))
            (q/floor (/ (q/screen-height) (last ratio)))))
(def width (* size (first ratio)))
(def height (* size (last ratio)))

; the setup function run once, and returns the initial state
(defn setup []
  (q/frame-rate 30) ; target 30 frames per second
  {:time 0
   :hue 0
   :brightness 255
   :debug true
   :bold-font (q/create-font "Menlo-Bold" 12 true)
   :regular-font (q/create-font "Menlo-Regular" 12 true)
   :days days})

; mouse-moved runs every time the mouse is moved
; and gets passed an object with the current mouse x and y, and previous mouse x and y
(defn mouse-moved [state mouse]
  (let [hue (q/map-range (:x mouse) 0 (q/width) 0 255)
        brightness (q/map-range (:y mouse) 0 (q/height) 0 255)]
    (assoc state :hue hue :brightness brightness)))

; key-pressed for debug mode, and saving graphics
(defn key-pressed [state event]
  (let [key (:key event)]
    (cond
      (= :esc key) (q/exit)
      (= :q key) (q/exit)
      (= :d key) (assoc state :debug (not (:debug state)))
      (= :s key) (assoc state :save-frame true)
      :else (do (println (str event)) state))))

; update-state runs before every frame
(defn update-state [state]
  (let [t (/ (q/frame-count) (q/target-frame-rate))]
    (if (state :save-frame) (q/save-frame "calendar-####.png"))
    (assoc state :time t :save-frame false)))

(defn draw-state [state]
  (q/color-mode :hsb)
  (q/background (:hue state) 140 (:brightness state))
  (q/no-stroke)
  (q/fill 0 0 255)
  (q/text-align :right)
  (doseq [day (:days state)]
    (let [day-of-month (t/day day)
          day-of-week (t/day-of-week day)
          month-of-year (t/month day)
          first-day-of-week (t/day-of-week (t/first-day-of-the-month day))
          x (* 0.8 (/ (+ first-day-of-week day-of-month) 31) (q/width))
          y (* 0.9 (/ month-of-year 12) (q/height))]
      (if (= 1 day-of-week)
        (q/text-font (:bold-font state))
        (q/text-font (:regular-font state)))
      (q/text (str day-of-month) x y)))

  (if (:debug state)
    (let [text-size 20
          line-height (* 1.25 text-size)
          x 0
          y (- (q/height) text-size)]
      (q/fill 0 0 255)
      (q/text-size text-size)
      (q/text-align :right)
      (q/text "'d' toggles debug
              's' screenshots
              'q' quits" (- (q/width) 10) (- y 10 (* 2 line-height)))
      (q/text-align :left)
      (let [newlines (clojure.string/replace (str (assoc state :time (q/floor (:time state)))) "," "\n")
            no-brackets (clojure.string/replace newlines "}" "")
            state-info (clojure.string/replace no-brackets "{" " ")]
        (q/text state-info 0 line-height)))))

(q/defsketch calendar
  :host "calendar"
  :size [width height]
  :setup setup
  :update update-state
  :draw draw-state
  :mouse-moved mouse-moved
  :key-pressed key-pressed
  :middleware [m/fun-mode])
