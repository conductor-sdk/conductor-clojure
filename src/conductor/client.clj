(ns conductor.client
  (:import (com.netflix.conductor.client.worker Worker)
           (io.orkes.conductor.client.http OrkesTaskClient)
           (com.netflix.conductor.client.automator TaskRunnerConfigurer$Builder)
           (com.netflix.conductor.common.metadata.tasks TaskResult TaskResult$Status))

  (:require [clojure.tools.logging :as log]
            [conductor.mapper-utils :as mapperutils]
            [conductor.metadata :as metadata]))

(defn task-client
  "Returns an instance of TaskClient. when app-key and app-secret are provided
  Then returned instance will be Orkes Compatible"
  [{:keys [app-key app-secret url] :or {url "http://localhost:8080/api"}} ]
  (let [client (OrkesTaskClient. )]
    (.setRootURI client url)
    (when app-key
      (.withCredentials client app-key app-secret)
      (log/debug "Creating client with authentication"))
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
       (map mapperutils/clj-worker->Worker workers)
       (->> options (merge {:thread-count (count workers)}) :thread-count))
      (doto (.init))))
