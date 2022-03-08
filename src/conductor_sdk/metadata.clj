(ns conductor-sdk.metadata
  (:import (com.netflix.conductor.client.http MetadataClient)
           (io.orkes.conductor.client.http OrkesMetadataClient)
           (com.netflix.conductor.common.metadata.tasks TaskDef)
           (com.netflix.conductor.common.metadata.tasks TaskType)
           (com.netflix.conductor.common.metadata.workflow WorkflowDef)
           (com.netflix.conductor.common.metadata.workflow WorkflowTask))
    (:require [clojure.tools.logging :as log])
  )

(defn metadata-client
  "Given a map with options creates a metadata-client"
  [{:keys [app-key app-secret url] :or {url "http://localhost:8080/api"}}]
  (let [client (OrkesMetadataClient. )]
    (.setRootURI client url)
    (when app-key
      (.withCredentials client app-key app-secret)
      (log/debug "Creating client with authentication")
      )
    client))

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

(defn register-tasks-using-client
  "Given a client instance and a list of tasks,
  will register the task in consuctor"
  [client tasks]
  (.registerTaskDefs client (map clj-task->TaskDef tasks)))

(defn register-tasks
  "Takes options and a list of tasks in EDN, will register the tasks in conductor"
  [options tasks]
  (-> options
      (metadata-client)
      (register-tasks-using-client tasks)))

(defn workflow-def-using-client
  "Takes a client and a workflow definition in EDN, will register a worflow in conductor"
  [client workflow]
  (.registerWorkflowDef client (clj-workflow->WorkflowDef workflow)))

(defn register-workflow-def
  "Takes a map of options, and an EDN defined workflow. Will register a workflow"
  [options workflow]
(-> options
      (metadata-client)
      (workflow-def-using-client workflow)))
