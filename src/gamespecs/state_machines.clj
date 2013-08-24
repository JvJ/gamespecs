(ns gamespecs.state-machines
  (:require [simplecs.core :as scs]
            [gamespecs.util :as util]
            [gamespecs.input :as inp]))
  

(defcomponent state-machine 
  "This represents state-machines.  Default is the starting state.
Transitions are mappings between states and update functions.

Each state has a unique update function, which is executed whenever
an entity is in that state.

The functions should be of the form [ecs ent cmp] -> cmp
"
  [default &{:as updates}]
  {:state default
   :default default
   :states (set (keys updates))
   :updates updates})


(defcomponentsystem state-machine-update :state-machine
  []
  [ecs ent cmp]

  (try
    ( ( (:updates cmp) (:state cmp) )
      ecs ent cmp)
    (catch Exception e
      (throw (Exception.
              (str "State update error. State: " (:state cmp)
                   "Entity: " ent
                   "Exception: " e))))))


;;;; Utility functions for finite state machines
(defn switch-state
  [new-state ces entity component]
  (update-entity ces entity
                 [:state-machine :state]
                 (constantly new-state)))

(defn switch-state-m
  "Monadic function for state switching."
  ([new-state]
     (fn [[ecs ent cmp]]
       [nil [(switch-state
              new-state ecs ent)
             ent
             cmp]]))
  ([ent new-state]
     (fn [[ecs ent2 cmp]]
       [nil [(switch-state
              new-state ecs ent)
             ent
             cmp]])))
