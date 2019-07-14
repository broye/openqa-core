(defproject core "0.1.0-SNAPSHOT"
  :description "OpenQA, open question answer / open quora alike core."
  :url "https://github.com/broye/openqa-core"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 ;; https://mvnrepository.com/artifact/io.netty/netty-all
                 [io.netty/netty-all "4.1.37.Final"]
                 [io.vertx/vertx-core "3.7.1"]
                 ]
  :main ^:skip-aot core.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
