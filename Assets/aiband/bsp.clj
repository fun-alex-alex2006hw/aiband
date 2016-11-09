(ns aiband.bsp
  (:require [aiband.v2d :refer :all :reload true]))

;; Binary Space Partitioning algorithm
;;
;; BSP data structure is inclusive, so the min and max numbers are all
;; valid coordinates. So, a width 10 and height 10 BSP entry will have
;; min 0, max 9 of each for x and y.

(def min-dim
  "Minimum dimension after fully partitioned"
  6)

(def min-area
  "Minimum area for a partitioning"
  (* (inc (* 2 min-dim)) 
     (inc (* 2 min-dim))))

(def min-area-mult
  "Minimum multiple of the area for 100% chance of splitting"
  3)

(def min-split-chance
  "Base chance of splitting something with the minimal area (decimal 0-1)."
  0.10)

(def max-split-chance
  "Base chance of splitting something with the maximum area before guaranteed
   split."
  0.85)

(defn make-bsp
  [min-x min-y max-x max-y children]
  { :min-x min-x :min-y min-y :max-x max-x :max-y max-y
    :children (into [] children) })

(defn split
  "Splits a bsp randomly either horizontally or vertically.
   Returns list of two new bsps. Always leaves a column/row
   of buffer between the two. As such, make sure that the
   width or height of this input bsp is at least 3."
  [bi] ; bsp-in
  ;; Only split an axis if it's big enough
  (let [vs  ; vertical split
          (cond
            ;; Is X too small to split vertically?
            (<= (- (:max-x bi) (:min-x bi)) (* 2 min-dim))
            false
            ;; Is Y too small to split horizontally?
            (<= (- (:max-y bi) (:min-y bi)) (* 2 min-dim))
            true
            ;; Randomly pick something to split
            :else
            (= 1 (rand-int 2)))
        min-d (if vs (:min-x bi) (:min-y bi))
        max-d (if vs (:max-x bi) (:max-y bi))
        d-split (+ min-d min-dim (rand-int (- max-d min-d min-dim min-dim)))]
    ;; First our lesser half, then our greater half
    [(make-bsp (:min-x bi) (:min-y bi)
               (if vs (- d-split 1) (:max-x bi))
               (if vs (:max-y bi) (- d-split 1)) [])
     (make-bsp (if vs (+ d-split 1) (:min-x bi))
               (if vs (:min-y bi) (+ d-split 1))
               (:max-x bi) (:max-y bi) [])]))

(defn split?
  "Decides if we should split this BSP.
   It has to be big enough to split, but also
   if not super-big has only a chance to split.
   This is _not_ a pure function."
  [bi] ; BSP in
  (let [w (- (:max-x bi) (:min-x bi) -1)
        h (- (:max-y bi) (:min-y bi) -1)]
    (if 
      ;; Big enough to split?
      (and (or (> w (* 2 min-dim))
               (> h (* 2 min-dim)))
            (> (* w h) min-area))
      (if (> (* w h) (* min-area min-area-mult))
        ;; Guaranteed split - too big
        true
        ;; Randomly guess if we split
        (let [mult-of-min (/ (* w h) min-area)
              ;; mult-of-min will be 1 to min-area-mult
              ;; scale it to 0-1
              portion-to-split (/ (dec mult-of-min) (dec min-area-mult))
              chance-to-split (+ min-split-chance
                                 (* portion-to-split (- max-split-chance min-split-chance)))]
          (println "mult-of-min:" mult-of-min "portion-to-split:" portion-to-split
                   "chance-to-split:" chance-to-split)
          ;; Split if our random number says to do it...
          (<= (rand) chance-to-split)))
      ;; Not big enough to split
      false)))


(defn partition-bsp
  "Returns the input bsp with children added, or
   if it's too small to partition, unchagned."
  [bsp-in]
  ;; TODO: Only partition it with some percentage chance proportional to how much
  ;;       bigger than the minimum size this partition is.
  ;; The -1 is because if it's exactly one column/row, it's still that wide
  (let [w (- (:max-x bsp-in) (:min-x bsp-in) -1)
        h (- (:max-y bsp-in) (:min-y bsp-in) -1)]
    ;; FIXME: Split it if EITHER dimension is big enough
    (if (split? bsp-in)
      ;; Partition this one and its (new) children
      (assoc bsp-in :children (mapv partition-bsp (split bsp-in)))
      ;; Don't partition
      bsp-in)))

;; Returns a structure like this:
;; { :min-x :min-y :max-x :max-y :children [...] }
(defn gen-bsp
  [w h]
  { :min-x 0 :max-x (dec w) :min-y 0 :max-y (dec h) :children () })




(defn visualize-internal
  "Recursively fills in the 2d vector of characters with letters representing each
   node of the BSP."
  [bsp v node-num]
  (if (nil? bsp)
    ;; Terminal case - no bsp
    [v node-num]
    ;; Recursive case - a bsp
    (let [node-char (char (+ (int \A) node-num))
          v-node (map2d-indexed 
                  (constantly node-char)
                  [(:min-x bsp) (:min-y bsp) (:max-x bsp) (:max-y bsp)]
                  v)
          [left-bsp right-bsp] (:children bsp)
          [v-left num-left]   (visualize-internal left-bsp v-node (inc node-num))
          [v-right num-right] (visualize-internal right-bsp v-left num-left)]
      [v-right num-right])))

(defn visualize
  "Turns the BSP into a set of strings that represents all the partitions
   graphically."
  [bi]
  (let [w (inc (:max-x bi)) ; These maxes are inclusive
        h (inc (:max-y bi))
        v (vec2d w h \.)
        v-seq (first (visualize-internal bi v 0))]
    (into [] (map #(apply str %) v-seq))))

;; To print the visualization:
#_(do (doall (map println (visualize (partition-bsp (gen-bsp 95 35))))) nil)

