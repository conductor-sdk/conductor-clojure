;;/*
;; * <p>
;; * Licensed under the Apache License, Version 2.0 (the "License"); you may not
;; use this file except in compliance with
;; * the License. You may obtain a copy of the License at
;; * <p>
;; * http://www.apache.org/licenses/LICENSE-2.0
;; * <p>
;; * Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on
;; * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
;; express or implied. See the License for the
;; * specific language governing permissions and limitations under the License.
;; */
(ns io.orkes.workflow-resource
  (:require [io.orkes.api-client :refer [generic-client]]
            [clojure.walk :as walk]))

;; (defn workflow-client [options] (generic-client options "workflow"))

(defn start-workflow-with-client [client wf-request]
  (client "workflow" :method :post :body wf-request))

(defn start-workflow
  "Takes an option map and a start-request map and starts a workflow.
  Returns the id of a workflow execution"
  ([options wf-request]
   (-> (generic-client options)
       (start-workflow-with-client wf-request))))

;;; THIS DOES NOT WORK. PLEASE TEST
(defn get-workflow-with-client
  ([client workflow-id include-tasks]
   (client (str "workflow/"  workflow-id) :method :get
           :query-params {"includeTasks" include-tasks}))
  ([client workflow-id] (get-workflow-with-client client workflow-id true)))

(defn get-workflow
  "Returns a workflow execution for given workflow-id"
  [options workflow-id & {:keys [includeTasks], :or {includeTasks true}}]
  (-> (generic-client options)
      (get-workflow-with-client workflow-id includeTasks)))

(defn terminate-workflow-with-client
  "Takes a client a workflow-id and an optional reason. will terminate a running workflow"
  ([client workflow-id reason]
   (client (str "workflow/" workflow-id) :method :delete :query-params {"reason" reason}))
  ([client workflow-id] (client (str "workflow/" workflow-id) :method :delete)))

