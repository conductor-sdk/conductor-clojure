(ns conductor.metadata
  (:import (com.netflix.conductor.client.http MetadataClient)
           (io.orkes.conductor.client.http OrkesMetadataClient)
           (com.netflix.conductor.common.metadata.tasks TaskDef)
           (com.netflix.conductor.common.metadata.tasks TaskType)
           (com.netflix.conductor.common.metadata.workflow WorkflowDef)
           (com.netflix.conductor.common.metadata.workflow WorkflowTask))
  (:require [clojure.tools.logging :as log]
            [conductor.mapper-utils :as mapperutils]))

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


(defn register-tasks-using-client
  "Given a client instance and a list of tasks,
  will register the task in consuctor"
  [client tasks]
  (.registerTaskDefs client (map mapperutils/clj-task->TaskDef tasks)))

(defn register-tasks
  "Takes options and a list of tasks in EDN, will register the tasks in conductor"
  [options tasks]
  (-> options
      (metadata-client)
      (register-tasks-using-client tasks)))

(defn workflow-def-using-client
  "Takes a client and a workflow definition in EDN, will register a worflow in conductor"
  [client workflow]
  (.registerWorkflowDef client (mapperutils/clj-workflow->WorkflowDef workflow)))

(defn register-workflow-def
  "Takes a map of options, and an EDN defined workflow. Will register a workflow"
  [options workflow]
(-> options
      (metadata-client)
      (workflow-def-using-client workflow)))
