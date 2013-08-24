(ns gamespecs.physics
  (:require [simplecs.core :as scs]
            [gamespecs.app :as app])
  (:import (com.badlogic.gdx.physics.box2d World
                                           Box2DDebugRenderer)
           (com.badlogic.gdx.math Vector2)))

(def velocity-iterations 10)
(def position-iterations 10)

(defn world-from-spec
  "Returns a function that creates a world based on the specification.
 This function will be called to create a world when the application
starts or is reset."
  [{:keys [gravity]}]
  (fn [] 
    (World. gravity true)))



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
      (.step w (:delta-time app/*app-state*)
             velocity-iterations position-iterations)
      w)))

(scs/defcomponentsystem physics-debug-draw :physics-world
  "Renders a debug view of the physical world."
  [dbr]
  [ces ent cmp]
  (scs/letc ces ent
            [world [:physics-world :world]]
            (if (instance? World world)
              ))
  ces)
