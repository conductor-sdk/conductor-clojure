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

;; (defn meta-client [options] (generic-client options "metadata"))

(defn get-workflow-def-using-client
  "Takes a client a name and a version. Will fetch for workflow definition"
  ([client name version]
   (client (str "metadata/workflow/" name) :query-params {"version" version}))
  ([client name] (get-workflow-def-using-client client name 1))
  ([client] (client "metadata/workflow")))

(defn get-workflow-def
  "Takes a map of options, a name and a version. Will fetch for workflow definition"
  ([options name version]
   (-> options
       (generic-client)
       (get-workflow-def-using-client name version)))
  ([options]
   (-> options
       (generic-client)
       (get-workflow-def-using-client))))

(defn
  register-workflow-def-using-client
  "Takes a client and a workflow definition in edn, will register a worflow in conductor"
  ([client workflow overwrite]
   (client "metadata/workflow" :method :post :body workflow :query-params {"overwrite" overwrite}))
  ([client workflow] (register-workflow-def-using-client client workflow false)))

(defn register-workflow-def
  "Takes a map of options, and an EDN defined workflow. Will register a workflow"
  ([options workflow overwrite]
   (-> options
       (generic-client)
       (register-workflow-def-using-client workflow overwrite)))
  ([options workflow] (register-workflow-def options workflow false)))

(defn update-workflows-def-using-client
  "takes a client and a list of workflows definition in edn, will update all workflows in list"
  [client workflows]
  (client "metadata/workflow" :method :put :body workflows))

(defn update-workflows-def
  "Takes a map of options, and a list of workflow definitions. will update every workflow on the list"
  [options workflows]
  (-> options
      (generic-client)
      (update-workflows-def-using-client workflows)))

(defn unregister-workflow-def-using-client
  "Takes a client a name and a version. will unregister workflow. returns nil on success"
  [client name version]
  (client (str "metadata/workflow/" name "/" version) :method :delete))

(defn unregister-workflow-def
  "Takes a map of options, a name and a version. will unregister workflow. returns nil on success"
  [options name version]
  (-> options
      (generic-client)
      (unregister-workflow-def-using-client name version)))

(defn register-tasks-using-client
  "Given a client instance and a list of tasks,
  will register the task in consuctor"
  [client tasks]
  (client "metadata/taskdefs" :method :post :body tasks))

(defn register-tasks
  "Takes options and a list of tasks in EDN, will register the tasks in conductor"
  [options tasks]
  (-> options
      (generic-client)
      (register-tasks-using-client tasks)))

(defn update-task-definition-with-client
  [client task-definition]
  (client "metadata/taskdefs" :method :put :body task-definition))

(defn update-task-definition
  "Takes a map of options, and a list of workflow definitions. will update every workflow on the list"
  [options task-definition]
  (-> options
      (generic-client)
      (update-task-definition-with-client task-definition)))

(defn get-task-def-with-client
  "Takes a client and a task-name. Returns a task definition"
  [client task-ref]
  (client (str "metadata/taskdefs/" task-ref) :method :get))

(defn get-task-def
  "Takes options and a task-definition name. Returns the task definition"
  [options task-def]
  (-> (generic-client options)
      (get-task-def-with-client task-def)))

(defn unregister-task-with-client
  "Takes a client and a task-name. Unregisters the task. Returns nil"
  [client task-ref]
  (client (str "metadata/taskdefs/" task-ref) :method :delete))

(defn unregister-task
  "Takes an options map and a task name. Unregisters the task. Returns nil"
  [options task-ref]
  (-> options
      (generic-client)
      (unregister-task-with-client task-ref)))

(comment
  (def options
    {:app-key "1f8f740c-9117-4016-9cb8-c1d43ed75bb4",
     :app-secret "zR0kkWGx17HDNhH2zlfu2IrGtATlmnyQS6FrHlDZXriSsW7M",
     :url "http://localhost:8080/api/"})
  (count (get-workflow-def options))
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
             {:name "something_else",
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
  (register-workflow-def options wf-sample true)
  (update-workflows-def options [wf-sample])
  (json/generate-string cool-b-task)
  (spit "/tmp/testw.edn" (with-out-str (pr (get-workflow-def options "testing_loop_iterations" 1))))
  (def wf (get-workflow-def options "si" 1))
  (:tasks wf)
  (register-workflow-def options (assoc wf :version 29))
  (unregister-workflow-def options "cool_clj_workflow_2" 1)
  (def some-task (get-task-def options "cool_clj_task_b"))
  (update-task-definition options
                          (assoc cool-b-task :ownerEmail "othermaila@mail.com"))
  (unregister-task options "cool_clj_task_b"))
