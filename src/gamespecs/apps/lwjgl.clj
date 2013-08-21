(ns gamespecs.apps.lwjgl
  "This namespace provides app functions for lwjgl."
  (:gen-class)
  
  (:require [gamespecs.app :as app]
            [gamespecs.input :as inp]
            [gamespecs.util :as util]
            [simplecs.core :as scs]
            [clojure.contrib.map-utils :as mu]
            [clojure.algo.generic.functor :as ft])


  (:import (com.badlogic.gdx ApplicationAdapter
                             Gdx
                             InputAdapter
                             GamespecsApplicationAdapter)
           (com.badlogic.gdx.graphics GL10
                                      Mesh
                                      OrthographicCamera
                                      Texture)
           (com.badlogic.gdx.graphics VertexAttribute
                                      VertexAttributes
                                      VertexAttributes$Usage)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.backends.lwjgl LwjglApplication
                                            LwjglApplicationConfiguration)))

;;;; Backend keywords
(def LWJGL ::LWJGL)


(defmethod app/engine-app LWJGL
  [astate
   &{:as hooks}]
  {:pre [(every? sequential? (vals hooks))]}
  
  (let [astate-atom (atom astate)
        
        {:keys [startup
                pre-update
                post-update
                pre-render
                post-render
                paused
                disposed]}
        (ft/fmap #(apply comp (reverse %)) (or hooks {}))]
    
    (proxy
     [GamespecsApplicationAdapter] []
     
      ;;; This method creates the app
     (create
      []
       (println "Creation exists!")
       (println "The this: " this)
       ;; We need to set the input processor to work with
       ;; gamespecs
       (.setInputProcessor
        Gdx/input
        (proxy [InputAdapter] []
          (keyDown [keycode]
            (swap! astate-atom update-in [:input-state]
                   inp/press-key keycode)
            true)
          (keyUp [keycode]
            (swap! astate-atom update-in [:input-state]
                   inp/release-key keycode)
            true)))
       
       ;; Next, apply the startup function
       (error-print-block
        (swap! astate-atom startup)))
      
     
     ;; The rendering/update hooks
     (render
       []
       ;; Reset the app if we hit the keys
       ;;(if (app/reset-key-combo @astate-atom)
       ;;  (.reset this))
       
       (error-print-block
        (let [dt  (.getDeltaTime Gdx/graphics)]
          (swap! astate-atom assoc :delta-time dt)
          (swap! astate-atom update-in [:total-time] + dt)))
       (error-print-block
        (swap! astate-atom pre-update))
       (error-print-block
        (swap! astate-atom pre-render))
       (error-print-block
        (swap! astate-atom scs/advance-ces))
       (error-print-block
        (swap! astate-atom post-render))
       (error-print-block
        (swap! astate-atom post-update))
       (error-print-block
        (swap! astate-atom inp/update-keys (:delta-time @astate-atom))))

     ;; Pausing
     (pause
       []
       (error-print-block
        (swap! astate-atom paused)))
     
     ;; Disposing!
     (dispose
       []
       (error-print-block
        (swap! astate-atom disposed)))

     (getInitialState []
       astate)

     (getCurrentState []
       @astate-atom)
     
     (reset []
       (reset! astate-atom astate)))))

(defmethod app/start-app-multi ::LWJGL
  [{:keys [width
           height
           title
           app-state
           display-settings
           hooks
           backend]
    :as m}]

  (let [display-settings (app/display-settings-gen display-settings [width height])
        
        app-state (merge app-state {:backend backend})
        
        ;; Merge with the app-state last, so that
        ;; any pre-existing options overwrite the defaults        
        app-state (app/app-state-merge app-state display-settings)
        
        _ (clojure.pprint/pprint app-state)
        
        ;; App Configuration
        cfg (LwjglApplicationConfiguration.)
        ;; Variable options
        _ (set! (. cfg title) title)
        _ (set! (. cfg width) width)
        _ (set! (. cfg height) height)
        ;; Constant options
        _ (set! (. cfg useGL20) false)
        _ (set! (. cfg vSyncEnabled) true)]
    (LwjglApplication. (apply app/engine-app app-state
                              (apply concat hooks))
                       cfg)))
