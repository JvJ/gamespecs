(ns gamespecs.input
  (:require [clojure.algo.generic.functor :as ft])
  (:import (com.badlogic.gdx Input$Keys)))

(def keys-map
  {Input$Keys/A \A
   Input$Keys/B \B
   Input$Keys/C \C
   Input$Keys/D \D
   Input$Keys/E \E
   Input$Keys/F \F
   Input$Keys/G \G
   Input$Keys/H \H
   Input$Keys/I \I
   Input$Keys/J \J
   Input$Keys/K \K
   Input$Keys/L \L
   Input$Keys/M \M
   Input$Keys/N \N
   Input$Keys/O \O
   Input$Keys/P \P
   Input$Keys/Q \Q
   Input$Keys/R \R
   Input$Keys/S \S
   Input$Keys/T \T
   Input$Keys/U \U
   Input$Keys/V \V
   Input$Keys/W \W
   Input$Keys/X \X
   Input$Keys/Y \Y
   Input$Keys/Z \Z
   Input$Keys/LEFT :Left
   Input$Keys/RIGHT :Right
   Input$Keys/UP :Up
   Input$Keys/DOWN :Down
   Input$Keys/SPACE :Space
   Input$Keys/ESCAPE :Esc
   Input$Keys/CONTROL_LEFT :Ctrl-L
   Input$Keys/CONTROL_RIGHT :Ctrl-R
   Input$Keys/ALT_LEFT :Alt-L
   Input$Keys/ALT_RIGHT :Alt-R})

(def default-input-state
  {:pressed #{}
   :released #{}
   :held {}})

(defn press-key
  "Updates the application's app state when a key is pressed."
  [app-state k-code]
  (let [k (keys-map k-code)]
    (-> app-state
        (update-in [:input-state :pressed]
                   conj k)
        (update-in [:input-state :held]
                   assoc k 0))))

(defn release-key
  "Release a key in the app state."
  [app-state k-code]
  (let [k (keys-map k-code)]
    (-> app-state
        (update-in [:input-state :pressed]
                   disj k)
        (update-in [:input-state :held]
                   dissoc k))))

(defn update-keys
  "Update the keys in the app state."
  [app-state dt]
  (-> app-state
      (update-in [:input-state :pressed] empty)
      (update-in [:input-state :released] empty)
      (update-in [:input-state :held] #(ft/fmap (partial + dt) %))))

(defn keys-pressed
  "Gets the keys pressed in the app state."
  ([app-state] (get-in app-state [:input-state :pressed]))
  ([app-state k] (get-in app-state [:input-state :pressed k])))

(defn keys-held
  "Gets the keys held in the app state."
  ([app-state] (get-in app-state [:input-state :held]))
  ([app-state k] (get-in app-state [:input-state :held k])))

(defn keys-released
  "Gets the keys released in the app state."
  ([app-state] (get-in app-state [:input-state :released]))
  ([app-state k] (get-in app-state [:input-state :released k])))

