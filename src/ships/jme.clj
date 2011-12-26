(ns ships.jme
  "Abstraction for the jMonkeyEngine Basic Geometries"
  (:require [ships.state :as state])
  (:import (com.jme3.scene.shape Box)
           (com.jme3.scene Geometry)
           (com.jme3.material Material)
           (com.jme3.texture Texture2D Texture Texture3D)
           (com.jme3.math Vector3f Vector2f Vector4f Plane Quaternion FastMath Matrix4f)
           (com.jme3.math ColorRGBA)
           (com.jme3.shader VarType)))

;; I'd like to pull these functions out into seperate files but am busy
;; seeing what makes logical sense.

;; ========= UTIL FUNCTIONS ================================================
(defmacro map-enums [enumclass]
  `(apply merge (map #(hash-map (keyword (.name %)) %) (~(symbol (apply str (name enumclass) "/values"))))))

(defn transform-to-hashmap
  "Takes a list and transforms it into a hashmap based on keyfn and valfn"
  [list keyfn valfn]
  (let [keys (map keyfn list)
        values (map valfn list)]
    (zipmap keys values)))

;; ========= MATH FUNCTIONS =================================================


(defn vec4f
  "Wrapper function for generating a Vector4f"
  [[x y z a]])

(defn vec3f
  "Wrapper function for generating a Vector3f"
  [[x y z]]
  (Vector3f. x y z))

(defn vec2f
  "Wrapper function for generating a Vector2f"
  [[x y]]
  (Vector2f. x y))

;; ========= MATERIAL FUNCTIONS =============================================
(def vartypes (map-enums VarType))

(defn- make-material-key
  "Convert a material entry into a key value"
  [material]
  (-> material
      (.replaceAll "(Misc/|Gui/|Nifty/|SSAO/|Water/|/)" "")
      (.replaceAll "[A-Z]" "-$0")
      (.toLowerCase)
      (.substring 1)
      keyword))


(defn import-materials
  "Import a material list, binding it to sensible key => filename pairs "
  [matlist]
  (transform-to-hashmap matlist make-material-key #(str "Common/MatDefs/" % ".j3md")))


(def materials
  (import-materials
   ["Blur/HGaussianBlur" "Blur/VGaussianBlur" "Blur/RadialBlur"
    "Gui/Gui"
    "Hdr/LogLum" "Hdr/ToneMap"
    "Light/Lighting" "Light/Deferred"
    
    "Misc/ColoredTextured" "Misc/Particle" "Misc/ShowNormals" "Misc/SimpleTextured"
    "Misc/Sky" "Misc/SolidColor" "Misc/Unshaded" "Misc/VertexColor" "Misc/WireColor"
    
    "Nifty/Nifty"
    
    "Post/BloomExtract" "Post/BloomFinal" "Post/CartoonEdge" "Post/CrossHatch"
    "Post/DepthOfField" "Post/FXAA" "Post/Fade" "Post/Fog" "Post/GammaCorrection"
    "Post/LightScattering" "Post/Overlay" "Post/Posterization"

    "SSAO/ssao" "SSAO/ssaoBlur"

    "Shadow/PostShadow" "Shadow/PostShadowPSSM" "Shadow/PreShadow"

    "Terrain/HeightBasedTerrain" "Terrain/Terrain" "Terrain/TerrainLighting"

    "Texture3D/tex3D"

    "Water/SimpleWater" "Water/Water"]))


(defmulti mat-set-param
  "A simple wrapper for all the different kinds of material parameters that can be set."
  (fn [mat param val] (class val)))

(defmethod mat-set-param Float [mat param val]
  (.setFloat mat param val))

(defmethod mat-set-param Long [mat param val]
  (.setFloat mat param (float val)))

(defmethod mat-set-param Boolean [mat param val]
  (.setBoolean mat param val))

(defmethod mat-set-param ColorRGBA [mat param val]
  (.setColor mat param val))

(defmethod mat-set-param Integer [mat param val]
  (.setInt mat param val))

(defmethod mat-set-param Matrix4f [mat param val]
  (.setMatrix4 mat param val))

(defmethod mat-set-param Texture2D [mat param val]
  (.setTexture mat param val))

(defmethod mat-set-param Texture3D [mat param val]
  (.setTexture mat param val))

(defmethod mat-set-param Vector2f [mat param val]
  (.setVector2 mat param val))

(defmethod mat-set-param Vector3f [mat param val]
  (.setVector3 mat param val))

(defmethod mat-set-param Vector4f [mat param val]
  (.setVector4 mat param val))


(defn load-material
  "Load a material from the asset manager, optionally loading in a vector of property bindings"
  ([material-key]
     (let [matfile (materials material-key)]
       (Material. (state/asset-manager) matfile)))
  
  ([material-key properties]
     (let [propmap (apply hash-map properties)
           lowerprops (into {} (map #(vector (.toLowerCase (first %)) (second %)) propmap))
           mat (load-material material-key)
           paramset (filter #(contains? lowerprops (-> % .getName .toLowerCase))
                            (seq (-> mat .getMaterialDef .getMaterialParams)))]
       
                                        ; Apply properties
       (doseq [param paramset
               :let [name (.getName param)
                     value (lowerprops (.toLowerCase name))]]
         (mat-set-param mat name value))
                                        ; Finally, return the material.
       mat)))

(defn inspect-material
  "Show a list of properties for a given material. Useful for REPL use."
  [material-key]
  (let [matdef (.loadAsset (state/asset-manager) (materials material-key))
        paramset (sort-by #(str (.name (.getVarType %)) (.getName %)) (seq (.getMaterialParams matdef)))]
    (println (.getAssetName matdef))
    (println "--------------------------------")
    (doseq [param paramset]
      (println (.name (.getVarType param)) (.getName param)))))

(defn set-material
  "Sets a material on a geometry with the provided property map"
  [geometry material-key properties]
  (let [mat (load-material material-key properties)]
    (.setMaterial geometry mat)))

(defn texture
  "Load a texture asset from the asset-manager"
  [texturefile]
  (.loadTexture (state/asset-manager) texturefile))

;; ========= FONT FUNCTIONS ===================================================
(defn import-fonts
  "Import the font list, binding it to sensible key => filename pairs"
  [fontlist]
  (transform-to-hashmap fontlist make-material-key #(str "Interface/Fonts/" % ".fnt")))

(def fonts
  (import-fonts ["Console" "Default"]))


;; ========= GEOMETRY FUNCTIONS =============================================
(defn box
  "Simple box constructor wrapper function - Creates a box centered at location with size"
  ([name location size]
     (box name location size :light-lighting))
  ([name location [sx sy sz] material]
     (com.jme3.scene.Geometry. name (Box. (vec3f location) sx sy sz))))

;; ========= LIGHT FUNCTIONS ================================================
(defn direction-light [location color]
  (doto (com.jme3.light.DirectionalLight.)
    (.setColor color)
    (.setDirection (.normalizeLocal (vec3f location)))))

(defn point-light [location radius color]
  (doto (com.jme3.light.PointLight.)
    (.setColor color)
    (.setRadius radius)
    (.setPosition (vec3f location))))

(defn ambient-light [color]
  (doto (com.jme3.light.AmbientLight.)
    (.setColor color)))

;; ========= WATER FUNCTIONS ================================================
