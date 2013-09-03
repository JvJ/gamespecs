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
    
                                     
;;;; Rendering
(sc/defcomponent renderable
  "Components which are renderable.  Func should take 3 arguments:
  [ces entity component], and should return an updated ces."
  [func]
  {:func func})

(sc/defcomponentsystem
  render :renderable
  "Rendering function for renderable components."
  [] ; Parameters?  Don't think so.
  [ces entity component]

  (let [ret ( (component :func)
              ces entity component )]
    ;; Basically, just execute the function
    ret))

;;;; Timer

(sc/defcomponent timer
  "Countdown timer.  Create a timer with a key path and an event to
trigger on that key path.  It will fire after the count-down.
The trigger function should accept an ecs and event descriptor.
The default trigger just triggers the event.

A timer should be spawned in its own entity!!
"
  ([ecs ks evt count-down]
     (timer ecs count-down
            (fn [ecs evt]
              (evt-trigger ecs ks evt))))
  ([ecs ks evt count-down trigger]
     {:count-down count-down
      :time count-down
      :trigger trigger
      :event evt}
     ))

(defcomponentsystem timer-update :timer
  []
  [ecs ent cmp]
  (do-ecs ecs ent cmp
          
          _ (e-upd [:timer :time] - (ecs :delta-time))

          t (e-get [:timer :time])
          
          _ (if (<= t 0)
              (ecs-upd [] (:trigger cmp) (:event cmp))
              (m-result nil))

          _ (if (<= t 0)
              (e-remove ent)
              (m-result nil))))
