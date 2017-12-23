(ns calendar.sketch
  "Recreation of popul aere produkt wall calendar"
  (:require [clojure2d.core :refer :all]
            [clojure2d.math :as m]
            [clojure2d.color :as c]
            [clojure.string :as s]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [metapng.core :as metapng]))

(set! *warn-on-reflection* true)
;(set! *unchecked-math* :warn-on-boxed)

(def target-frame-rate 30)
(def first-of-year (t/date-time (t/year (t/now)) 1 1))
(def days (take 365 (p/periodic-seq first-of-year (t/days 1))))
(def months (partition-by t/month days))
(def number-of-months (count months))
(def longest-month (apply max (map count months)))

; for horizontal orientation...
;(def width (q/screen-width))
(def target-width 1680)
(def target-height (m/floor (* number-of-months (/ target-width longest-month) )))

(defn day-to-grid [day]
  "Maps a day to a normalized grid with 12 rows and 31+ columns"
  (let [day-of-month (t/day day)
        day-of-week (t/day-of-week day)
        month-of-year (t/month day)
        first-day-of-month (t/day-of-week (t/first-day-of-the-month day))
        x (/ (+ day-of-month first-day-of-month) longest-month)
        y (/ month-of-year number-of-months)]
    {:x x :y y :bold (= 1 day-of-week) :text (str day-of-month)}))

(defmethod mouse-event [(str *ns*) :mouse-moved] [event state]
  "Update the background hue with mouse x and brightness with mouse y"
  (let [hue (m/norm (mouse-x event) [0 target-width] [0 255])
        brightness (m/norm (mouse-y event) [0 target-height] [0 255])]
    (assoc state :hue hue :brightness brightness)))

(defmethod key-pressed [(str *ns*) \d] [event state]
  "d toggles debug"
  (assoc state :debug (not (:debug state))))

(defmethod key-pressed [(str *ns*) \s] [event state]
  "s makes a snapshot image"
  (assoc state :snapshot true))

(defmethod key-pressed [(str *ns*) virtual-key] [event state]
  "q/esc for exit"
  (condp = (key-code event)
    :esc (close-window)
    :q (close-window)))

(defn snapshot [canvas state framecount]
  "Save a png with the code and state in its metadata"
  (save canvas "tmp.png")
  (metapng/bake "tmp.png" (str *ns* "-" framecount ".png") {
    :code (slurp (str "src/" (replace (str *ns*) ["." "/"]) ".clj"))
    :state (str state)
    :author "Jonathan Dahan"}))

(defn prettify [state]
  "Only show human-understandable state - numbers, strings, booleans, round time"
  (let [filtered (select-keys state [:time :hue :brightness :font-name :font-size])
        time-normalized (assoc filtered :time (m/floor (:time filtered)))]
    (-> time-normalized
        (s/replace ", " "\n")
        (s/replace "{" "")
        (s/replace "}" ""))))

(defn draw-debug [state]
  (let [text-size 20
        line-height (* 1.25 text-size)
        x 0
        y (- target-height text-size)]
    (set-color :white)
    ;(q/text-align :right)
    (text "'d' toggles debug
            's' screenshots
            'q' quits" (- target-width 10) (- y 10 (* 2 line-height)))
    ;(q/text-align :left)
    (text (prettify state) x line-height)))

(defn draw [canvas window ^long framecount state]
  "Draw rotating rectangle. This function is prepared to be run in refreshing thread from your window."
  (if (= framecount 0)
    (println (str *ns*))
    (assoc state {:time 0
                  :hue 0
                  :brightness 255
                  :debug true
                  :font-name "Menlo"
                  :font-size 12
                  :grid (map day-to-grid days)}))

  (println (:grid state))

  (let [now (/ framecount target-frame-rate)]
    (if (:snapshot state) (snapshot state framecount))
    (assoc state :time now :snapshot false))

  (-> canvas
    (set-background (c/from-HSB (:hue state) 140 (:brightness state)))
    (set-color :white)
    ;(q/text-align :right)
    (doall [day (:grid state)]
      (set-font-attributes (:font-size state) (if (:bold day) :bold))
      (text (:text day) (* 0.8 target-width (:x day)) (* 0.9 target-height (:y day))))
    (if (:debug state) (draw-debug state))))

(let [canvas (make-canvas target-width target-height :high "Menlo")]
 (show-window canvas (str *ns*) target-width target-height target-frame-rate draw))