(defn terminate-workflow
  "Terminates a running workflow. given an id and an optional reason"
  ([options workflow-id & args]
   (-> (generic-client options)
       (#(apply terminate-workflow-with-client % workflow-id args)))))

(defn get-workflows-with-client
  "Takes a client,workflow-name,correlation-id and optional keyword arguments :inclide-closed and :include-tasks
  Return a list of workflow-executions"
  ([client wf-name correlation-id
    {:keys [includeClosed includeTasks],
     :or {includeClosed false, includeTasks false}}]
   (client (str "workflow/" wf-name "/correlated/" correlation-id)
           :method :get
           :query-params {"includeClosed" includeClosed,
                          "includeTasks" includeTasks})))
;;;
;;;GOT HERE IN THE REFACTOR
;;;
(defn get-workflows
  "Takes a options, workflow-name, correlation-id and optional keyword arguments :inclide-closed and :include-tasks
  Return a list of workflow-executions"
  ([options wf-name correlation-id o-options]
   (-> (generic-client options)
       (get-workflows-with-client wf-name correlation-id o-options)))
  ([options wf-name correlation-id]
   (get-workflows options wf-name correlation-id {})))

(defn delete-workflow-with-client
  "Takes a client,workflow-id and an optional archive-workflow boolean. Deletes the workflow execution.
  Returns nil"
  ([client workflow-id archive-workflow]
   (client (str "workflow/" workflow-id "/remove")
           :method :delete
           :query-params {"archiveWorkflow" archive-workflow}))
  ([client workflow-id] (delete-workflow-with-client client workflow-id true)))

(defn delete-workflow
  "Takes a options,workflow-id and an optional archive-workflow boolean. Deletes the workflow execution.
  Returns nil"
  ([options workflow-id archive-workflow]
   (-> (generic-client options)
       (delete-workflow-with-client workflow-id archive-workflow)))
  ([options workflow-id] (delete-workflow options workflow-id true)))

(defn get-running-workflows-with-client
  "Takes a client,workflow-name and a version.
  Returns a list of running workflow ids"
  ([client wf-name options]
   (client (str "workflow/running/" wf-name)
           :method :get
           :query-params (-> (merge {:version 1} options)
                             walk/stringify-keys)))
  ([client wf-name] (get-running-workflows-with-client client wf-name {})))

(defn get-running-workflows
  "Takes options, workflow-name and a version.
  Returns a list of running workflow ids"
  ([options wf-name o-options]
   (-> (generic-client options)
       (get-running-workflows-with-client wf-name o-options)))
  ([options wf-name] (get-running-workflows options wf-name {})))

(defn pause-workflow-with-client
  "Takes a client and a workflow-id. Pauses the current workflow.
  Returns nil"
  [client workflow-id]
  (client (str "workflow/" workflow-id "/pause") :method :put))

(defn pause-workflow
  "Takes options and a workflow-id. Pauses the current workflow.
  Returns nil"
  [options workflow-id]
  (-> (generic-client options)
      (pause-workflow-with-client workflow-id)))

(defn resume-workflow-with-client
  "Takes a client and a workflow-id. Resumes a paused workflow.
  Returns nil"
  [client workflow-id]
  (client (str "workflow/" workflow-id "/resume") :method :put))

(defn resume-workflow
  "Takes options and a workflow-id. Resumes a paused workflow.
  Returns nil"
  [options workflow-id]
  (-> (generic-client options)
      (resume-workflow-with-client workflow-id)))

(defn skip-task-from-workflow-with-client
  "Takes a client a workflow-id and a task-reference-name. Will skip the task.
  Returns nil"
  [client workflow-id task-reference-name]
  (client (str "workflow/" workflow-id "/skiptask/" task-reference-name) :method :put))

(defn skip-task-from-workflow
  "Takes options a workflow-id and a task-reference-name. Will skip the task.
  Returns nil"
  [options workflow-id task-reference-name]
  (-> (generic-client options)
      (skip-task-from-workflow-with-client workflow-id task-reference-name)))

(defn rerun-workflow-with-client
  [client workflow-id rerun-req]
  (client (str "workflow/" workflow-id "/rerun") :method :post :body rerun-req))

(defn rerun-workflow
  [options workflow-id rerun-wf-request]
  (-> (generic-client options)
      (rerun-workflow-with-client workflow-id rerun-wf-request)))

(defn restart-workflow-with-client
  ([client workflow-id use-latest-definitions]
   (client (str "workflow/" workflow-id "/restart")
           :method :post
           :query-params {"useLatestDefinitions" use-latest-definitions}))
  ([client workflow-id] (restart-workflow-with-client client workflow-id false)))

(defn run-workflow-sync-with-client
  "Executes a workflow syncronously"
  ([client workflow-id version request-id wf-request wait-until-task-ref]
   (client (str "workflow/execute/" workflow-id "/" version) :method :post :body wf-request :query-params {:requestId request-id :waitUntilTaskRef wait-until-task-ref}))
  ([client workflow-id version request-id wf-request]
   (run-workflow-sync-with-client client workflow-id version request-id wf-request nil)))

(defn run-workflow-sync
  "Executes a workflow syncronously"
  ([options workflow-id version request-id wf-request wait-until-ref]
   (-> (generic-client options)
       (run-workflow-sync-with-client workflow-id version request-id wf-request wait-until-ref)))
  ([options workflow-id version request-id wf-request]
   (run-workflow-sync options workflow-id version request-id wf-request nil)))

(defn workflow-decide-with-client
  "Starts the decision task for a workflow"
  [client workflow-id]
  (client (str "workflow/decide/" workflow-id) :method :put))

(defn workflow-decide
  "Starts the decision task for a workflow"
  [options workflow-id]
  (-> (generic-client options)
      (workflow-decide-with-client workflow-id)))

(defn restart-workflow
  ([options workflow-id use-latest-definitions]
   (-> (generic-client options)
       (restart-workflow-with-client workflow-id use-latest-definitions)))
  ([options workflow-id] (restart-workflow options workflow-id false)))

(defn retry-last-failed-task-with-client
  ([client workflow-id resume-subworkflow-tasks]
   (client (str "workflow/" workflow-id "/retry")
           :method :post
           :query-params {"resumeSubWorkflowTasks" resume-subworkflow-tasks}))
  ([client workflow-id]
   (retry-last-failed-task-with-client client workflow-id false)))

(defn retry-last-failed-task
  ([options workflow-id resume-subworkflow-tasks]
   (-> (generic-client options)
       (retry-last-failed-task-with-client workflow-id
                                           resume-subworkflow-tasks)))
  ([options workflow-id] (retry-last-failed-task options workflow-id)))

(defn search-with-client
  [client query]
  (client "workflow/search"
          :method :get
          :query-params (-> (merge {:start 0, :size 100, :freeText "*"}
                                   query))))

(defn search
  [options query]
  (-> (generic-client options)
      (search-with-client query)))

(comment (def options
           {:app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d",
            :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean",
            :url "http://localhost:8080/api/"})

         (run-workflow-sync options "test_sync_workflow" 1 "arequest" {})

         (start-workflow options {:name  "with_wait"
                                  :input {}})
         ;; => "23d56593-e5f1-11ed-840e-32f1717a6621"

         (workflow-decide options  "23d56593-e5f1-11ed-840e-32f1717a6621")

         (def wf-id (start-workflow options {:name  "testing_super_workflow"
                                             :input {}
                                             :correlationId "some"}))
         (get-workflow options wf-id)
         (identity wf-id)
         (terminate-workflow options wf-id)

         (get-workflows options "testing_super_workflow" "some" {:includeClosed true :includeTasks true})

        ;; Needs re-testing
         (delete-workflow options "928ab4c5-2f86-4dd2-8c37-7c781c0087d5")

         (def client (generic-client options))
         (.getWorkflow client "8542dfe4-259b-4e65-99ca-4116a020524d" false)

         (get-workflow options "8542dfe4-259b-4e65-99ca-4116a020524d")
         (get-running-workflows options "testing_super_workflow")
         (pause-workflow options  "8542dfe4-259b-4e65-99ca-4116a020524d")
         (resume-workflow options  "8542dfe4-259b-4e65-99ca-4116a020524d")
         (skip-task-from-workflow options "e6cc9fbe-671b-4f42-80f9-13c1ada92db4" "create_dynamic_task_downloads_ref")
         (rerun-workflow options "e6cc9fbe-671b-4f42-80f9-13c1ada92db4" {:workflowInput {"test" "something"}})
         (restart-workflow options "e6cc9fbe-671b-4f42-80f9-13c1ada92db4" true)
         (retry-last-failed-task options "e6cc9fbe-671b-4f42-80f9-13c1ada92db4" true))
