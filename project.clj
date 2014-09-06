(defproject om-merels "0.1.0-SNAPSHOT"
  :description "Merels in ClojureScript"
  :url "http://github.com/robertberry"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.7.1"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds [{:id "om-merels"
              :source-paths ["src"]
              :compiler {
                :output-to "om_merels.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
