(ns gamespecs.app
  
  "This namespace provides functions which assist in the creation of apps
for LibGDX, and which are integrated with the rest of the gamespecs API."

  (:gen-class)

  (:require [clojure.stacktrace]
            [clojure.java.io :as io]
            [clojure.set :as st]
            [gamespecs.input :as inp]
            [gamespecs.files :as files]
            [gamespecs.animations :as animations]
            [simplecs.core :as scs]
            [clojure.contrib.map-utils :as mu]
            [clojure.algo.generic.functor :as ft]))


;;;; App debug print options

(def ^:dynamic *app-debug*
  (atom #{}))

(def ^:dynamic *dbg-stream*
  (atom *out*))

(defn set-debug-options!
  [& r]
  (apply swap! *app-debug* conj r))

(defn unset-debug-options!
  [& r]
  (apply swap! *app-debug* disj r))

(defn set-debug-file!
  [f]
  (reset! *dbg-stream* (io/writer f)))

(defn set-debug-stream!
  [f]
  (reset! *dbg-stream* f))

(defmacro dbg-do
  "If all the keys are in *lwj-debug*"
  [keys & r]
  `(when (every? @*app-debug* ~keys)
     ~@r))

(defmacro error-print-block
  "Wraps a statement in a try/catch block and prints the
exception if it occurs." 
  [keys & exps]
  `(try
     (do ~@exps)
     (catch Exception e#
       (dbg-do
        ~keys
        (binding [*out* @*dbg-stream*]
          (println "Error with " '~exps ": " e#)
          (dbg-do
           [:verbose]
           (clojure.stacktrace/print-stack-trace e#)))))))


;;;; Application Utility Functions

;;;; App control functions
(defn reset-key-combo
  "Determines whether or not the reset key combo has been pressed."
  [app-state]
  (and (or (inp/keys-held app-state :Ctrl-L)
           (inp/keys-held app-state :Ctrl-R))
       (inp/keys-pressed app-state \R)))


(def ^:dynamic *app-state*
  "Dynamically re-bound to the entire app state.  The ces will
 be the under the :ces key of this map, but keep in mind it will
 represent the ces as it was *before* the update cycle started."
  nil)

;;;; 
(defn make-app-state
  "Returns a default application state as a map, which includes the
 following fields:
 Fields with * are necessary.  Others will 
 
 *:ces -> The component entity system.
 *:title -> Application window title
 *:backend -> What kind of application is it?
 :display-settings* -> A map, not all keys are necessary.
                     {:display-res [w h]* -> Width and height of window
                      :forced-res [w h] -> Forced lower resolution
                      :offset [x y] -> Display offset
                      :zoom [zx zy] -> X and y display scale}
 :hooks -> A map of sequences of functions.  Each should take
  an app state and return an app state.
  They are applied in the following order:
  Start of app : startup
  Update cycle : pre-update, pre-render, post-update, post-render
  Pause : paused
  Dispose (end of app) : disposed

 The user may provide additional map entries as key-value pairs in the
 arguments.  (i.e. (default-app-state :a 1 :b 2 :c 3))
 Key-value pairs provided in the arguments will override the defaults."
  [& {:keys [ces
             title
             backend]
      {[width height :as display-res] :display-res
       [rx ry :as forced-res] :forced-res
       [ox oy :as offset] :offset
       :as display-setings} :display-settings
      {:as hooks} :hooks
      :as kvps}]
  ;; Ensure all keys are provided
  {:pre [ces
         title
         backend
         width height]}
  
  {:ces ces
   :title title
   :backend backend
   :display-settings {:display-res display-res
                      :forced-res (or (and rx ry forced-res)
                                      display-res)
                      :offset (or (and ox oy offset)
                                  [0 0])
                      :zoom (if (and rx ry)
                              [(/ width rx) (/ height ry)]
                              [1 1])}
   :input-state {:held {}
                 :pressed #{}
                 :released #{}}
   :delta-time 0
   :total-time 0
   :hooks (ft/fmap #(apply comp (reverse %))
                   (merge {:startup nil
                           :pre-update nil
                           :pre-render nil
                           :post-update nil
                           :post-render nil
                           :paused nil
                           :disposed nil}
                          hooks))})

(def current-app
  "The current application."
  (atom nil))

(def current-app-adapter
  (atom nil))

(defmulti engine-app
  "Start an application adapter with an app state and function hooks.
Function hooks are sequences of functions of the form:
app-state -> app-state.

Make sure that each function always accepts and returns a valid game state.
If your function is just there for side effects, then return an un-modified
state."
  (fn [a & _] (:backend a)))
  
(defmulti start-app-multi
  "Override this to provide your own implementation of start-app."
  :backend)

(defn start-app
  "Start an application based on an initial configuration.  The
 configuration should be created with make-app-state."
  [{{[width height :as display-res] :display-res
     :as display-settings} :display-settings
     :as m}]
  (if-not @current-app
    ;; Starting a new application
    (reset! current-app (start-app-multi m))
    ;; Re-using an application that already exists
    (do (println  "Here is m this time hahaha.")
        (.reset @current-app-adapter m)
        ;; Looks like resizing can't happen
        ;;(.resize @current-app width height)
        current-app)))

(def app-with-state
  "Convenience function.  Composition of start-app and make-app-state."
  (comp start-app make-app-state))
