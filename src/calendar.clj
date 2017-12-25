(ns calendar
  "Recreation of popul aere produkt wall calendar"
  (:import  [java.awt Graphics2D Toolkit FontMetrics])
  (:require [clojure2d.core :as c]
            [clojure2d.math :as m]
            [clojure2d.color :as color]
            [clojure.string :as s]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [clj-time.predicates :as is]
            [metapng.core :as metapng]))

(defn right-text
  "Draw text right aligned"
  [canvas ^String s ^long x ^long y]
  (let [width (.stringWidth (.getFontMetrics (.graphics canvas)) s)]
    (c/text canvas s (- x width) y)))

(defn day-to-grid [months-count longest-month day]
  "Maps a day to a normalized grid with 12 rows and 31+ columns"
  (let [day-of-month (t/day day)
        day-of-week (t/day-of-week day)
        month-of-year (t/month day)
        first-day-of-month (t/day-of-week (t/first-day-of-the-month day))
        this-day-of-month (+ day-of-month first-day-of-month)
        x (/ this-day-of-month longest-month)
        y (/ month-of-year months-count)
        bold? (is/sunday? day)]
    {:x x :y y :bold bold? :text (str day-of-month)}))

(defmethod c/mouse-event [(str *ns*) :mouse-moved] [event state]
  "Update the background hue with mouse x and brightness with mouse y"
  (let [hue (m/norm (c/mouse-x event) 0 (:w state) 0 255)
        brightness (m/norm (c/mouse-y event) 0 (:h state) 0 255)]
    (assoc state :hue hue :brightness brightness)))

(defmethod c/key-pressed [(str *ns*) \d] [event state]
  "d toggles debug"
  (assoc state :debug (not (:debug state))))

(defmethod c/key-pressed [(str *ns*) \s] [event state]
  "s makes a snapshot image"
  (assoc state :snapshot true))

(defmethod c/key-pressed [(str *ns*) c/virtual-key] [event state]
  "q/esc for exit"
  (let [k (c/key-code event)]
    (cond
      (= :esc k) (assoc state :close true)
      (= :q k) (assoc state :close true)
      :else (do
              (println (str "unknown key " event))
              state))))

(defn snapshot [canvas state framecount]
  "Save a png with the code and state in its metadata"
  (c/save canvas "tmp.png")
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

(defn draw-debug [canvas state]
  (if (:debug state)
    (let [text-size 20
          line-height (* 1.25 text-size)
          x 0
          y (- (:h state) text-size)]
      (-> canvas
        ;(q/text-align :right)
        (c/text "'d' toggles debug
                's' screenshots
                'q' quits" (- (:w state) 10) (- y 10 (* 2 line-height)))
        ;(q/text-align :left)
        (c/text (prettify state) x line-height)))))

(defn draw [canvas window ^long framecount _]
  "Draw calendar maybe"
  (let [window-state (c/get-state window)
        snapshot? (:snapshot window-state)
        close? (:close window-state)
        now (/ framecount (:fps window))
        w (c/width canvas)
        h (c/height canvas)
        scaled-width (* 0.8 w)
        scaled-height (* 0.9 h)
        state (assoc window-state :time now :w w :h h :snapshot false)
        background-color (color/from-HSB (color/make-color (:hue state) 255 (:brightness state)))]
    (if close? (c/close-window window))
    (if snapshot? (snapshot canvas state framecount))

    (c/set-background canvas background-color)
    (doseq [day (:days state)]
      (c/set-font-attributes canvas (:font-size state) (if (:bold day) :bold :regular))
      (right-text canvas (:text day) (* scaled-width (:x day)) (* scaled-height (:y day))))
    (draw-debug canvas state)
    state))

(let [first-of-year (t/date-time (t/year (t/now)) 1 1)
      days (take 365 (p/periodic-seq first-of-year (t/days 1)))
      months (partition-by t/month days)
      months-count (count months)
      longest-month (apply max (map count months))
      width (.getWidth (.getScreenSize (Toolkit/getDefaultToolkit)))
      height (m/floor (* months-count (/ width longest-month) ))
      canvas (c/make-canvas width height :high "Menlo")
      state {:time 0
             :hue 0
             :brightness 255
             :debug true
             :font-name "Menlo"
             :font-size 12
             :w width
             :h height
             :days (map (partial day-to-grid months-count longest-month) days)}
      window-name (str *ns*)
      fps 30]
  (c/show-window {:canvas canvas :window-name window-name :draw-fn draw :state state :fps fps}))
