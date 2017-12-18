(ns calendar.sketch
  "Recreation of popul aere produkt wall calendar"
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [clojure.string :as s]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [clj-time.predicates :as d]))

(def first-of-year (t/date-time (t/year (t/now)) 1 1))
(def days (take 365 (p/periodic-seq first-of-year (t/days 1))))
(def months (partition-by t/month days))

; for horizontal orientation...
(def width (q/screen-width))
(def height (q/floor (*
                       (/ width (count months)
                       (apply max (map count months))))))

(defn day-to-grid [day]
  "Maps a day to a normalized grid with 12 rows and 31+ columns"
  (let [day-of-month (t/day day)
        day-of-week (t/day-of-week day)
        month-of-year (t/month day)
        day-of-week-of-the-first-day-of-the-month (t/day-of-week (t/first-day-of-the-month day))
        number-of-months (count months)
        longest-month-in-days (apply max (map count months))
        x (/ (+ day-of-month day-of-week-of-the-first-day-of-the-month) longest-month-in-days)
        y (/ month-of-year number-of-months)]
    {:x x :y y :bold (= 1 day-of-week) :text (str day-of-month)}))

(defn setup []
  "Return the initial state in setup"
  (q/frame-rate 30) ; target 30 frames per second
  (let [font-name "Menlo"
        font-size 12]
    {:time 0
     :hue 0
     :brightness 255
     :debug true
     :font-name font-name
     :font-size font-size
     :bold-font (q/create-font (str font-name "-Bold") font-size true)
     :regular-font (q/create-font (str font-name "-Regular") font-size true)
     :days (map day-to-grid days)
     :months months}))

(defn mouse-moved [state mouse]
  "Update the background hue with mouse x and brightness with mouse y"
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

(defn snapshot [state]
  "Save a png with the code and state in its metadata"
  (let [frame-count (q/frame-count)
        filename-out (str "calendar-" frame-count ".png")]
    (q/save "calendar-tmp.png")))
;    (png/bake "calendar-tmp.png" filename-out [
;       ["code" (slurp "src/calendar/sketch.clj")]
;       ["state" (str state)]
;       ["author" "Jonathan Dahan"]
;    ])))

; update-state runs before every frame
(defn update-state [state]
  "Update the time, and handle saving a snapshot"
  (let [now (/ (q/frame-count) (q/target-frame-rate))]
    (if (:snapshot state) (snapshot state))
    (assoc state :time now :snapshot false)))

(defn prettify [state]
  "Only show human-understandable state - numbers, strings, booleans, round time"
  (let [filtered (juxt state :time :hue :brightness :font-name :font-size)
        time-normalized (assoc filtered :time (q/floor (:time filtered)))
        newlines (s/replace (str time-normalized) "," "\n")
        no-brackets (s/replace newlines "}" "")
        state-info (s/replace no-brackets "{" " ")]
  state-info))

(defn draw-debug [state]
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
  (q/text (prettify state) x line-height)))

(defn draw-state [state]
  (q/color-mode :hsb)
  (q/background (:hue state) 140 (:brightness state))
  (q/no-stroke)
  (q/fill 0 0 255)
  (q/text-align :right)
  (doseq [day (:grid state)]
    (q/text-font (if (:bold day) (:bold-font state) (:regular-font state)))
    (q/text (:text day) (* 0.8 (q/width) (:x day)) (* 0.9 (q/height) (:y day))))

  (if (:debug state) (draw-debug state)))

(q/defsketch calendar
  :host "calendar"
  :size [width height]
  :setup setup
  :update update-state
  :draw draw-state
  :mouse-moved mouse-moved
  :key-pressed key-pressed
  :middleware [m/fun-mode])
