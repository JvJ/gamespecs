(ns gamespecs.core
  (:require [potemkin :as ptk]
            simplecs.core
            simplecs.tags
            gamespecs.animations
            gamespecs.app
            gamespecs.input
            gamespecs.util
            gamespecs.files
            gamespecs.systems))

(defmacro import-nss
  "Import entire namespaces."
  [& nss]
  `(ptk/import-vars
    ~@(map #(apply vector % (keys (ns-publics %))) nss))) 

  
;;; Import all those crazy namespaces!
(import-nss simplecs.core ;simplecs
            simplecs.tags
            
            ;;gamespecs
            gamespecs.animations 
            gamespecs.app
            gamespecs.input
            gamespecs.util
            gamespecs.files
            gamespecs.systems
            gamespecs.physics)



