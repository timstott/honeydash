(defproject honeydash "1.0.0"
  :description "A Honeybadger dashboard for teams"
  :url "http://github.com/timstott/honeydash"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :min-lein-version "2.5.3"

  :dependencies [[clj-http "2.1.0"]
                 [cljs-http "0.1.39"]
                 [com.cognitect/transit-cljs "0.8.237"]
                 [compojure "1.4.0"]
                 [inflections "0.12.0"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"
                  :exclusions [org.clojure/tools.reader]]
                 [prismatic/schema "1.0.5"]
                 [reagent "0.5.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-json "0.4.0"]]

  :plugins [[lein-figwheel "0.5.0-6"]
            [lein-cljsbuild "1.1.2" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src" "test"]

                ;; If no code is to be run, set :figwheel true for continued automagical reloading
                :figwheel {:on-jsload "honeydash.test/run"}

                :compiler {:main honeydash.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/honeydash.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}
               ;; This next build is an compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/honeydash.js"
                           :main honeydash.core
                           :optimizations :advanced
                           :pretty-print false}}
               {:id "test"
                :source-paths ["src" "test"]
                :compiler {:output-to "resources/test/js/compiled/honeydash.js"
                           :optimizations :whitespace
                           :pretty-print true}}]

              :test-commands {"test" ["phantomjs"
                                      "resources/test/test.js"
                                      "resources/test/test.html"]}}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this
             ;; doesn't work for you just run your own server :)
             :ring-handler honeydash.handlers/app

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             })
