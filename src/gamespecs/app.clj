(ns gamespecs.app
  
  "This namespace provides functions which assist in the creation of apps
for LibGDX, and which are integrated with the rest of the gamespecs API."

  (:gen-class)

  (:require [clojure.java.io :as io]
            [clojure.set :as st]
            [gamespecs.input :as inp]
            [gamespecs.files :as files]
            [gamespecs.animations :as animations]
            [simplecs.core :as scs]
            [clojure.contrib.map-utils :as mu]))


;;;; App debug print options

(def ^:dynamic *lwjgl-debug*
  (atom #{}))

(defn set-debug-options!
  [& r]
  (apply swap! *lwj-debug* conj r))

(defn unset-debug-options!
  [& r]
  (apply swap! *lwj-debug* disj r))

(defmacro dbg-do
  "If all the keys are in *lwj-debug*"
  [keys & r]
  `(when (every? @*lwj-debug* ~keys)
     ~@r))

(defmacro error-print-block
  "Wraps a statement in a try/catch block and prints the
exception if it occurs." 
  [& exps]
  `(try
     (do ~@exps)
     (catch Exception e#
       (binding [*out* *out*]
         (println "Error with " '~exps ": " e#)))))


;;;; Utility functions
(defmulti delta-time :backend)

;;;; 
(defn default-app-state
  "Returns a default application state as a map, which includes the
following fields:
:input-states -> keys pressed, keys held, keys released

The user may provide additional map entries as key-value pairs in the
arguments.  (i.e. (default-app-state :a 1 :b 2 :c 3))
Key-value pairs provided in the arguments will override the defaults."
  [& {:as kvps}]
  
  (scs/make-ces
   (merge-with
    
    ;; A super-special merge function! 
    (fn [v1 v2]
      (cond
       ;; Two sequences
       (and (seq? v1)
            (seq? v2))
       (concat v1 v2)

       ;; Two vectors
       (and (vector? v1)
            (vector? v2))
       (vec (concat v1 v2))

       ;; Two sets
       (and (set? v1)
            (set? v2))
       (st/union v1 v2)

       ;; Two maps
       (and (map? v1)
            (map? v2))
       (merge v1 v2)

       ;; Other: just select the second argument
       :else
       v2))
    
    {:input-state {:held {}
                   :pressed #{}
                   :released #{}}
     :game-state {}
     :entities []
     :systems []
     :delta-time 0
     :total-time 0 }
    kvps)))


(def current-app
  "The current application, as created by engine-state."
  (atom nil))

(defmulti engine-app
  "Start an application adapter with an app state and function hooks.
Function hooks are sequences of functions of the form:
app-state -> app-state.

They are applied in the following order:
Start of app : startup
Update cycle : pre-update, pre-render, post-update, post-render
Pause : paused
Dispose (end of app) : disposed

Make sure that each function always accepts and returns a valid game state.
If your function is just there for side effects, then return an un-modified
state."
  (fn [a & _] (:backend a)))
  
(defmulti start-app-multi
  "Override this to provide your own implementation of start-app."
  :backend)

(defn start-app
  "Start an application based on an initial configuration.
  Keys with * are necessary.
  width ; window width
  height ; window height 
  title*
  app-state* ;;ces state
  display-settings : {:display-res [w h]
                      :forced-res [w h]
                      :offset [x y]
                      :zoom [zx zy]} ;; zoom out < 1 < zoom in
  hooks : Function hooks.  See doc for gamespecs.app/engine-app."
  [m]
  (reset! current-app (start-app-multi m)))
  

;;;; Application Utility Functions
;;;; App utility functions
(defn display-settings-gen
  ""
  [m [width height]]
  (merge 
   {:display-res [width height]
    :forced-res [width height]
    :offset [0 0]
    :zoom (if (:forced-res m)
            (let [[rx ry] (:forced-res m)]
              [(/ width rx) (/ height ry)])
            [1 1])}
   m))

(defn app-state-merge
  "Apply a deep merge to the application state and associate
 the display settings under :display-settings."
  [as ds]
  (mu/deep-merge-with
   #(-> %2)
   {:input-state inp/default-input-state
    :display-settings ds}
   as))

;;;; App control functions
(defn reset-key-combo
  "Determines whether or not the reset key combo has been pressed."
  [app-state]
  (and (or (inp/keys-held app-state :Ctrl-L)
           (inp/keys-held app-state :Ctrl-R))
       (inp/keys-pressed app-state \R)))
