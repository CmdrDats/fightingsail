(ns ships.core
  (:import (com.jme3.app SimpleApplication)
           (com.jme3.scene.shape Box)
           (com.jme3.material Material)
           (com.jme3.math Vector3f Vector2f Plane Quaternion FastMath)
           (com.jme3.math ColorRGBA)
           (com.jme3.scene Geometry)
           
           (com.jme3.input KeyInput)
           (com.jme3.input.controls KeyTrigger ActionListener AnalogListener MouseButtonTrigger))
  (:require [ships.state :as state])
  (:require [ships.jme :as jme])
  (:require [clojure.set :as set])
  (:require [swank.swank])
  (:gen-class
   :extends com.jme3.app.SimpleApplication))

(defmacro auto-proxy
  "Automatically build a proxy, stubbing out useless entries, ala: http://www.brool.com/index.php/snippet-automatic-proxy-creation-in-clojure"
  [interfaces variables & args]
  (let [defined (set (map #(str (first %)) args))
        names (fn [i] (map #(.getName %) (.getMethods i)))
        all-names (into #{} (apply concat (map names (map resolve interfaces))))
        undefined (set/difference all-names defined) 
        auto-gen (map (fn [x] `(~(symbol x) [& ~'args])) undefined)]
    `(proxy ~interfaces ~variables ~@args ~@auto-gen)))


(def rotator (agent 0))

(defn rotate-node [_ total node]
  (.rotate node 0.1 0.1 0)
  (Thread/sleep 100)
  (send rotator rotate-node (- total 0.1) node)
  (- total 0.1))

(defn start []
  (doto (new ships.core) (.start))
  )

(defn -main [& args]
  (swank.swank/start-repl 4005)
  (start))


(defn add-sky [node]
  (let [skyhi (com.jme3.asset.TextureKey. "Textures/sky_ang.jpg" true)]
    (doto skyhi (.setGenerateMips true) (.setAsCube false))
    (let [texhi (.loadTexture (state/asset-manager) skyhi)
          sky (com.jme3.util.SkyFactory/createSky (state/asset-manager) texhi true)]
      (.attachChild node sky)
      KeyTrigger
      )
    )
  )

(defn remove-lights []
  (doall (for [light (seq (.getWorldLightList (state/rootnode)))]
           (.removeLight (state/rootnode) light))))

(defn create-box
  ([location size color] (create-box location size color (state/rootnode)) )
  ([location size color root]
       (let [b (jme/box "Box" location size)]
         (jme/set-material b :light-lighting
                           ["Shininess" (float 5)
                            "DiffuseMap" (jme/texture "Textures/wood.jpg")])
         (.attachChild root b) b)))

(defn load-model [modelfile]
  (let [result (.loadModel  (state/asset-manager) modelfile)
        mat (Material. (state/asset-manager) "Common/MatDefs/Misc/ShowNormals.j3md")]
    (.setMaterial result mat)
    result
    )
  )

(defn new-node
  ([] (com.jme3.scene.Node.))
  ([name] (com.jme3.scene.Node. name)))

(defn water-processor [node location]
  (let [location (jme/vec3f location)
        plane (Plane. (Vector3f/UNIT_Y) (.dot location (Vector3f/UNIT_Y)))]
    (doto (com.jme3.water.SimpleWaterProcessor. (state/asset-manager))
      (.setReflectionScene node)
      (.setPlane plane)
      (.setWaterDepth 40)
      (.setDistortionScale 0.2)
      (.setWaterTransparency  0.2)
      (.setWaveSpeed 0.02)
      )
    )
  )

(defn add-water [node location]
  (let [quad (doto (com.jme3.scene.shape.Quad. 400 400)
               (.scaleTextureCoordinates (Vector2f. 6.0 6.0)))
        processor (water-processor node location)
        plane (.createWaterGeometry processor 400 400)
        water (doto plane
                (.setLocalTranslation -200 0 200)
                (.setMaterial (.getMaterial processor)))]
    (.addProcessor (state/viewport) processor)
    (.attachChild (state/rootnode) water)

    
    (state/assoc-to-root! :water-processor processor)
    (state/assoc-to-root! :water water)
    )
  )

(defn setup-player []
  (let [player (jme/box "Player" [0 0 0] [0.5 1 0.5])
        mat (Material. (state/asset-manager) "Common/MatDefs/Light/Lighting.j3md")]
    (jme/set-material player :light-lighting ["Shininess" 5
                                              "Diffuse" (ColorRGBA/Blue)])
    (.attachChild (:mainscene @state/rootapp) player)
    (state/assoc-to-root! :player player)
    
    )
  )

(defn handle-analog [^String name value tpf]
  (let [player (:player @state/rootapp)
        local (.getLocalTranslation player)]
    (cond
     (.equals "Left" name) (.setLocalTranslation player (- (.x local) (* 10 tpf)) (.y local) (.z local))
     (.equals "Right" name) (.setLocalTranslation player (+ (.x local) (* 10 tpf)) (.y local) (.z local))
     (.equals "Back" name) (.setLocalTranslation player (.x local) (- (.y local) (* 10 tpf)) (.z local))
     (.equals "Forward" name) (.setLocalTranslation player (.x local) (+ (.y local) (* 10 tpf)) (.z local))
     (.equals "Rotate" name) (.rotate player 0.0  (* 10.0 tpf) 0.0)
     )
    )
  )

(defn get-action-listener []
  (proxy [ActionListener] []
              (onAction [^String name keypressed tpf] (.println (System/out) (str "Pressed pause? :) " keypressed)))))

(defn get-analog-listener []
  (proxy [AnalogListener] []
    (onAnalog [^String name value tpf]
      (handle-analog name value tpf))))

(defn add-input-mapping [name & triggers]
  (.addMapping (state/input-manager) name (into-array com.jme3.input.controls.Trigger triggers)))

(defn setup-keybindings []
  (add-input-mapping "Pause" (KeyTrigger. (KeyInput/KEY_P)))
  (add-input-mapping "Left" (KeyTrigger. (KeyInput/KEY_J)))
  (add-input-mapping "Right" (KeyTrigger. (KeyInput/KEY_L)))
  (add-input-mapping "Forward" (KeyTrigger. (KeyInput/KEY_I)))
  (add-input-mapping "Back" (KeyTrigger. (KeyInput/KEY_K)))
  (add-input-mapping "Rotate" (KeyTrigger. (KeyInput/KEY_SPACE)))
  
  (doto (state/input-manager)
    (.addListener (get-action-listener) (into-array String ["Pause"]))
    (.addListener (get-analog-listener) (into-array String ["Left" "Right" "Forward" "Back" "Rotate"]))
    ))

(defn -simpleInitApp [this]
  (state/assoc-to-root! :app this)
  
  (println "Starting simple app")
  (let [main-scene (new-node "MainScene")]
    (state/assoc-to-root! :mainscene main-scene)
    (create-box [1 1 1] [1 1 1] ColorRGBA/Blue main-scene)
    (create-box [1 3 1] [1 1 1] ColorRGBA/Red main-scene)
    (create-box [-100 -20 -100] [200 0.2 200] ColorRGBA/Yellow main-scene)
    (.addLight main-scene (jme/ambient-light (.mult (ColorRGBA/White) 1.13)))
    (.addLight main-scene (jme/direction-light [-1.13 -1.13 1.13] (.mult (ColorRGBA/White) 0.7)))
    (add-sky main-scene)
    (setup-player)
    (setup-keybindings)
    (.attachChild (state/rootnode) main-scene)
    ;(add-water main-scene [0 0 0])
    )

  ;(.setPauseOnLostFocus this false)
  (.setMoveSpeed (.getFlyByCamera (:app @state/rootapp)) 30.0)
  
)

(defn -simpleUpdate [this timediff]
  (try
    (state/handle-actionlist!)
    (catch Exception e* (.printStackTrace e*)))
  ;(.setLocalTransform (state/rootnode) com.jme3.math.Transform/IDENTITY)
  ;(.rotate (state/rootnode) 0 timediff 0)
  )

