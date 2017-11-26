(defproject dcs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [orchestra "2017.11.12-1"]
                 [brute "0.4.0"]
                 [org.clojure/test.check "0.10.0-alpha2"]]
  :main ^:skip-aot dcs.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
