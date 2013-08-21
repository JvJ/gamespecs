(ns gamespecs.files
  "Provides an interface to libgdx's file management system.
 Re-bind or re-define *file-mode* in order to change the way
 files are handled."
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.files FileHandle)))

;;; File hanlding modes
(def absolute ::absolute)
(def internal ::internal)
(def classpath ::classpath)
(def local ::local)

(def ^:dynamic *file-mode*
  "Determines file loading mode;
 either absolute, internal, classpath, or local."
  absolute)

(defmulti get-file-handle
  "Get a file handle based on the current file mode."
  (fn [& _] *file-mode*))

(defmethod get-file-handle ::absolute
  [fname]
  (.absolute Gdx/files fname))

(defmethod get-file-handle ::internal
  [fname]
  (.internal Gdx/files fname))

(defmethod get-file-handle ::classpath
  [fname]
  (.classpath Gdx/files fname))

(defmethod get-file-handle ::local
  [fname]
  (.local Gdx/files fname))

(defmethod get-file-handle :default
  [fname]
  (Exception. (str "File mode not supported: " *file-mode*)))
