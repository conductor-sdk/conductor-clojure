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
(ns conductor.client
  (:import (com.netflix.conductor.client.worker Worker)
           (io.orkes.conductor.client.http OrkesTaskClient OrkesWorkflowClient)
           (com.netflix.conductor.client.automator TaskRunnerConfigurer$Builder)
           (com.netflix.conductor.common.metadata.tasks TaskResult TaskResult$Status))

  (:require [clojure.tools.logging :as log]
            [conductor.mapper-utils :as mapperutils]
            [conductor.metadata :as metadata]))

(defn task-client
  "Returns an instance of TaskClient. when app-key and app-secret are provided
  Then returned instance will be Orkes Compatible"
  [{:keys [app-key app-secret url] :or {url "http://localhost:8080/api/"}} ]
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

