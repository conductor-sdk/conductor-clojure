(ns conductor.mapper-utils
(:import (com.netflix.conductor.client.http MetadataClient)
           (io.orkes.conductor.client.http OrkesMetadataClient)
           (com.netflix.conductor.common.metadata.tasks TaskDef)
           (com.netflix.conductor.common.metadata.tasks TaskType)
           (com.netflix.conductor.common.metadata.workflow WorkflowDef)
           (com.netflix.conductor.common.metadata.workflow WorkflowTask)
           (com.netflix.conductor.common.metadata.tasks TaskResult TaskResult$Status)
           (com.netflix.conductor.client.worker Worker)))

(defn clj-task->TaskDef [{:keys [name description owner-email retry-count timeout-seconds response-timeout-seconds]}]
  (TaskDef. name description owner-email retry-count timeout-seconds response-timeout-seconds))

(defn clj-task-type->TaskType [type]
  (case type
    :simple (.name TaskType/SIMPLE )
    :dynamic (.name TaskType/DYNAMIC )
    :fork-join (.name TaskType/FORK_JOIN )
    :fork-join-dynamic (.name TaskType/FORK_JOIN_DYNAMIC )
    :decision (.name TaskType/DECISION )
    :switch (.name TaskType/SWITCH )
    :join (.name TaskType/JOIN )
    :do-while (.name TaskType/DO_WHILE )
    :sub-workflow (.name TaskType/SUB_WORKFLOW )
    :event (.name TaskType/EVENT )
    :wait (.name TaskType/WAIT )
    :user-defined (.name TaskType/USER_DEFINED )
    :http (.name TaskType/HTTP )
    :lambda (.name TaskType/LAMBDA )
    :inline (.name TaskType/INLINE )
    :exclusive-join (.name TaskType/EXCLUSIVE_JOIN )
    :terminate (.name TaskType/TERMINATE )
    :kafka-publish (.name TaskType/KAFKA_PUBLISH )
    :json-jq-transform (.name TaskType/JSON_JQ_TRANSFORM )
    :set-variable (.name TaskType/SET_VARIABLE)) )

(defn clj-workflow-task->WorkflowTask [{:keys [name task-reference-name description input-parameters type]}]
  (doto (WorkflowTask.)
    (.setName name)
    (.setTaskReferenceName task-reference-name)
    (.setDescription description)
    (.setInputParameters input-parameters)
    (.setType (clj-task-type->TaskType type))))

(defn clj-workflow->WorkflowDef
  [{:keys [name description version tasks
           input-parameters output-parameters schema-version
           restartable owner-email timeout-policy timeout-seconds] :or {restartable true}}]
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
    ;; (.setTimeoutPolicy timeout-policy)
    (.setTimeoutSeconds timeout-seconds)))

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
        task-result
        )
      )))
