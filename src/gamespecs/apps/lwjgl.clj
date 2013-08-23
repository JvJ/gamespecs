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
  [{:as astate}]
  
  (let [init-astate (atom astate)
        astate-atom (atom astate)]
    
    (proxy
        [GamespecsApplicationAdapter] []
      
      ;;; This method creates the app
      (create
        []

        ;; We need to set the input processor to work with
        ;; gamespecs
        (.setInputProcessor
         Gdx/input
         (proxy [InputAdapter] []
           (keyDown [keycode]
             (swap! astate-atom inp/press-key keycode)
             true)
           (keyUp [keycode]
             (swap! astate-atom inp/release-key keycode)
             true)))
        
        ;; Next, apply the startup function
        (app/error-print-block [:startup]
                               (swap! astate-atom (:startup @astate-atom))))
      
      
      ;; The rendering/update hooks
      (render
        []
        (let [{{:keys [pre-update
                       pre-render
                       post-render
                       post-update]} :hooks} @astate-atom]
          (println "Starting render cycle")

          ;; Reset the app if we hit the keys
          (if (app/reset-key-combo @astate-atom)
            (.reset this))
            
          (app/error-print-block
           [:delta-time]
           (let [dt  (.getDeltaTime Gdx/graphics)]
             (swap! astate-atom assoc :delta-time dt)
             (swap! astate-atom update-in [:total-time] + dt)))
          
          ;; Hooks
          (app/error-print-block
           [:pre-update]
           (swap! astate-atom pre-update))
          (app/error-print-block
           [:pre-render]
           (swap! astate-atom pre-render))
          ;; Main update
          (app/error-print-block
           [:advance-ces]
           (binding [app/*app-state* @astate-atom]
             (swap! astate-atom update-in [:ces] scs/advance-ces)))
          ;; Hooks again
          (app/error-print-block
           [:post-render]
           (swap! astate-atom post-render))
          (app/error-print-block
           [:post-update]
           (swap! astate-atom post-update))
          ;; Input update
          (app/error-print-block
           [:update-keys]
           (swap! astate-atom inp/update-keys (:delta-time @astate-atom)))))

      ;; Pausing
      (pause
        []
        (app/error-print-block
         [:paused]
         (swap! astate-atom (:paused @astate-atom))))
      
      ;; Disposing!
      (dispose
        []
        (app/error-print-block
         [:disposed]
         (swap! astate-atom (:disposed @astate-atom))))
      
      (getInitialState []
        astate)
      
      (getCurrentState []
        @astate-atom)
      
      (reset
        ([]
           (reset! astate-atom @init-astate))
        ([st]
           (reset! astate-atom
                   (reset! init-astate st)))))))
  
(defmethod app/start-app-multi ::LWJGL
  [{{[width height] :display-res} :display-settings
    title :title
    :as m}]

  (let [_ (clojure.pprint/pprint m)
        
        ;; App Configuration
        cfg (LwjglApplicationConfiguration.)
        ;; Variable options
        _ (set! (. cfg title) title)
        _ (set! (. cfg width) width)
        _ (set! (. cfg height) height)
        ;; Constant options
        _ (set! (. cfg useGL20) false)
        _ (set! (. cfg vSyncEnabled) true)]
    (LwjglApplication.
     (reset! app/current-app-adapter
             (app/engine-app m))
     cfg)))
