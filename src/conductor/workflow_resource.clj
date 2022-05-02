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
(ns conductor.workflow-resource
  (:import
   (io.orkes.conductor.client.http OrkesWorkflowClient))
  (:require [clojure.tools.logging :as log]
            [conductor.mapper-utils :as mapperutils]))

(defn workflow-client
  "Returns an instance of TaskClient. when app-key and app-secret are provided
  Then returned instance will be Orkes Compatible"
  [{:keys [app-key app-secret url] :or {url "http://localhost:8080/api/"}} ]
  (let [client (OrkesWorkflowClient. )]
    (.setRootURI client url)
    (when app-key
      (.withCredentials client app-key app-secret)
      (log/debug "Creating client with authentication"))
    client))

(defn start-workflow-with-client
  "Takes a client and a start-request map, and starts a workflow"
  [client start-request]
  (.startWorkflow client (mapperutils/clj-start-workflow-request->StartWorkflowRequest start-request)))


(defn start-workflow
  "Takes an option map and a start-request map and starts a workflow.
  Returns the id of a workflow execution"
  [options start-request]
  (-> (workflow-client options)
      (start-workflow-with-client start-request)))

(defn get-workflow-with-client
  ([client workflow-id] (.getWorkflow client workflow-id false))
  ([client workflow-id include-tasks] (.getWorkflow client workflow-id include-tasks)))

(defn get-workflow
  "Returns a workflow execution for given workflow-id"
  [options workflow-id & args]
  (let [client-inst (workflow-client options)]
    (mapperutils/java-map->clj (apply get-workflow-with-client client-inst workflow-id args))))

(defn terminate-workflow-with-client
  "Takes a client a workflow-id and an optional reason. will terminate a running workflow"
  ([client workflow-id reason] (.terminateWorkflow client workflow-id reason))
  ([client workflow-id] (.terminateWorkflow client workflow-id nil)))

