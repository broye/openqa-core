(defproject core "0.1.0-SNAPSHOT"
  :description "OQA, Open Question Answer / open quora alike core."
  :url "https://github.com/broye/openqa-core"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 ;; https://mvnrepository.com/artifact/io.netty/netty-all
                 [io.vertx/vertx-core "3.8.0"]
                 [io.forward/yaml "1.0.9"]
                 ["io.vertx/vertx-pg-client" "3.8.0"]
                 ]
  :main ^:skip-aot io.oqa.core.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
