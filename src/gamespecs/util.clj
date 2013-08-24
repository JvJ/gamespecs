(ns gamespecs.util

  "Utility functions, including state monad functions for
dealing with the ECS.

State monad functions expect the following state format:
 [ces entity component], as a single parameter.
"


  (:use [simplecs.core]
        [clojure.algo.monads])
  (:import (com.badlogic.gdx.physics.box2d World)
           (com.badlogic.gdx.math Vector2
                                  Vector3)))


;;;; State Monad Functions

(defn f-args
  "Create an arguments list!"
  [& items]
  (with-meta (apply list items)
    {:args true}))

(defn f-args?
  "Check if it's an args list."
  [a]
  (get-in (meta a) [:args]))

(def nop
  "Take no action."
  (fn [ [ces entity component] ]
    [nil
     [ces entity component]]))


(defn e-this
  "Sate monad function.  Returns this entity."
  []
  (fn [ [ces entity component] ]
    [ entity
     [ces entity component]]))

(defn e-get
  "State-monad function.  Wrapper for get-in-entity.
When given a path, it looks up a field in 'this' entity.
Otherwise, you provide an entity and a path."
  ([path] (fn [ [ces entity component :as s] ]
            [ (get-in-entity ces entity path)
              s ]))
  ([ent path]
     (fn [ [ces entity component :as s] ]
       [ (get-in-entity ces ent path)
         s ]
       )))

(defn e-upd
  "State monad function.  Update a field in an entity.  Semantics are
similar to update-entity, but args are provided as an explicit seq rather
than a rest argument."
  ([keyword-or-list f args]
     {:pre [(f-args? args)]}
     (fn [[ces entity component :as s]]
       [ nil
        [(apply update-entity ces entity keyword-or-list f args) entity component] ]
       )
     )
  ([ent keyword-or-list f args]
     {:pre [(f-args? args)]}
     (fn [[ces entity component :as s]]
       [ nil
        [(apply update-entity ces ent keyword-or-list f args) entity component] ]
       )
     ))

(defn e-set
  "State monad function.  Set a field in an entity to a value."
  ([keyword-or-list val]
     (e-upd keyword-or-list (constantly val) (f-args))
     )
  ([ent keyword-or-list val]
     (e-upd ent keyword-or-list (constantly val) (f-args))
     ))

(defn e-remove
  "State monad function.  Remove an entity."
  [ent]
  (fn [[ecs ent cmp]]
    [nil
     [(remove-entity ecs ent) ent cmp]]))

(defn ecs-upd
  "State monad function.  Update a field in the ces."
  ([path f args]
     {:pre [(f-args? args)]}
     (fn [[ces e c :as s]]
       [ nil
        [(apply update-in ces path f args) e c] ])))

(defn ecs-set
  "State monad function.  Set a field in the ces."
  ([path val]
     (fn [[ces e c :as s]]
       [ nil
        [(assoc-in ces path val) e c] ])
     ))

(defn ecs-get
  "State monad function.  Looks up the given path."
  ([path]
     (ecs-get path nil))
  ([path not-found]
     (fn [[ces e c :as s]]
       [(get-in ces path not-found) s])))

(defn st-cmp
  "Sync's the state's current component and returns it."
  []
  (fn [[ecs ent cmp]]
    (let [cmp (get-component ent (:name cmp))]
      [cmp
       [ecs ent cmp]])))

(defn st-ecs
  "Return the state's ecs."
  []
  (fn [[ecs ent cmp]]
    [ecs
     [ecs ent cmp]]))

(defn st-ent
  "Returns the state's entity."
  []
  (fn [[ecs ent cmp]]
    [ent
     [ecs ent cmp]]))

(defmacro do-ecs
  "Use the state monad to modify the ecs."
  [ecs entity component & r]
  `(first
    (second
     ((domonad state-m
               [~@r]
               nil)
      ~[ecs entity component]))))

(defmacro do-state
  "Create a new domonad block inside of another domonad block."
  [bindings & r]
  `(fn [st#]
     ((domonad state-m
               ~bindings
               ~@(or r [nil])
               ) st#)))
             


(defn ancestor-trace
  "Recursively gets the ancestors, in bottom-up order, of k"
  [k]
  (let [a (parents k)]
    (if (empty? a)
      (list k)
      (lazy-seq
       (cons k (ancestor-trace (first a)))))))

;;;; Other functions
(defn q
  "Put a vector in this."
  ([sq]
     `(reduce conj
              clojure.lang.PersistentQueue/EMPTY
              ~sq)))

(defn queue
  "Construct a queue."
  [& r]
  (reduce conj
          clojure.lang.PersistentQueue/EMPTY
          r))


;;;; Vector operations

(defn v+
  [& r]
  (vec (apply map + r)))

(defn pv+
  "Partial vector add."
  [& r]
  (apply partial v+ r))

(defn v-
  [& r]
  (vec (apply map - r)))

(defn pv-
  "Partial vector minus.
 ((pv- a) b) is (v- a b)"
  [& r]
  (apply partial v- r))

(defn pv-sub
  "Partial vector subtract."
  [& r]
  (fn [a]
    (apply - a r)))  

(defn lerp
  "Interpolate a factor of f between vectors a and b."
  [f a b]
  (if-not (= (count a) (count b))
    (throw (Exception. "Interpolate requires equal sized vectors.")))
  (->> (mapv (comp #(* f %) -) b a)
       (mapv + a)))
         
          
(defn lerp-factor
  "Map the interval [lo hi] to [0 1] place mid accordingly."
  [mid lo hi]
  (let [interval (- hi lo)]
    (/ (- mid lo) interval)))

(defn clamp
  "Clamp n into the interval [lo hi]."
  [n lo hi]
  (let [[lo hi] (if (< hi lo)
                  [hi lo]
                  [lo hi])]
    (cond
     (< n lo) lo
     (> n hi) hi
     :else n)))
  

;;; Colors and graphics
(defn clr-lerp
  "Interpolation for RGB or RGBA colors."
  [factor a b]
  (map float (lerp factor a b))) 
  
  
;;;; Reader macro functions
(defn make-float
  [x]
  (if (seq? x)
    `(float ~@x)
    `(float ~x)))

(defn b2vec2
  [[x y]]
  `(Vector2. (float ~x) (float ~y)))

(defn b2vec3
  [[x y z]]
  `(Vector3. (float ~x) (float ~y) (float ~z)))
