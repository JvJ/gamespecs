(ns gamespecs.physics-test
  (:require [gamespecs.core :as gs]
            [gamespecs.physics :as ph]
            [gamespecs.shapes :as sh]
            [gamespecs.apps.lwjgl :as lw]
            [clojure.walk :as wk])
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
        
        tri (PolygonShape.)
        _ (. tri (set (float-array [0,15 -5,0 5,0])))
        
        triFixDef (FixtureDef.)
        _ (set! (. triFixDef shape) tri)
        _ (set! (. triFixDef density) #f,0.5)

        fixture (. body (createFixture fixtureDef))
        triFix (. body (createFixture triFixDef))
        _ (. circle (dispose))
        _ (. tri (dispose))

        ;; The box on the ground
        groundBodyDef (BodyDef.)
        _ (.. groundBodyDef position (set #v2(0,10)))
        groundBody (. w (createBody groundBodyDef))
        groundBox (PolygonShape.)
        _ (. groundBox (setAsBox #f,200 #f,10))
        _ (. groundBody (createFixture groundBox #f,0))
        _ (. groundBox (dispose))
        ]
    w))

(defn test-shape []
  (sh/fix {:transforms [(sh/scale 20)]
           :shape (sh/circ :align :bottom)}
          (sh/fix {:transforms [(sh/rotate -90)
                                (sh/translate #v2(0,0))]
                   :shape (sh/rect 3 :align :left)}
                  #_(sh/fix {:transforms [(sh/translate #v2(0,2))]
                           :shape (sh/eq-tri)})
                  (sh/fix {:transforms [(sh/rotate 45)
                                        (sh/scale 0.5)]
                           :shape (sh/rect 5 :align :left)})
                  (sh/fix {:transforms [(sh/rotate -45)
                                        (sh/scale 0.5)]
                           :shape (sh/rect 5 :align :left)})
                  (sh/fix {:transforms [(sh/translate #v2(3,0))
                                        (sh/rotate 45)
                                        (sh/scale 0.5)]
                           :shape (sh/rect 5 :align :left)})
                  (sh/fix {:transforms [(sh/translate #v2(3,0))
                                        (sh/rotate -45)
                                        (sh/scale 0.5)]
                           :shape (sh/rect 5 :align :left)}))))

(defn make-bodies-2
  "Create shapes with the shapes library."
  [w]
  (let [b (BodyDef.)
        _ (set! (. b type) BodyDef$BodyType/DynamicBody)
        _ (.. b position (set #v2(-10,150)))
        bd (. w (createBody b))
        ts (test-shape)
        fxs (sh/create-fixtures bd ts)
        
        _ (println "Meta shapes: " (:shapes (meta ts)))
        _ (println "Meta matrices: "
                   (wk/prewalk #(if (sequential? %)
                                  (:matrix (meta %))
                                  nil)
                               fxs))
        ;; The box on the ground
        groundBodyDef (BodyDef.)
        _ (.. groundBodyDef position (set #v2(0,10)))
        groundBody (. w (createBody groundBodyDef))
        groundBox (PolygonShape.)
        _ (. groundBox (setAsBox #f,200 #f,10))
        _ (. groundBody (createFixture groundBox #f,0))
        _ (. groundBox (dispose))]
    w))

(defn physics-reset-test
  []
  (let [test-ces
        (gs/make-ces
         {:entities [[(ph/physics-world
                       :gravity #v2(0,-9.8)
                       :init-funcs [make-bodies-2])]]
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
   :advance-ces
   :pre-update
   :update-keys
   :reset
   :verbose))
