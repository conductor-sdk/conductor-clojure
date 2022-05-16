
;;/*
;; * <p>
;; * Licensed under the Apache License, Version 2.0 (the "License"); you may
;; not
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
(ns io.orkes.task-resource
  (:require [io.orkes.api-client :refer [generic-client]]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]))

(defn task-client [options] (generic-client options "tasks"))

(defn get-task-logs-with-client
  [client task-ex-id]
  (client (str task-ex-id "/log") :method :get))

(defn get-task-logs
  [options task-ex-id]
  (-> (task-client options)
      (get-task-logs-with-client task-ex-id)))

(defn log-task-execution-details-with-client
  [client task-ex-id log-message]
  (client (str task-ex-id "/log") :method :post :body log-message))

(defn log-task-execution-details
  [options task-ex-id log-message]
  (-> (task-client options)
      (log-task-execution-details-with-client task-ex-id log-message)))

(defn get-task-details-with-client
  [client task-ex-id]
  (client task-ex-id :method :get))

(defn get-task-details
  [options task-ex-id]
  (-> (task-client options)
      (get-task-details-with-client task-ex-id)))

(defn update-task-with-client
  [client task-result]
  (client "" :method :post :body task-result))

(defn update-task
  [options task-result]
  (-> (task-client options)
      (update-task-with-client task-result)))

(defn batch-poll-tasks-by-type-with-client
  ([client task-type query-options]
   (client (str "poll/batch/" task-type)
           :method :get
           :query-params (-> (merge {:count 1, :timeout 100} query-options)
                             walk/stringify-keys)))
  ([client task-type]
   (batch-poll-tasks-by-type-with-client client task-type {})))

(defn batch-poll-tasks-by-type
  ([options task-type query-options]
   (-> (task-client options)
       (batch-poll-tasks-by-type-with-client task-type query-options)))
  ([options task-type] (batch-poll-tasks-by-type options task-type {})))

(comment
(def options
    {:app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d",
     :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean",
     :url "http://localhost:8080/api/"})

(get-task-details options "a09ee9d3-c393-4ef4-98c4-e47fb4f43597")
(get-task-logs options  "a09ee9d3-c393-4ef4-98c4-e47fb4f43597")
(log-task-execution-details options   "a09ee9d3-c393-4ef4-98c4-e47fb4f43597" "This is a message im logging")
(def original-task (get-task-details options "a09ee9d3-c393-4ef4-98c4-e47fb4f43597"))
(identity original-task)
(update-task options (merge original-task {:status "IN_PROGRESS"} ))
(batch-poll-tasks-by-type options "go_task_example" {})

  )
