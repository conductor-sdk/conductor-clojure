(ns io.orkes.utils)

(defmulti flat-task (fn [{task-type :type}] task-type))

;; (defmethod task-map :default [m mfn] (mfn m))

(defmethod flat-task "FORK_JOIN"
  [m]
  (into [m] (->> m :forkTasks flatten (map flat-task) flatten)))

(defmethod flat-task "DO_WHILE"
  [m]
  (into [m] (->> m :loopOver (map flat-task) flatten)))

(defmethod flat-task "SWITCH"
  [m]
  (into [m] (->> m :decisionCases vals flatten (map flat-task) flatten))
  )

(defmethod flat-task :default [m] [m])


(defn tasks-filter [pred coll]
(->> coll (map flat-task) flatten (filter pred) ))

;; (comment
;; (def options
;;     {:app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d",
;;      :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean",
;;      :url "http://localhost:8080/api/"})

;; (def tasks (-> (io.orkes.metadata/get-workflow-def options "Github_star_workflow" 1) :tasks ) )
;; (count (map :type (flat-task (first tasks)) ) )
;; (count (flatten (map #(-> % flat-task ) tasks) ) )
;; (count (tasks-filter #(= (:type %) "HTTP") tasks) )
;; )
