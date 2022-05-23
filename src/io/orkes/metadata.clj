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
(ns io.orkes.metadata
  (:require [io.orkes.api-client :refer [generic-client]]
            [cheshire.core :as json]))

(def authorization-header-key "X-AUTHORIZATION")
(def json-headers
  {"Content-Type" "application/json", "Accept" "application/json"})


(defn meta-client [options] (generic-client options "metadata"))

(defn get-workflow-def-using-client
  "Takes a client a name and a version. Will fetch for workflow definition"
  ([client name version]
   (client (str "workflow/" name) :query-params {"version" version}))
  ([client name] (get-workflow-def-using-client client name 1)))

(defn get-workflow-def
  "Takes a map of options, a name and a version. Will fetch for workflow definition"
  [options name version]
  (-> options
      (meta-client)
      (get-workflow-def-using-client name version)))


(defn register-workflow-def-using-client
  "Takes a client and a workflow definition in edn, will register a worflow in conductor"
  [client workflow]
  (client "workflow" :method :post :body workflow))

(defn register-workflow-def
  "Takes a map of options, and an EDN defined workflow. Will register a workflow"
  [options workflow]
  (-> options
      (meta-client)
      (register-workflow-def-using-client workflow)))

(defn update-workflows-def-using-client
  "takes a client and a list of workflows definition in edn, will update all workflows in list"
  [client workflows]
  (client "workflow" :method :put :body workflows))

(defn update-workflows-def
  "Takes a map of options, and a list of workflow definitions. will update every workflow on the list"
  [options workflows]
  (-> options
      (meta-client)
      (update-workflows-def-using-client workflows)))


(defn unregister-workflow-def-using-client
  "Takes a client a name and a version. will unregister workflow. returns nil on success"
  [client name version]
  (client (str "workflow/" name "/" version) :method :delete))

(defn unregister-workflow-def
  "Takes a map of options, a name and a version. will unregister workflow. returns nil on success"
  [options name version]
  (-> options
      (meta-client)
      (unregister-workflow-def-using-client name version)))

(defn register-tasks-using-client
  "Given a client instance and a list of tasks,
  will register the task in consuctor"
  [client tasks]
  (client "taskdefs" :method :post :body tasks))

(defn register-tasks
  "Takes options and a list of tasks in EDN, will register the tasks in conductor"
  [options tasks]
  (-> options
      (meta-client)
      (register-tasks-using-client tasks)))

(defn update-task-definition-with-client
  [client task-definition]
  (client "taskdefs" :method :put :body task-definition ) )

(defn update-task-definition
  "Takes a map of options, and a list of workflow definitions. will update every workflow on the list"
  [options task-definition]
  (-> options
      (meta-client)
      (update-task-definition-with-client task-definition)))

(defn get-task-def-with-client
  "Takes a client and a task-name. Returns a task definition"
  [client task-ref]
  (client (str "taskdefs/" task-ref) :method :get))

(defn get-task-def
  "Takes options and a task-definition name. Returns the task definition"
  [options task-def]
  (-> (meta-client options)
      (get-task-def-with-client task-def)))


(defn unregister-task-with-client
  "Takes a client and a task-name. Unregisters the task. Returns nil"
  [client task-ref]
  (client (str "taskdefs/" task-ref) :method :delete))

(defn unregister-task
  "Takes an options map and a task name. Unregisters the task. Returns nil"
  [options task-ref]
  (-> options
      (meta-client)
      (unregister-task-with-client task-ref)))

(comment
  (def options
    {:app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d",
     :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean",
     :url "http://localhost:8080/api/"})
  (def cool-b-task
    {:name "cool_clj_task_n",
     :description "some description",
     :ownerEmail "mail@gmail.com",
     :retryCount 3,
     :timeoutSeconds 300,
     :responseTimeoutSeconds 180})
  (def wf-sample
    {:name "cool_clj_workflow_2",
     :description "created programatically from clj",
     :version 1,
     :tasks [{:name "cool_clj_task_n",
              :taskReferenceName "cool_clj_task_ref",
              :inputParameters {},
              :type "SIMPLE"}
             {:name "something",
              :taskReferenceName "other",
              :inputParameters {},
              :type "FORK_JOIN",
              :forkTasks [[{:name "cool_clj_task_n",
                            :taskReferenceName "cool_clj_task_z_ref",
                            :inputParameters {},
                            :type "SIMPLE"}]]}
             {:name "join",
              :type "JOIN",
              :taskReferenceName "join_ref",
              :joinOn ["cool_clj_task_z" "cool_clj_task_x"]}],
     :inputParameters [],
     :outputParameters {"message" "${clj_prog_task_ref.output.:message}"},
     :schemaVersion 2,
     :restartable true,
     :ownerEmail "mail@yahoo.com",
     :timeoutSeconds 0,
     :timeoutPolicy "ALERT_ONLY"})

  (register-tasks options [cool-b-task])
  (register-workflow-def options wf-sample)
  (update-workflows-def options [wf-sample])
  (json/generate-string cool-b-task)
  (get-workflow-def options "simple_wf" 1)
  (def wf (get-workflow-def options "simple_wf" 1))
  (:tasks wf)
  (register-workflow-def options (assoc wf :version 29))
  (unregister-workflow-def options "cool_clj_workflow_2" 1)
  (def some-task (get-task-def options "cool_clj_task_b"))
  (update-task-definition options
                          (assoc cool-b-task :ownerEmail "othermaila@mail.com"))
  (unregister-task options "cool_clj_task_b")
  )