(defn terminate-workflow
  "Terminates a running workflow. given an id and an optional reason"
  [options workflow-id & args]
  (-> (workflow-client options)
      ( #(apply terminate-workflow-with-client % workflow-id args) )))

(defn get-workflows-with-client
  "Takes a client,workflow-name,correlation-id and optional keyword arguments :inclide-closed and :include-tasks
  Return a list of workflow-executions"
  ([client wf-name correlation-id & {:keys [include-closed include-tasks] :or {include-closed false, include-tasks false}}] (mapperutils/java-map->clj (.getWorkflows client wf-name correlation-id include-closed include-tasks))))

(defn get-workflows
  "Takes a options, workflow-name, correlation-id and optional keyword arguments :inclide-closed and :include-tasks
  Return a list of workflow-executions"
  [options wf-name correlation-id & args]
  (-> (workflow-client options)
      ( #(apply get-workflows-with-client % wf-name correlation-id args) )))

(defn delete-workflow-with-client
  "Takes a client,workflow-id and an optional archive-workflow boolean. Deletes the workflow execution.
  Returns nil"
  ([client workflow-id archive-workflow] (.deleteWorkflow client workflow-id archive-workflow))
  ([client workflow-id] (delete-workflow-with-client client workflow-id false)))

(defn delete-workflow
  "Takes a options,workflow-id and an optional archive-workflow boolean. Deletes the workflow execution.
  Returns nil"
  [options workflow-id & args]
  (-> (workflow-client options)
      (#(apply delete-workflow-with-client % workflow-id args))))

(defn terminate-workflows-with-client
  "Takes a client and a list of workflow-ids and an optional reason. Terminates workflows in the list.
  Returns a BulkResponse map with success and failures"
  ([client workflow-ids reason] (mapperutils/java-map->clj (.terminateWorkflows client workflow-ids reason) ))
  ([client workflow-ids] (terminate-workflows-with-client client workflow-ids nil)))

(defn terminate-workflows
  "Takes an options map and a list of workflow-ids and an optional reason. Terminates workflows in the list.
  Returns a BulkResponse map with success and failures"
  [options workflow-ids & args]
  (-> (workflow-client options)
      (#(apply terminate-workflows-with-client % workflow-ids args))))

(defn get-running-workflow-with-client
  "Takes a client,workflow-name and a version.
  Returns a list of running workflow ids"
  [client wf-name version] (.getRunningWorkflow client wf-name (int version)))

(defn get-running-workflow
  "Takes options, workflow-name and a version.
  Returns a list of running workflow ids"
  [options wf-name version]
  (-> (workflow-client options)
      (get-running-workflow-with-client wf-name version) ))

(defn get-workflow-by-time-period-with-client
  "Takes a client, workflow-name version start-time and end-time (in milliseconds).
  Returns a list of workflow-ids within that period"
  [client wf-name version start-time end-time]
  (.getWorkflowsByTimePeriod client wf-name (int version) start-time end-time))

(defn get-workflow-by-time-period
  "Takes options, workflow-name version start-time and end-time (in milliseconds).
  Returns a list of workflow-ids within that period"
  [options wf-name version start-time end-time]
  (-> (workflow-client options)
      (get-workflow-by-time-period-with-client wf-name version start-time end-time)))

;; (defn run-decider-with-client
;;   [client workflow-id]
;;   (.runDecider client workflow-id))

;; (defn run-decider [options workflow-id]
;;   (-> (workflow-client options)
;;       (run-decider-with-client workflow-id)))

(defn pause-workflow-with-client
  "Takes a client and a workflow-id. Pauses the current workflow.
  Returns nil"
  [client workflow-id]
  (.pauseWorkflow client workflow-id))

(defn pause-workflow
  "Takes options and a workflow-id. Pauses the current workflow.
  Returns nil"
  [options workflow-id]
  (-> (workflow-client options)
      (pause-workflow-with-client workflow-id)))

(defn resume-workflow-with-client
  "Takes a client and a workflow-id. Resumes a paused workflow.
  Returns nil"
  [client workflow-id]
  (.resumeWorkflow client workflow-id))

(defn resume-workflow
  "Takes options and a workflow-id. Resumes a paused workflow.
  Returns nil"
  [options workflow-id]
  (-> (workflow-client options)
      (resume-workflow-with-client workflow-id)))

(defn skip-task-from-workflow-with-client
  "Takes a client a workflow-id and a task-reference-name. Will skip the task.
  Returns nil"
  [client workflow-id task-reference-name]
  (.skipTaskFromWorkflow client workflow-id task-reference-name))

(defn skip-task-from-workflow
  "Takes options a workflow-id and a task-reference-name. Will skip the task.
  Returns nil"
  [options workflow-id task-reference-name]
  (-> (workflow-client options)
      (skip-task-from-workflow-with-client workflow-id task-reference-name)))

(defn rerun-workflow-with-client
  [client workflow-id rerun-wf-request]
  (.rerunWorkflow client workflow-id (mapperutils/clj-rerun-workflow-request->RerunWorkflowRequest rerun-wf-request)))

(defn rerun-workflow [options workflow-id rerun-wf-request]
  (-> (workflow-client options)
      (rerun-workflow-with-client workflow-id rerun-wf-request)))

(defn restart-with-client
  ([client workflow-id use-latest-definitions] (.restart client workflow-id use-latest-definitions))
  ([client workflow-id ] (restart-with-client client workflow-id false)))

(defn restart
  [options workflow-id & args]
  (-> (workflow-client options)
      ( #(apply restart-with-client workflow-id args) )))

(defn retry-last-failed-task-with-client
  [client workflow-id] (.retryLastFailedTask client workflow-id))

(defn retry-last-failed-task
  [options workflow-id]
  (->
   (workflow-client options)
   (retry-last-failed-task-with-client workflow-id)))

;; (defn reset-callbacks-for-in-progress-tasks-with-client
;;   [client workflow-id]
;;   (.resetCallbacksForInProgressTasks client workflow-id))

;; (defn reset-callbacks-for-in-progress-tasks
;;   [options workflow-id]
;;   (-> (workflow-client options)
;;       (reset-callbacks-for-in-progress-tasks workflow-id)))

(defn search-with-client
  [client query]
  (mapperutils/java-map->clj (.search client query) ))

(defn search [options query]
  (-> (workflow-client options)
      (search-with-client query)))


(comment
(def options {
              :app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d"
              :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean"
              :url "http://localhost:8080/api/"
              })

(def client
(workflow-client options)
  )
(.getWorkflow client  "8542dfe4-259b-4e65-99ca-4116a020524d" false)
(mapperutils/java-map->clj (get-workflow-with-client client "8542dfe4-259b-4e65-99ca-4116a020524d") )
(get-workflow options "8542dfe4-259b-4e65-99ca-4116a020524d" )


  )
