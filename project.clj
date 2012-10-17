(defproject ships "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :repositories {"oss-sonatype" "https://oss.sonatype.org/content/repositories/snapshots/"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.jme3/jME3-core "3.0.0-SNAPSHOT"]
                 [com.jme3/jME3-blender "3.0.0-SNAPSHOT"]
                 [com.jme3/jME3-desktop "3.0.0-SNAPSHOT"]
                 [com.jme3/jME3-lwjgl "3.0.0-SNAPSHOT"]
                 [com.jme3/jME3-lwjgl-natives "3.0.0-SNAPSHOT"]
                 [com.jme3/jME3-plugins "3.0.0-SNAPSHOT"]
                 [com.jme3/jME3-jogg "3.0.0-SNAPSHOT"]
                 [com.jme3/jmonkeyengine3 "3.0.0-SNAPSHOT"]
                 [com.jme3/lwjgl "3.0.0-SNAPSHOT"]
;                 [com.jme3/lwjgl-natives "3.0.0-SNAPSHOT"]
;                 [com.jme3/jogl "3.0.0-SNAPSHOT"]
;                 [com.jme3/jogl-natives "3.0.0-SNAPSHOT"]
;                 [com.jme3/j-ogg-oggd "3.0.0-SNAPSHOT"]
;                 [com.jme3/j-ogg-vorbisd "3.0.0-SNAPSHOT"]
                 [com.jme3/vecmath "3.0.0-SNAPSHOT"]
                 [com.jme3/j-ogg-oggd "3.0.0-SNAPSHOT"]
                 [com.jme3/j-ogg-vorbisd "3.0.0-SNAPSHOT"]
                 [swank-clojure/swank-clojure "1.3.3"]]
  :main ships.core
  
  ;lein 2
  :resource-paths ["assets"]
  ;lein 1
  :resources-path "assets"
  
  :jvm-opts ["-Xmx512m"])