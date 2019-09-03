(defproject  stigmergy/voodoo "0.0.1-SNAPSHOT"
  :min-lein-version "2.8.3" 
  :dependencies [[org.clojure/clojure "1.10.1"]  
                 [org.clojure/clojurescript "1.10.520"]
                 [com.taoensso/timbre "4.10.0"]
                 [commons-codec/commons-codec "1.13"]]
  :plugins [[lein-figwheel "0.5.18"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    :target-path]
  :source-paths ["src/cljc"]
  
  :profiles {:project/dev {:dependencies [[figwheel-sidecar "0.5.18"]
                                          [cider/piggieback "0.4.0"]
                                          [binaryage/devtools "0.9.10"]]
                           :source-paths ["src/clj" "src/cljc" "env/dev/clj"]}
             :project/prod {:prep-tasks ["compile" ["cljsbuild" "once" "prod"]]
                            :source-paths ["src/clj" "src/cljc"]
                            :main stigmergy.cdr.server
                            :aot :all}

             :dev [:project/dev]
             :uberjar [:project/prod]
             }
  )
