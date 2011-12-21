(ns ships.core
  (:import (com.jme3.app SimpleApplication)
           (com.jme3.material Material)
           (com.jme3.math Vector3f Vector2f Plane Quaternion FastMath)
           (com.jme3.scene Geometry)
           (com.jme3.scene.shape Box)
           (com.jme3.math ColorRGBA)
           (com.jme3.input KeyInput)
           (com.jme3.input.controls KeyTrigger ActionListener AnalogListener MouseButtonTrigger))
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

(defmacro map-enums [enumclass]
  `(apply merge (map #(hash-map (keyword (.name %)) %) (~(symbol (apply str (name enumclass) "/values"))))))

;; Action queue to run actions in main thread (mostly for REPL use)
(def action-list (ref []))

(defn enlist
  "Enlist a form to be run in the main GUI thread"
  [form]
  (dosync
   (alter action-list conj form)))

(defn handle-actionlist!
  "The GUI thread will call this to invoke all the actions onthe queue stack"
  []
  (loop [action (first @action-list)]
    ;; Pop it off the stack before actioning it so that if there's problems, it gets effectively skipped.
    (let [next (first (dosync (alter action-list rest)))]
      (eval action)
      (if (not (nil? next)) (recur next)))))

(def rootapp* (atom {}))

(defn assoc-to-root! [& items]
  (apply swap! rootapp* assoc items))

(defn rootnode []
  (.getRootNode (:app @rootapp*)))

(defn asset-manager []
  (.getAssetManager (:app @rootapp*)))

(defn input-manager []
  (.getInputManager (:app @rootapp*)))

(defn viewport [] (.getViewPort (:app @rootapp*)))

(def rotator (agent 0))

(defn rotate-node [_ total node]
  (.rotate node 0.1 0.1 0)
  (Thread/sleep 100)
  (send rotator rotate-node (- total 0.1) node)
  (- total 0.1))

(defn -main [& args]
  (swank.swank/start-repl 4005)
  (doto (new ships.core) (.start)))

(defn vec3f [[x y z]]
  (Vector3f. x y z))


(defn add-sky [node]
  (let [skyhi (com.jme3.asset.TextureKey. "Textures/sky_ang.jpg" true)]
    (doto skyhi (.setGenerateMips true) (.setAsCube false))
    (let [texhi (.loadTexture (asset-manager) skyhi)
          sky (com.jme3.util.SkyFactory/createSky (asset-manager) texhi true)]
      (.attachChild node sky)
      KeyTrigger
      )
    )
  )

(defn remove-lights []
  (doall (for [light (seq (.getWorldLightList (rootnode)))]
           (.removeLight (rootnode) light))))

(defn create-box
  ([location size color] (create-box location size color (rootnode)) )
  ([location [sx sy sz] color root]
     (let [b (Box. (vec3f location) sx sy sz)
           geom (com.jme3.scene.Geometry. "Box" b)
           mat (Material. (asset-manager) "Common/MatDefs/Light/Lighting.j3md")]
       (doto mat
         (.setFloat "Shininess" 5)
         (.setTexture "DiffuseMap" (.loadTexture (asset-manager) "Textures/wood.jpg")))
       (.setMaterial geom mat)
       (.attachChild root geom)
       b)))

(defn direction-light [[dx dy dz] color]
  (doto (com.jme3.light.DirectionalLight.)
    (.setColor color)
    (.setDirection (.normalizeLocal (Vector3f. dx dy dz)))))

(defn point-light [location radius color]
  (doto (com.jme3.light.PointLight.)
    (.setColor color)
    (.setRadius radius)
    (.setPosition (vec3f location))))

(defn ambient-light [color]
  (doto (com.jme3.light.AmbientLight.)
    (.setColor color)))

(defn load-model [modelfile]
  (let [result (.loadModel  (asset-manager) modelfile)
        mat (Material. (asset-manager) "Common/MatDefs/Misc/ShowNormals.j3md")]
    (.setMaterial result mat)
    result
    )
  )

(defn new-node
  ([] (com.jme3.scene.Node.))
  ([name] (com.jme3.scene.Node. name)))

(defn v3f [[x y z]]
  (Vector3f. x y z))

(defn water-processor [node location]
  (let [location (v3f location)
        plane (Plane. (Vector3f/UNIT_Y) (.dot location (Vector3f/UNIT_Y)))]
    (doto (com.jme3.water.SimpleWaterProcessor. (asset-manager))
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
    (.addProcessor (viewport) processor)
    (.attachChild (rootnode) water)

    
    (assoc-to-root! :water-processor processor)
    (assoc-to-root! :water water)
    )
  )

(defn setup-player []
  (let [box (Box. (Vector3f/ZERO), 0.5, 1, 0.5)
        player (Geometry. "Player" box)
        mat (Material. (asset-manager) "Common/MatDefs/Light/Lighting.j3md")]
    (doto mat
      (.setFloat "Shininess" 5)
      (.setColor "Diffuse" (ColorRGBA/Blue))
      )
    (.setMaterial player mat)
    (.attachChild (:mainscene @rootapp*) player)
    (assoc-to-root! :player player)
    
    )
  )

(defn handle-analog [^String name value tpf]
  (let [player (:player @rootapp*)
        local (.getLocalTranslation player)]
    (cond
     (.equals "Left" name) (.setLocalTranslation player (- (.x local) (* 10 tpf)) (.y local) (.z local))
     (.equals "Right" name) (.setLocalTranslation player (+ (.x local) (* 10 tpf)) (.y local) (.z local))
     (.equals "Back" name) (.setLocalTranslation player (.x local) (- (.y local) (* 10 tpf)) (.z local))
     (.equals "Forward" name) (.setLocalTranslation player (.x local) (+ (.y local) (* 10 tpf)) (.z local))
     (.equals "Rotate" name) (.rotate 0.0  (* 10.0 tpf) 0.0)
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
  (.addMapping (input-manager) name (into-array com.jme3.input.controls.Trigger triggers)))

(defn setup-keybindings []
  (add-input-mapping "Pause" (KeyTrigger. (KeyInput/KEY_P)))
  (add-input-mapping "Left" (KeyTrigger. (KeyInput/KEY_J)))
  (add-input-mapping "Right" (KeyTrigger. (KeyInput/KEY_L)))
  (add-input-mapping "Forward" (KeyTrigger. (KeyInput/KEY_I)))
  (add-input-mapping "Back" (KeyTrigger. (KeyInput/KEY_K)))
  (add-input-mapping "Rotate" (KeyTrigger. (KeyInput/KEY_SPACE)))
  
  (doto (input-manager)
    (.addListener (get-action-listener) (into-array String ["Pause"]))
    (.addListener (get-analog-listener) (into-array String ["Left" "Right" "Forward" "Back" "Rotate"]))
    ))

(defn -simpleInitApp [this]
  (assoc-to-root! :app this)
  
  (println "Starting simple app")
  (let [main-scene (new-node "MainScene")]
    (assoc-to-root! :mainscene main-scene)
    (create-box [1 1 1] [1 1 1] ColorRGBA/Blue main-scene)
    (create-box [1 3 1] [1 1 1] ColorRGBA/Red main-scene)
    (create-box [-100 -20 -100] [200 0.2 200] ColorRGBA/Yellow main-scene)
    (.addLight main-scene (ambient-light (.mult (ColorRGBA/White) 1.13)))
    (.addLight main-scene (direction-light [-1.13 -1.13 1.13] (.mult (ColorRGBA/White) 0.7)))
    (add-sky main-scene)
    (setup-player)
    (setup-keybindings)
    (.attachChild (rootnode) main-scene)
    (add-water main-scene [0 0 0])
    )

  (.setPauseOnLostFocus this false)
  (.setMoveSpeed (.getFlyByCamera (:app @rootapp*)) 30.0)
  
)

(defn -simpleUpdate [this timediff]
  (try
    (handle-actionlist!)
    (catch Exception e* (.printStackTrace e*)))
  ;(.setLocalTransform (rootnode) com.jme3.math.Transform/IDENTITY)
  ;(.rotate (rootnode) 0 timediff 0)
  )

