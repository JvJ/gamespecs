(ns gamespecs.shapes
  "A sublanguage for defining shapes."
  (:require [gamespecs.util :as util]
            [clojure.walk :as wk])
  (:import (com.badlogic.gdx.math Matrix3
                                  Vector2
                                  MathUtils)
           (com.badlogic.gdx.physics.box2d World
                                       
                                           Box2DDebugRenderer
                                           
                                           Body
                                           BodyDef
                                           BodyDef$BodyType
                                           
                                           Fixture
                                           FixtureDef
                                           
                                           Shape
                                           CircleShape
                                           PolygonShape)))


(def align-flags #{:center :left :right :top :bottom})

(def ^:dynamic *the-matrix*
  "The current transformation matrix."
  (. (Matrix3.) idt))

;;; Macros for using the matrix
(defmacro with-transforms
  "Binds *the-matrix* to a new matrix, modified by
 the provided transformation operator."
  [ts & r]
  `(binding [*the-matrix* ((apply make-transform ~ts) (Matrix3. *the-matrix*))]
     (do ~@r)))

;;; Transformation operators
(defn scale
  "Scale matrix by a vector or scalar."
  [v]
  {:pre [(or (number? v) (instance? Vector2 v))]}
  (fn [m] (. m (scale
                (cond (number? v) #v2(v,v)
                      (instance? Vector2 v) v)))))

(defn translate
  "Translate matrix by a vector."
  [v]
  (fn [m] (. m (translate v))))

(defn rotate
  "Rotate counterclockwise by an angle in degrees."
  [f]
  (fn [m] (. m (rotate (float f)))))

(defn make-transform
  "Create a composite transformation operation."
  [& ops]
  (apply comp (reverse ops)))
  
(defn transform
  "Apply the current transformation matrix to a vector."
  [v]
  (. v (mul *the-matrix*)))

;;; The primary fixture-making function
(defn fix-fn
  "Returns a list of fixtures which may be added to a
 physical body."
  [;; Main properties
   {:keys [shape
           density
           friction
           restitution]
    :or {density #f,1.0
         friction #f,0.0
         restitution #f,0.0}}
   ;; Child fixtures
   children]
  ;; A shape must certainly be provided
  {:pre [shape]}
  (let [f (FixtureDef.)
        shape (shape) ; Note: shapes must be evaluated as functions
        _ (set! (. f shape) shape) 
        _ (set! (. f density) density)
        _ (set! (. f friction) friction)
        _ (set! (. f restitution) restitution)]
    ;; LEFTOFF: Turn this into a macro!!)
    (with-meta
      (cons f children)
      {:matrix *the-matrix* 
       :shapes (->> children
                    (map (comp :shapes meta))
                    (filter identity)
                    (cons shape))})))

(defmacro fix
  [{:keys [transforms]
    :or {transforms ()}
    :as props}
   & children]
  `(with-transforms ~transforms
     (let [ch# (list ~@children)]

       (fix-fn ~props ch#))))

(defn create-fixtures
  "Create a number of fixtures based on a body and a nested
 list of fixtures."
  [^Body b fxs]
  (let [ret (force (wk/prewalk #(if (sequential? %)
                                  %
                                  (do (. b (createFixture %)) %))
                               fxs))
        _ (doseq [s (flatten (:shapes (meta ret)))] (. s (dispose)))]
    ret))
                               

;;; Shape utilities

(defn poly-verts
  "Gets the vertices of the provided PolygonShape."
  [p]
  (map #(do (. p (getVertex %1 %2))
            %2)
       (range (. p (getVertexCount)))
       (repeatedly #(Vector2.))))
   

(defn v-shift
  "Shift a sequence of vectors by an offset."
  [ofs vs]
  (map #(. (Vector2. %) (add ofs)) vs))

(let [ty (MathUtils/sinDeg 120)
      tx -0.5]
  (defn eq-tri
    "An equilateral triangle facing the positive x axis.  Default
 alignment is center."
    [&{:keys[align]
       :or {align :center}}]
    {:pre [(align-flags align)]}
    #(let [vecs
          (->>
           [#v2(1,0) #v2(tx,ty) #v2(tx,(- ty))]
           (map transform)
           (v-shift
            (case align
              :center #v2(0,0) :left #v2((- tx), 0)
              :right #v2(-1, 0) :top #v2(0,(- ty))
              :bottom #v2(0,ty)
              (throw (Exception.
                      (str "Unsupported alignment option: " align))))))
          ret (PolygonShape.)
          _ (. ret (set (util/v2-to-floats vecs)))]
      ret)))

(defn iso-tri
  "Create an isoceles triangle, defined by the ratio of its base to
 its length, oriented along the x axis.  Default alignment is left."
  [r &{:keys [align] :or {align :left}}]
  {:pre [(align-flags align)]}
  #(let [verts [#v2(r,0) #v2(0,1) #v2(0,-1)]
        centroid (. (reduce (fn [v1 v2] (. v1 (add v2))) #v2(0,0) verts)
                    (div #f,3))
        [cx cy] [(. centroid x) (. centroid y)]
        verts (->> (v-shift (case align
                              :center #v2((- cx),0)
                              :left #v2(0,0)
                              :right #v2((- r),0)
                              :top #v2((- cx),-1)
                              :bottom #v2((- cx),1))
                            verts)
                   (map transform))
        ret (PolygonShape.)
        _ (. ret (set (util/v2-to-floats verts)))]
    ret))
    
        
(defn rect
  "Defines a rectangle along the x axis, defined by the
 ratio of its width to its height.  Default alignment is center"
  [r &{:keys [align] :or {align :center}}]
  {:pre [(align-flags align)]}
  #(let [verts (->> [#v2(0,0) #v2(r,0) #v2(r,1) #v2(0,1)]
                   (v-shift (case align
                              :center #v2((- (/ r 2.0)),-0.5)
                              :left #v2(0,-0.5)
                              :right #v2((* -2 r),-0.5)
                              :top #v2((- (/ r 2.0)),-1)
                              :bottom #v2((- (/ r 2.0)),0)))
                   (map transform))
        ret (PolygonShape.)
        _ (. ret (set (util/v2-to-floats verts)))]
    ret))

(defn circ
  "Make a unit circle.  Default alignment is
 centered."
  [&{:keys[align] :or {align :center}}]
  {:pre [(align-flags align)]}
  #(let [vert (case align :center #v2(0,0) :left #v2(1,0)
                    :right #v2((- 1),0) :top #v2(0,(- 1))
                    :bottom #v2(0,1))
         vert (transform vert)
         ret (CircleShape.)
         _ (. ret (setRadius (. (transform #v2(1,0)) (len))))
         _ (. ret (setPosition vert))]
     ret))
