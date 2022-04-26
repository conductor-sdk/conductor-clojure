;;/*
;; * <p>
;; * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
;; * the License. You may obtain a copy of the License at
;; * <p>
;; * http://www.apache.org/licenses/LICENSE-2.0
;; * <p>
;; * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
;; * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
;; * specific language governing permissions and limitations under the License.
;; */
(ns conductor.metadata
  (:import (com.netflix.conductor.client.http MetadataClient)
           (io.orkes.conductor.client.http OrkesMetadataClient)
           (com.netflix.conductor.common.metadata.tasks TaskDef)
           (com.netflix.conductor.common.metadata.tasks TaskType)
           (com.netflix.conductor.common.metadata.workflow WorkflowDef)
           (com.netflix.conductor.common.metadata.workflow WorkflowTask))
  (:require [clojure.tools.logging :as log]
            [conductor.mapper-utils :as mapperutils]
            [conductor.metadata :as metadata]))

(defn metadata-client
  "Given a map with options creates a metadata-client"
  [{:keys [app-key app-secret url] :or {url "http://localhost:8080/api/"}}]
  (let [client (OrkesMetadataClient. )]
    (.setRootURI client url)
    (when app-key
      (.withCredentials client app-key app-secret)
      (log/debug "Creating client with authentication"))
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

(defn register-workflow-def-using-client
  "Takes a client and a workflow definition in edn, will register a worflow in conductor"
  [client workflow]
  (.registerWorkflowDef client (mapperutils/clj-workflow->WorkflowDef workflow)))

(defn register-workflow-def
  "Takes a map of options, and an EDN defined workflow. Will register a workflow"
  [options workflow]
(-> options
      (metadata-client)
      (register-workflow-def-using-client workflow)))

(defn update-workflows-def-using-client
  "takes a client and a list of workflows definition in edn, will update all workflows in list"
  [client workflows]
  (.updateWorkflowDefs client (map mapperutils/clj-workflow->WorkflowDef workflows)))

(defn update-workflows-def
  "Takes a map of options, and a list of workflow definitions. will update every workflow on the list"
  [options workflows]
  (-> options
      (metadata-client)
      (update-workflows-def-using-client workflows)))

(defn get-workflow-def-using-client
  "Takes a client a name and a version. Will fetch for workflow definition"
  ([client name version]
   (mapperutils/java-map->clj (.getWorkflowDef client name (int version))) )
  ([client name]
   (mapperutils/java-map->clj (.getWorkflowDef client name 1))))

(defn get-workflow-def
  "Takes a map of options, a name and a version. Will fetch for workflow definition"
  [options name version]
(-> options
      (metadata-client)
      (get-workflow-def-using-client name version)))


(defn unregister-workflow-def-using-client
  "Takes a client a name and a version. will unregister workflow. returns nil on success"
  [client name version]
(.unregisterWorkflowDef client name (int version)))

(defn unregister-workflow-def
  "Takes a map of options, a name and a version. will unregister workflow. returns nil on success"
  [options name version]
  (-> options
      (metadata-client)
      (unregister-workflow-def-using-client name version)))

(defn update-task-definition-with-client [client task-definition]
  (.updateTaskDef client (mapperutils/clj-task->TaskDef task-definition)))

(defn update-task-definition
  "Takes a map of options, and a list of workflow definitions. will update every workflow on the list"
  [options task-definition]
  (-> options
      (metadata-client)
      (update-task-definition-with-client task-definition)))

(defn get-task-def-with-client [client task-ref]
  (mapperutils/java-map->clj (.getTaskDef client task-ref) ))

(defn get-task-def [options task-def]
[options task-def]
  (-> options
      (metadata-client)
      (get-task-def-with-client task-def)))


(defn unregister-task-with-client [client task-ref]
  (.unregisterTaskDef client task-ref))

(defn unregister-task
[options task-ref]
  (-> options
      (metadata-client)
      (unregister-task-with-client task-ref)))

(comment
(def options {
              :app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d"
              :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean"
              :url "http://localhost:8080/api/"
              })
(def wf (get-workflow-def options "simple_wf" 1) )

(:tasks wf)
(register-workflow-def options (assoc wf :version 29))

(unregister-workflow-def options "exclusive_join" 1)

(def some-task (get-task-def options "cool_clj_task_b") )

(update-task-definition options (assoc some-task :owner-email "othermaila@mail.com") )

(unregister-task options "cool_clj_task_b")


)

