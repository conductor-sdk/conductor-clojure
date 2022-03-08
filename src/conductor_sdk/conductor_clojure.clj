(ns conductor-sdk.conductor-clojure
  (:import (com.netflix.conductor.client.worker Worker)
           (io.orkes.conductor.client.http OrkesTaskClient)
           (com.netflix.conductor.client.automator TaskRunnerConfigurer$Builder)
           (com.netflix.conductor.common.metadata.tasks TaskResult TaskResult$Status))

  (:require [clojure.tools.logging :as log]
            [conductor-sdk.workers :as workers]))

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
  (def worker
    {
     :name "clj_task"
     :execute (fn [someData]
                (println "This is imput data" (str someData))
                [:completed {:message "Hi From Clj"}])
     })

  (def instance (runner-executor-for-workers
                 (list worker)
                 {
                  :url  "http://localhost:8080/api/"
                  :app-key "f082aa94-ba42-4f95-9d9f-c808b3fd7485"
                  :app-secret "JkSxYI8D8YIpcuNnSeBbPS9ug1rVPqii3Xia2nGRE1ICcepW"
                  :thread-count 2} ))
  (.shutdown instance)
  )
