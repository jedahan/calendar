(ns calendar.sketch
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]))

(def first-of-year (t/date-time (t/year (t/date-time))))
(def days (take 365 (t/days first-of-year)))
(def ratio [31 12])
(def size (min
            (q/floor (/ (q/screen-width) (first ratio)))
            (q/floor (/ (q/screen-height) (last ratio)))))
(def width (* size (first ratio)))
(def height (* size (last ratio)))

; the setup function run once, and returns the initial state
(defn setup []
  (q/frame-rate 30) ; target 30 frames per second
  {
   :hue 0
   :debug true
   :days days
  }
)

; mouse-moved runs every time the mouse is moved
; and gets passed an object with the current mouse x and y, and previous mouse x and y
(defn mouse-moved [state mouse]
  (let [hue (q/map-range (:x mouse) 0 (q/width) 0 255)]
    (assoc state :hue hue)
  ))

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
  (let [t (/ (q/frame-count) (q/target-frame-rate)) ]
    (if (state :save-frame) (q/save-frame "calendar-####.png"))
    (assoc state :time t :save-frame false)))

(defn draw-state [state]
  (q/color-mode :rgb)
  (q/background 0 140 255)
  (q/no-stroke)
  (q/color-mode :hsb)
  (q/fill (:hue state) 200 200)
  (q/text-size 8)
  (doseq [[month-index month] (map-indexed vector (:months state))]
    (doseq [[day-index day] (map-indexed vector (take 31 (cycle (:days state))))]
      (q/text-num (q/floor day-index) (* (/ day-index 31) (q/width)) (* (/ month-index 12) (q/height)))
    )
  )
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
      (let [newlines (clojure.string/replace (str (assoc state :time (q/floor (:time state)))) "," "\n" )
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
