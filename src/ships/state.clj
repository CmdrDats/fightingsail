(ns ships.state)

(defonce rootapp (atom {}))

(defn assoc-to-root! [& items]
  (apply swap! rootapp assoc items))

(defn rootnode []
  (.getRootNode (:app @rootapp)))

(defn asset-manager []
  (.getAssetManager (:app @rootapp)))

(defn input-manager []
  (.getInputManager (:app @rootapp)))

(defn viewport [] (.getViewPort (:app @rootapp)))


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
