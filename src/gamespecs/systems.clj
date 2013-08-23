(ns gamespecs.systems
  "This namespace provides some common, useful
 systems and component systems."
  (:require [simplecs.core :as sc]
            [gamespecs.util :as util]
            [gamespecs.app :as app])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics GL10)))


(sc/defsystem color-clearer
  [[r g b]]
  [ces]
  (.glClearColor Gdx/gl r g b #f(1.0))
  (.glClear Gdx/gl GL10/GL_COLOR_BUFFER_BIT)
  ces)


(sc/defcomponent background-color
  "Create a background color component.  I
 suggest only one of these.  Must pass 1, 3
 or 4 args.
 1 arg: a shade of gray as specified by the one number.
 3 args: RGB color with full opacity
 4 args: RGBA color"
  [[r g b a?]]
  (let [color (cond
               (and r g b a?) [#f,r #f,g #f,b #f,a?]
               (and r g b)    [#f,r #f,g #f,b #f,1.0]
               (and r (not g)
                    (not b))
               [#f,r #f,r #f,r #f,1.0]
               :else (throw (Exception.
                             "Color requires 1, 3, or 4 params.")))]
    {:color color
     :init-color color
     :time 0}))

(sc/defcomponentsystem bg-color-draw-update :background-color
  "Updates and draws the background color.
 Params : 
 tmin : Before this time, the background color will stay as it is.
 tmax : Between tmin and tmax, the background color wil lerp towards
 end-clr.
 end-clr : The ending color.
 "
  [tmin tmax end-clr]
  [ces ent cmp]
  (let [lf (util/clamp (util/lerp-factor (:total-time app/*app-state*) tmin tmax) 0 1)
        [r g b a] (:color cmp)]
    (.glClearColor Gdx/gl r g b a)
    (.glClear Gdx/gl GL10/GL_COLOR_BUFFER_BIT)
    
    (sc/update-entity ces ent
                      [:background-color :color]
                      (constantly
                       (util/clr-lerp lf (:init-color cmp) end-clr)))))
    
                                     
                    
                    
