(ns ships.core
  (:import (com.jme3.app SimpleApplication)
           (com.jme3.material Material)
           (com.jme3.math Vector3f Vector2f Plane Quaternion FastMath)
           (com.jme3.scene Geometry)
           (com.jme3.scene.shape Box)
           (com.jme3.math ColorRGBA))
  (:require [swank.swank])
  (:gen-class
   :extends com.jme3.app.SimpleApplication))

(def rootapp* (atom {}))

(defn assoc-to-root! [& items]
  (apply swap! rootapp* assoc items))

(defn rootnode []
  (.getRootNode (:app @rootapp*)))

(defn asset-manager []
  (.getAssetManager (:app @rootapp*)))

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

;; TextureKey skyhi = new TextureKey("Textures/skyboxes/skybox_01.png", true);
;; skyhi.setGenerateMips(true);
;; skyhi.setAsCube(false);
;; Texture texhi = assetManager.loadTexture(skyhi);
;; Geometry sp = (Geometry) SkyFactory.createSky(assetManager, texhi, true);
;; rootNode.attachChild(sp);

(defn add-sky [node]
  (let [skyhi (com.jme3.asset.TextureKey. "Textures/sky_ang.jpg" true)]
    (doto skyhi (.setGenerateMips true) (.setAsCube false))
    (let [texhi (.loadTexture (asset-manager) skyhi)
          sky (com.jme3.util.SkyFactory/createSky (asset-manager) texhi true)]
      (.attachChild node sky)
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
           mat (Material. (.getAssetManager (:app @rootapp*)) "Common/MatDefs/Light/Lighting.j3md")]
       (doto mat
         (.setFloat "Shininess" 5)
         ;(.setBoolean "UseMaterialColors" true)
         ;(.setColor "Ambient"  ColorRGBA/Black)
         ;(.setColor "Diffuse"  color)
         ;(.setColor "Specular"  ColorRGBA/Gray)
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

;; // we create a water processor
;; SimpleWaterProcessor waterProcessor = new SimpleWaterProcessor(assetManager);
;; waterProcessor.setReflectionScene(mainScene);

;; // we set the water plane
;; Vector3f waterLocation=new Vector3f(0,-6,0);
;; waterProcessor.setPlane(new Plane(Vector3f.UNIT_Y, waterLocation.dot(Vector3f.UNIT_Y)));
;; viewPort.addProcessor(waterProcessor);

;; // we set wave properties
;; waterProcessor.setWaterDepth(40);         // transparency of water
;; waterProcessor.setDistortionScale(0.05f); // strength of waves
;; waterProcessor.setWaveSpeed(0.05f);       // speed of waves

;; // we define the wave size by setting the size of the texture coordinates
;; Quad quad = new Quad(400,400);
;; quad.scaleTextureCoordinates(new Vector2f(6f,6f));

;; // we create the water geometry from the quad
;; Geometry water=new Geometry("water", quad);
;; water.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
;; water.setLocalTranslation(-200, -6, 250);
;; water.setShadowMode(ShadowMode.Receive);
;; water.setMaterial(waterProcessor.getMaterial());
;; rootNode.attachChild(water);


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

;; Geometry waterPlane = waterProcessor.createWaterGeometry(10, 10);
;; waterPlane.setLocalTranslation(-5, 0, 5);
;; waterPlane.setMaterial(waterProcessor.getMaterial());

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
    (.attachChild (rootnode) main-scene)
    (add-water main-scene [0 0 0])
    )

  
  (.setMoveSpeed (.getFlyByCamera (:app @rootapp*)) 30.0)
  
)

(defn -simpleUpdate [this timediff]
  ;(.setLocalTransform (rootnode) com.jme3.math.Transform/IDENTITY)
  ;(.rotate (rootnode) 0 timediff 0)
  )

;; PointLight lamp_light = new PointLight();
;; lamp_light.setColor(ColorRGBA.Yellow);
;; lamp_light.setRadius(4f);
;; lamp_light.setPosition(new Vector3f(lamp_geo.getLocalTranslation()));
;; rootNode.addLight(lamp_light);
  
  ;; /** create a red box straight above the blue one at (1,3,1) */
  ;; Box box2 = new Box( new Vector3f(1,3,1), 1,1,1) ;
  ;; Geometry red = new Geometry("Box", box2)        ;
  ;; Material mat2 = new Material(assetManager, 
  ;;                              "Common/MatDefs/Misc/Unshaded.j3md") ;
  ;; mat2.setColor("Color", ColorRGBA.Red)                             ;
  ;; red.setMaterial(mat2)                                             ;



;; /** Create a pivot node at (0,0,0) and attach it to the root node */
;; Node pivot = new Node("pivot");
;; rootNode.attachChild(pivot); // put this node in the scene

;; /** Attach the two boxes to the *pivot* node. */
;; pivot.attachChild(blue);
;; pivot.attachChild(red);
;; /** Rotate the pivot node: Note that both boxes have rotated! */
;; pivot.rotate(.4f,.4f,0f);