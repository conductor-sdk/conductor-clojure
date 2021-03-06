(ns io.orkes.utils
  (:require [clojure.spec.alpha :as s]))

(s/def :task/name string?)
(s/def :task/taskReferenceName string?)
(s/def :task/inputParameters map?)
(s/def :task/forkTasks coll?) ;; TODO it has to be a coll of coll of tasks
(s/def :task/joinOn coll?);; TODO coll of strings with no spaces
(s/def :task/loopCondition string?);; TODO non empty string
(s/def :task/loopOver coll?);; TODO coll of tasks
(s/def :task/decisionCases map?)
(s/def :task/defaultCase coll?)
(s/def :task/evaluatorType string?);;TODO non empty string
(s/def :task/expression string?);; TODO non empty
(s/def :task/type string?);; TODO non empty
(s/def :task/sink string?)
(s/def :task/dynamicForkTasksParam string?)
(s/def :task/dynamicForkTasksInputParamName string?)
(s/def :task/subWorkflowParam map?)

(s/def :task/simple-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/inputParameters :task/type]
          :opt []))

(s/def :task/fork-join-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/inputParameters :task/forkTasks :task/type]
          :opt []))

(s/def :task/do-while-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/inputParameters :task/type :task/loopOver :task/loopCondition]
          :opt []))

(s/def :task/event-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/inputParameters :task/type :task/sink]
          :opt []))

(s/def :task/join-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/inputParameters :task/type :task/joinOn]
          :opt []))

(s/def :task/wait-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type]
          :opt []))

(s/def :task/fork-join-dynamic-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type :task/inputParameters :task/dynamicForkTasksParam :task/dynamicForkTasksInputParamName]
          :opt []))

(s/def :task/dynamic-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type]
          :opt []))

(s/def :task/inline-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type]
          :opt []))

(s/def :task/switch-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type :task/evaluatorType :task/expression :task/decisionCases :task/defaultCase]
          :opt []))

(s/def :task/kafka-request-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type :task/inputParameters]
          :opt []))

(s/def :task/http-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type :task/inputParameters]
          :opt []))

(s/def :task/json-jq-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type :task/inputParameters]
          :opt []))

(s/def :task/terminate-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type]
          :opt []))

(s/def :task/set-variable-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type :task/inputParameters]
          :opt []))

(s/def :task/sub-workflow-param-task
  (s/keys :req-un [:task/name :task/taskReferenceName :task/type :task/inputParameters :task/subWorkflowParam]
          :opt []))


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
