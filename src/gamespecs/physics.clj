(ns gamespecs.physics
  (:require [simplecs.core :as scs]
            [gamespecs.app :as app])
  (:import (com.badlogic.gdx Gdx) 
           (com.badlogic.gdx.graphics GL10)
           (com.badlogic.gdx.physics.box2d World
                                           Box2DDebugRenderer)
           (com.badlogic.gdx.math Vector2)))

(def velocity-iterations 10)
(def position-iterations 10)


(def physics-paused-set (app/set-flag-hook :physics-paused-set))
(def physics-paused-clear (app/clear-flag-hook :physics-paused-set))

(defn make-debug-renderer
  "Reset hook that creates a renderer."
  [m]
  (assoc m :debug-renderer (Box2DDebugRenderer.)))

(defn world-from-spec
  "Returns a function that creates a world based on the specification.
 This function will be called to create a world when the application
starts or is reset.
 
Use the following keys:
 :gravity -> the world's gravitational pull
 :init-funcs -> a list of functions to be executed on the world"
  [{:keys [gravity
           init-funcs]}]
  (fn []
    (let [w (World. gravity true)]
      (doseq [f init-funcs]
        (f w))
      w)))

(scs/defcomponent physics-world
  "Uses world-from-spec to create a world.
 If the world is a function, it gets executed and turned into a
 world.  Otherwise, it stays as it is."
  [&{:as m}]
  {:world (world-from-spec m)})

(scs/defcomponentsystem physics-world-update :physics-world
  "Steps the physical world.  If the world is a function, it gets
 executed and turned into a world, then updated."
  []
  [ces ent cmp]
  (scs/update-entity
   ces ent [:physics-world :world]
   #(let [w (if (fn? %) (%) %)]
      (if-not (:physics-paused app/*app-state*)
        (.step w (:delta-time app/*app-state*)
               velocity-iterations position-iterations))
      w)))

(scs/defcomponentsystem physics-debug-draw :physics-world
  "Renders a debug view of the physical world."
  []
  [ces ent cmp]
  
  (scs/letc ces ent
            [world [:physics-world :world]]
            ;; (if (instance? World world)
            ;;   (. Gdx/gl (glClearColor #f,1.0 #f,0.0 #f,1.0 #f,1.0))
            ;;   (. Gdx/gl (glClearColor #f,0.0 #f,1.0 #f,0.0 #f,1.0)))
            ;; (. Gdx/gl (glClear GL10/GL_COLOR_BUFFER_BIT))
            (if (and
                 (instance? World world)
                 (:camera app/*app-state*))
              (try
                (.render (:debug-renderer app/*app-state*)
                         world (.combined (:camera app/*app-state*)))
                (catch Exception e
                  (spit "phys-exception.txt" e)
                  ))))
            
  ces)
