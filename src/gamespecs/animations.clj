 (ns gamespecs.animations
  "This namespace provides functions for 2d animations.
Animation : A map of the following format
 {:frame-index Current frame index
  :frame-dt Delta time for current frame
  :current-state The current state of the animation
  :prev-state Previous state of the animation
  :animations Map of states to a sequence of
 {:delay d, :textureregion textureregion} maps.


Animation Spec : A map of the following format
 {:files {kw filename} map
 :cell-sizes {kw [x y]} map. Cell sizes in pixels
 :animations map of states to
    {:delay delay
     :file-kw file-kw
     :cell [x y]}}"
  (:require [clojure.algo.generic.functor :as ft]
            [gamespecs.files :as fls]
            [gamespecs.util :as util])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion
                                          SpriteBatch)))

(defn frames
  "A helper function for creating a list of frames.
 mode: One of the following animation modes must be supplied:
  :loop
  :once

 file?: Optional file keyword.  If supplied, the frame specifications will all
 have the same file keyword."
  [mode file? & fs]
  (let [[file? fs]
        (if (keyword? file?)
          [file? (mapv #(cons file? %) fs)]
          [nil (vec (cons file? fs))])]
    (with-meta fs {:mode mode})))
        

(defn anim-spec
  "Helper function to create an animation specification.
 Params
   file-or-map: a filename or map of keywords to filenames.
   cell-sizes: either a single [w h] cell size, or a map of cell
   sizes which matches the file map.
   animations: maps of states to sequences of file-kw? delay [x y]]
   file-kw defaults to nil if not supplied."
  [file-or-map
   cell-sizes
   animations]
  (let [file-or-map (if (string? file-or-map)
                      {nil file-or-map}
                      file-or-map)
        cell-sizes (if (vector? cell-sizes)
                     (into {} (map #(-> [% cell-sizes]) (keys file-or-map)))
                     cell-sizes)

        _ (if-not (= (set (keys cell-sizes))
                     (set (keys file-or-map)))
            (throw (Exception. "File maps and cell-size maps require identical keysets.")))

        animations (ft/fmap
                    (fn [frseq]
                      (with-meta
                        (map (fn [frame]
                               (case (count frame)
                                 3 (let [[fkw dly cell] frame]
                                     {:delay dly :file-kw fkw :cell cell})
                                 2 (let [[dly cell] frame]
                                     {:delay dly :file-kw nil :cell cell})
                                 (throw (Exception.
                                         "Need 2 or 3 elements in animation frame vector."))))
                             frseq)
                        (meta frseq)))
                    animations)]
    (with-meta
      {:files file-or-map
       :cell-sizes cell-sizes
       :animations animations}
      {:type :animation-spec})))
    
(defn load-animation
  "Loads an animation from an animation spec."
  [{:keys [files
           cell-sizes
           animations]
    :as spec}]
  (let [;; Map of files to cell sizes
        handles (ft/fmap fls/get-file-handle files)
        textures (into
                  {} (map #(-> [%1 (Texture. (handles %1))])
                            (keys files)))
        cells (into
               {} (map #(-> [%1 (let [[x y] (cell-sizes %1)] 
                                  (TextureRegion/split
                                   (textures %1) x y))])
                       (keys files)))]    
    ;; TODO: (Important) does LibGDX/lwjgl cache textures??
    ;; or do we need a global cache?
    (merge
     {:frame-index 0
      :frame-dt 0
      :current-state nil
      :prev-state nil}
     (into
      {} (for [ [state frames] animations
                :let [mode (:mode (meta frames))] ]
           [state
            (with-meta
              (mapv
               (fn [{delay :delay
                     file-kw :file-kw
                     [r c] :cell}]
                 {:delay delay
                  :textureregion (aget (cells file-kw) r c)})
               frames)
              {:mode mode})])))))


                                 
