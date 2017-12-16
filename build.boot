(set-env!
 :source-paths #{"src"}
 :resource-paths #{"html"}

 :dependencies '[
                 [org.clojure/core.async "RELEASE"]
                 [org.clojure/clojurescript "RELEASE"]
                 [pandeiro/boot-http "RELEASE"]                     ; to serve clojurescript and html
                 [quil "RELEASE"]
                 [clj-time "RELEASE"]

                 [org.clojure/clojure "RELEASE" :scope "test"]
                 [adzerk/boot-cljs "RELEASE" :scope "test"]         ; build cljs
                 [adzerk/boot-cljs-repl "RELEASE" :scope "test"]    ; connect repl to browser
                 [adzerk/boot-reload "RELEASE" :scope "test"]       ; reload on build
                 [com.cemerick/piggieback "RELEASE" :scope "test"]  ; needed for boot-reload
                 [weasel "RELEASE" :scope "test"]                   ; needed for boot-reload
                 [org.clojure/tools.nrepl "RELEASE" :scope "test"]] ; needed for cljs-repl
)

(task-options!
  pom {:project 'calendar
       :version "0.1.0"})

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl]]
         '[adzerk.boot-reload :refer [reload]]
         '[pandeiro.boot-http :refer [serve]]
         '[clj-time.core :as t]
         '[clj-time.periodic :as p])

(deftask dev
  "dev clojure"
  []
  (comp (pom) (jar) (install))
)

(deftask develop
  "Serve up live-reloading site on localhost:3000 with notifications"
  []
  (comp (serve)
        (watch)
        (cljs-repl)
        (reload :on-jsload 'calendar.sketch/calendar)
        (cljs :optimizations :none :source-map true)
        (notify :audible true)
        (target :dir #{"target"})
        ))

(deftask build
  "Build optimized version of code to :directories (default `target/`)"
  [d directories PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq directories) directories #{"target"})]
    (comp
       (cljs :optimizations :advanced :source-map true)
       (target :dir dir)))
  )

(deftask github-pages
  "Build a production version to github-pages `docs/` directory"
  []
  (comp (build :directories ["docs/"])))
