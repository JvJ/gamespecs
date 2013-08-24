(ns gamespecs.app-test
  (:require [gamespecs.core :as gs]
            [gamespecs.apps.lwjgl :as lw]))

(defn app-reset-test
  []
  (let [test-ces (gs/make-ces {:entities [[(gs/background-color [#f,0])]]
                               :systems [(gs/bg-color-draw-update 0 20 [#f,1 #f,0 #f,1 #f,1])]})]
    (gs/app-with-state :backend lw/LWJGL
                       :ces test-ces
                       :title "Timer Test"
                       :display-settings {:display-res [640 480]})))


(defn test-dbg-opts
  []
  (gs/set-debug-options!
   :delta-time
   :pre-update
   :update-keys
   :verbose)
  (gs/set-debug-file! "outfile.log"))
