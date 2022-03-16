(ns conductor.client
  (:import (com.netflix.conductor.client.worker Worker)
           (io.orkes.conductor.client.http OrkesTaskClient)
           (com.netflix.conductor.client.automator TaskRunnerConfigurer$Builder)
           (com.netflix.conductor.common.metadata.tasks TaskResult TaskResult$Status))

  (:require [clojure.tools.logging :as log]
            [conductor.workers :as workers]
            [conductor.metadata :as metadata]))

(defn task-client
  "Returns an instance of TaskClient. when app-key and app-secret are provided
  Then returned instance will be Orkes Compatible"
  [{:keys [app-key app-secret url] :or {url "http://localhost:8080/api"}} ]
  (let [client (OrkesTaskClient. )]
    (.setRootURI client url)
    (when app-key
      (.withCredentials client app-key app-secret)
      (log/debug "Creating client with authentication")
      )
    client))


(defn task-runner-configurer
  "Returns a TaskRunnerConfigurer instance for given client and workers"
  ([client workers thread-count]
   (log/debug "Creating TaskRunnerConfigurer with thread-count" thread-count)
   (-> (TaskRunnerConfigurer$Builder. client workers)
       (.withThreadCount thread-count)
       (.build)) )
  ([client workers] (task-runner-configurer client workers (count workers))))

(defn runner-executor-for-workers
  "Takes a list of workers and connection options. returnes an initiated
  TaskRunnerConfigurer instance"
  [workers options]
  (-> (task-client options)
      (task-runner-configurer
       (map workers/clj-worker->Worker workers)
       (->> options (merge {:thread-count (count workers)}) :thread-count))
      (doto (.init))))


(comment
;; Given the options create-task and create-workflow
(def options {
                  :url  "http://localhost:8080/api/"
                  :app-key "e544d3d7-7680-42c5-ae0a-2408d395533d"
                  :app-secret "mYBkGm3RvWbRuFZlIShLyk8iu4IYxyhtxnrq0WEUWptqz8Id"
              } )
;; Programatically Create a task
(metadata/register-tasks options [{
                         :name "cool_clj_task"
                         :description "some description"
                         :owner-email "mail@gmail.com"
                         :retry-count 3
                         :timeout-seconds 300
                                   :response-timeout-seconds 180 }])
;; Programatically create a workflow
(metadata/register-workflow-def options {
                                              :name "cool_clj_workflow"
                                              :description "created programatically from clj"
                                              :version 1
                                              :tasks [ {
                                                       :name "cool_clj_task"
                                                       :task-reference-name "cool_clj_task_ref"
                                                       :input-parameters {}
                                                       :type :simple
                                                       } ]
                                              :input-parameters []
                                              :output-parameters {:message "${clj_prog_task_ref.output.:message}"}
                                              :schema-version 2
                                              :restartable true
                                              :owner-email "mail@yahoo.com"
                                              :timeout-seconds 0
                                         })

;; Programatically create a worker and run it to pool
(def instance (runner-executor-for-workers
               (list {
                      :name "cool_clj_task"
                      :execute (fn [someData]
                                 [:completed {:message "Hi From Clj i was created programatically"}])
                      })
               options ))
(.shutdown instance)

)
