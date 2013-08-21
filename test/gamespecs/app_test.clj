(ns gamespecs.app-test
  (:require [gamespecs.core :as gs]
            [gamespecs.apps.lwjgl :as lw]))

(def test-ces-1
  (gs/make-ces {:systems []
                :entities []}))

(defn app-start-test
  "This test ensures that the app opens a window."
  []
  (gs/start-app
   {:backend lw/LWJGL
    :app-state test-ces-1
    :title "App-Test"
    :width 640
    :height 480}))

(defn app-reset-test
  []
  (let [test-ces (gs/default-app-state :entities [[(gs/background-color [0])]]
                                        :systems [#_(gs/bg-color-draw-update 0 60 [#f,1 #f,1 #f,1 #f,1])])]
    (gs/start-app
     {:backend lw/LWJGL
      :app-state test-ces
      :title "Timer Test"
      :width 400
      :height 400})))
