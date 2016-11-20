;; Copyright 2016 Douglas P. Fields, Jr.
;; symbolics _at_ lisp.engineer
;; https://symbolics.lisp.engineer/
;; https://twitter.com/LispEngineer
;; https://github.com/LispEngineer
;;;; Aiband - The Artificial Intelligence Roguelike

;;;; Monad utilities for use in Aiband
;;;; Building upon clojure.algo.monads, and enhancing/replacing a few of those.

(ns aiband.monads
  (:require [clojure.algo.monads :as m]))


;; It would be nice to get a "hoist" ability from monad morphing,
;; so that we can apply functions to sub-state, but alas, this isn't
;; really plausible right now.

(defmacro dostate
  "Does a state monad. The same as (domonad state-m ...). Syntactic sugar."
  [& args]
  (apply list 'm/domonad 'm/state-m args))

(defmacro zoom
  "Renames m/with-state-field to zoom"
  [& args]
  (apply list 'm/with-state-field args))



;; Utility function "in" version from monads.
;; TODO: Add a & rest so you can have a function with other args
(defn update-in-val
  "Return a state-monad function that assumes the state to be a map and
   replaces the value associated with the given keys vector by the return value
   of f applied to the old value. The old value is returned."
  [keys f & args]
  (fn [s]
    (let [old-val (get-in s keys)
          new-s   (assoc-in s keys (apply f old-val args))]
      [old-val new-s])))

;; Utility function "in" version from monads.
(defn fetch-in-val
  "Return a state-monad function that assumes the state to be a nested map and
   returns the value corresponding to the given keys vector. The state is not modified."
  [keys]
  (fn [s] [(get-in s keys) s]))

;; Utility function "in" version from monads.
(defn set-in-val
  "Return a state-monad function that assumes the state to be a map and
   replaces the value associated with keys by val. The old value is returned."
  [keys val]
  (update-in-val keys (fn [_] val)))


;; Updated version of the one in clojure.algo.monads that takes additional
;; arguments to the function to be applied
(defn update-state
  "Return a state-monad function that replaces the current state by the
   result of f applied to the current state (and any optional arguments)
   and that returns the old state."
  [f & args]
  (fn [s] [s (apply f s args)]))


;; Updated version of the one in clojure.algo.monads that takes additional
;; arguments to the function to be applied
(defn update-val
  "Return a state-monad function that assumes the state to be a map and
   replaces the value associated with the given key by the return value
   of f applied to the old value and optional args. The old value is returned."
  [key f & args]
  (fn [s]
    (let [old-val (get s key)
          new-s   (assoc s key (apply f old-val args))]
      [old-val new-s])))



(defn run-state-atom
  "Updates the specified atom by running the specific monadic transformation
   on it as the target of a state monad, and returns the result value."
  [atom-name monad-to-run]
  (println "atom-name: " atom-name)
  (let [initial-state (deref atom-name)
        [retval final-state] (monad-to-run initial-state)]
    (reset! atom-name final-state)
    retval))
