(ns gamespecs.physics-test
  (:require [gamespecs.core :as gs]
            [gamespecs.physics :as ph]
            [gamespecs.apps.lwjgl :as lw])
  (:import (com.badlogic.gdx.physics.box2d Box2DDebugRenderer
                                           BodyDef
                                           BodyDef$BodyType
                                           Body
                                           FixtureDef
                                           
                                           PolygonShape
                                           CircleShape)))

(defn make-bodies
  "A testing function that makes a floor and some other crap."
  [w]
  (let [;; A bouncing ball
        bodyDef (BodyDef.)
        _ (set! (. bodyDef type) BodyDef$BodyType/DynamicBody)
        _ (.. bodyDef position (set #v2(100,150)))
        body (. w (createBody bodyDef))
        circle (CircleShape.)
        _ (. circle (setRadius #f,6))
        fixtureDef (FixtureDef.)
        _ (set! (. fixtureDef shape) circle)
        _ (set! (. fixtureDef density) #f,0.5)
        _ (set! (. fixtureDef friction) #f,0.4)
        _ (set! (. fixtureDef restitution) #f,0.6)
        fixture (. body (createFixture fixtureDef))
        _ (. circle (dispose))

        ;; THe box on the ground
        groundBodyDef (BodyDef.)
        _ (.. groundBodyDef position (set #v2(0,10)))
        groundBody (. w (createBody groundBodyDef))
        groundBox (PolygonShape.)
        _ (. groundBox (setAsBox #f,200 #f,10))
        _ (. groundBody (createFixture groundBox #f,0))
        _ (. groundBox (dispose))
        ]
    w))

(defn physics-reset-test
  []
  (let [test-ces
        (gs/make-ces
         {:entities [[(ph/physics-world
                       :gravity #v2(0,-9.8)
                       :init-funcs [make-bodies])]]
          :systems [(ph/physics-world-update)
                    (gs/color-clearer [ 0 0 0 ])
                    (ph/physics-debug-draw)]})]
    (gs/app-with-state :backend lw/LWJGL
                       :ces test-ces
                       :title "Physics Test"
                       :display-settings {:display-res [640 480]}
                       :hooks {:reset [lw/make-camera
                                       ph/make-debug-renderer]})))
                       
(defn test-dbg-opts
  []
  (gs/set-debug-options!
   :delta-time
   :pre-update
   :update-keys
   :reset
   :verbose)
  (gs/set-debug-file! "phys_outfile.log"))
