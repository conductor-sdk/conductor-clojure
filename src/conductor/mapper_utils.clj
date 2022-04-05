(ns conductor.mapper-utils
(:import (com.netflix.conductor.client.http MetadataClient)
           (io.orkes.conductor.client.http OrkesMetadataClient)
           (com.netflix.conductor.common.metadata.tasks TaskDef)
           (com.netflix.conductor.common.metadata.tasks TaskType)
           (com.netflix.conductor.common.metadata.workflow WorkflowDef WorkflowDef$TimeoutPolicy)
           (com.netflix.conductor.common.metadata.workflow WorkflowTask)
           (com.netflix.conductor.common.metadata.tasks TaskResult TaskResult$Status)
           (com.netflix.conductor.client.worker Worker)))

(defprotocol MapToClojure
  (->clj [o]))

(extend-protocol MapToClojure
  java.util.Map
  (->clj [o] (let [entries (.entrySet o)]
               (reduce (fn [m [^String k v]]
                         (assoc m (keyword k) (->clj v)))
                       {} entries)))

  java.util.List
  (->clj [o] (vec (map ->clj o)))

  java.lang.Object
  (->clj [o] o)

  nil
  (->clj [_] nil))

(defn java-map->clj-map
  [m]
  (->clj m))

(defn clj-task->TaskDef [{:keys [name description owner-email retry-count timeout-seconds response-timeout-seconds]}]
  (TaskDef. name description owner-email retry-count timeout-seconds response-timeout-seconds))

(defn clj-task-type->TaskType [type]
  (.name (case type
    :simple TaskType/SIMPLE
    :dynamic TaskType/DYNAMIC
    :fork-join TaskType/FORK_JOIN
    :fork-join-dynamic TaskType/FORK_JOIN_DYNAMIC
    :decision TaskType/DECISION
    :switch TaskType/SWITCH
    :join TaskType/JOIN
    :do-while TaskType/DO_WHILE
    :sub-workflow TaskType/SUB_WORKFLOW
    :event TaskType/EVENT
    :wait TaskType/WAIT
    :user-defined TaskType/USER_DEFINED
    :http TaskType/HTTP
    :lambda TaskType/LAMBDA
    :inline TaskType/INLINE
    :exclusive-join TaskType/EXCLUSIVE_JOIN
    :terminate TaskType/TERMINATE
    :kafka-publish TaskType/KAFKA_PUBLISH
    :json-jq-transform TaskType/JSON_JQ_TRANSFORM
    :set-variable TaskType/SET_VARIABLE
    nil TaskType/SIMPLE
    (throw (Exception. (str "Type " (name type) " cant be mapped. Did you misspell?")))) ) )

(defn clj-workflow-task->WorkflowTask [{:keys [
                                               name task-reference-name description
                                               input-parameters type fork-tasks join-on
                                               decision-cases default-case loop-condition
                                               loop-over dynamic-fork-tasks-input-param-name dynamic-fork-tasks-param
                                               dynamic-task-name-param async-complete case-value-param
                                               ]}]
  (doto (WorkflowTask.)
    (.setName name)
    (.setTaskReferenceName task-reference-name)
    (.setDescription description)
    (.setInputParameters input-parameters)
    (.setType (clj-task-type->TaskType type))
    (#(when fork-tasks (.setForkTasks % (map (fn [inner] (map clj-workflow-task->WorkflowTask inner)) fork-tasks))))
    (#(when join-on (.setJoinOn % join-on)))
    (#(when decision-cases (.setDecisionCases % (update-vals decision-cases (fn [dtasks] (map clj-workflow-task->WorkflowTask dtasks))))))
    (#(when default-case (.setDefaultCase % (map clj-workflow-task->WorkflowTask default-case))))
    (#(when loop-condition (.setLoopCondition % loop-condition)))
    (#(when loop-over (.setLoopOver % (map clj-workflow-task->WorkflowTask loop-over))))
    (#(when dynamic-fork-tasks-input-param-name (.setDynamicForkTasksInputParamName % dynamic-fork-tasks-input-param-name)))
    (#(when dynamic-fork-tasks-param (.setDynamicForkTasksParam % dynamic-fork-tasks-param)))
    (#(when dynamic-task-name-param (.setDynamicTaskNameParam % dynamic-task-name-param)))
    (#(when async-complete (.setAsyncComplete % async-complete)))
    (#(when case-value-param (.setCaseValueParam % case-value-param)))
    ))

(defn timeout-policy->TimeoutPolicy [timeout-policy]
  [timeout-policy]
  (case timeout-policy
    :time-out-wf WorkflowDef$TimeoutPolicy/TIME_OUT_WF
    :alert-only WorkflowDef$TimeoutPolicy/ALERT_ONLY
    (throw (Exception. (str "timeout-policy" type "cant be mapped. Did you misspell?")))))

(defn clj-workflow->WorkflowDef
  [{:keys [name description version tasks
           input-parameters output-parameters schema-version
           restartable owner-email timeout-policy timeout-seconds] :or {restartable true, timeout-policy :alert-only }}]
  (doto (WorkflowDef.)
    (.setName name)
    (.setDescription description)
    (.setVersion version)
    (.setTasks (map clj-workflow-task->WorkflowTask tasks) )
    (.setInputParameters input-parameters)
    (.setOutputParameters output-parameters)
    (.setSchemaVersion schema-version)
    (.setRestartable restartable)
    (.setOwnerEmail owner-email)
    (.setTimeoutPolicy (timeout-policy->TimeoutPolicy timeout-policy))
    (.setTimeoutSeconds timeout-seconds))
  )

(defn status->task-result-status
  "Maps a status key to a test result key"
  [status]
  (case status
    :in-progress TaskResult$Status/IN_PROGRESS
    :failed TaskResult$Status/FAILED
    :failed-with-terminal-error TaskResult$Status/FAILED_WITH_TERMINAL_ERROR
    :completed TaskResult$Status/COMPLETED))

(defn clj-worker->Worker
  "Returns a Worker instance for a worker provided in the form of a map"
  [worker]
  (reify Worker
    (getTaskDefName [this]
      (str (:name worker)))
    (execute [this task]
      (let [task-result (TaskResult. task)
            input-data (.getInputData task)
            executor-fn (:execute worker)
            [result-status result-output-data] (executor-fn input-data)]
        (.setStatus task-result (status->task-result-status result-status))
        (.setOutputData task-result result-output-data)
        task-result))))
