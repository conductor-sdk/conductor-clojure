
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
            [clojure.string :as string]
            [clojure.walk :as walk]))

;; (defn task-client [options] (generic-client options "tasks"))

(defn get-task-logs-with-client
  "Get task execution logs"
  [client task-ex-id]
  (client (str task-ex-id "tasks/log") :method :get))

(defn get-task-logs
  "Get task execution logs"
  [options task-ex-id]
  (-> (generic-client options)
      (get-task-logs-with-client task-ex-id)))

(defn log-task-execution-details-with-client
  "Log task execution details"
  [client task-ex-id log-message]
  (client (str task-ex-id "tasks/log") :method :post :body log-message))

(defn log-task-execution-details
  "Log task execution details"
  [options task-ex-id log-message]
  (-> (generic-client options)
      (log-task-execution-details-with-client task-ex-id log-message)))

(defn get-queue-all-with-client [client] (client "tasks/queue/all" :method :get))

(defn get-queue-all
  [options]
  (-> (generic-client options)
      (get-queue-all-with-client)))

(defn get-task-details-with-client
  [client task-ex-id]
  (client task-ex-id :method :get))

(defn get-task-details
  [options task-ex-id]
  (-> (generic-client options)
      (get-task-details-with-client task-ex-id)))

(defn update-task-with-client
  [client task-result]
  (client "" :method :post :body task-result))

(defn update-task
  [options task-result]
  (-> (generic-client options)
      (update-task-with-client task-result)))

(defn get-externalstoragelocation-with-client
  [client path operation payload-type]
  (client "tasks/externalstoragelocation"
          :method :get
          :query-params
          {"path" path, "operation" operation, "payloadType" payload-type}))

(defn get-externalstoragelocation
  [options path operation payload-type]
  (-> (generic-client options)
      (get-externalstoragelocation-with-client path operation payload-type)))

(defn get-all-queue-polldata-with-client
  [client]
  (client "tasks/queue/polldata/all" :method :get))

(defn get-all-queue-polldata
  [options]
  (-> (generic-client options)
      (get-all-queue-polldata-with-client)))

(defn get-task-type-polldata-with-client
  [client task-type]
  (client "tasks/queue/polldata" :method :get :query-params {"taskType" task-type}))

(defn get-task-type-polldata
  [options task-type]
  (-> (generic-client options)
      (get-task-type-polldata-with-client task-type)))

(defn update-task-by-reference-name-with-client
  [client workflow-id task-reference-name status update-req]
  (client (str "tasks/" workflow-id "/" task-reference-name "/" status)
          :method :post
          :body update-req))

(defn update-task-by-reference-name
  [options workflow-id task-reference-name status update-req]
  (-> (generic-client options)
      (update-task-by-reference-name-with-client workflow-id
                                                 task-reference-name
                                                 status
                                                 update-req)))

(defn search-with-client
  ([client query-options]
   (client "tasks/search"
           :method :get
           :query-params (-> (merge {:start 0, :size 100, :freeText "*"}
                                    query-options)
                             walk/stringify-keys))
   ([client] (search-with-client client {}))))

(defn search
  ([options query-options]
   (-> (generic-client options)
       (search-with-client query-options)))
  ([options] (search options {})))

(defn requeue-pending-tasks-with-client
  [client task-type]
  (client (str "tasks/queue/requeue/" task-type) :method :post))

(defn requeue-pending-tasks
  [options task-type]
  (-> (generic-client options)
      (requeue-pending-tasks-with-client task-type)))

(defn get-task-type-queue-sizes-with-client
  ([client task-types]
   (client "tasks/queue/sizes" :method :get :query-params {"taskType" task-types}))
  ([client] (get-task-type-queue-sizes-with-client client [])))

(defn get-task-type-queue-sizes
  ([options task-types]
   (-> (generic-client options)
       (get-task-type-queue-sizes-with-client task-types)))
  ([options] (get-task-type-queue-sizes options [])))

(defn poll-for-task-type-with-client
  ([client task-type query-options]
   (client (str "tasks/poll/" task-type)
           :method :get
           :query-params (merge {"user" {}} query-options)))
  ([client task-type] (poll-for-task-type-with-client client task-type)))

(defn poll-for-task-type
  ([options task-type query-options]
   (-> (generic-client options)
       (poll-for-task-type-with-client task-type query-options)))
  ([options task-type] (poll-for-task-type options task-type {})))

(defn get-queue-details-with-client
  [client]
  (client "tasks/queue/all/verbose" :method :get))

(defn get-queue-details
  [options]
  (-> (generic-client options)
      (get-queue-details-with-client)))

(defn batch-poll-tasks-by-type-with-client
  ([client task-type query-options]
   (client (str "tasks/poll/batch/" task-type)
           :method :get
           :query-params (-> (merge {:count 1, :timeout 100} query-options)
                             walk/stringify-keys)))
  ([client task-type]
   (batch-poll-tasks-by-type-with-client client task-type {})))

(defn batch-poll-tasks-by-type
  ([options task-type query-options]
   (-> (generic-client options)
       (batch-poll-tasks-by-type-with-client task-type query-options)))
  ([options task-type] (batch-poll-tasks-by-type options task-type {})))

;; (defn task-runner-configurer [client workers thread-count]

;;   )

(comment (def options
           {:app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d",
            :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean",
            :url "http://localhost:8080/api/"})
         (get-task-details options "a09ee9d3-c393-4ef4-98c4-e47fb4f43597")
         (get-task-logs options "a09ee9d3-c393-4ef4-98c4-e47fb4f43597")
         (log-task-execution-details options
                                     "a09ee9d3-c393-4ef4-98c4-e47fb4f43597"
                                     "This is a message im logging")
         (def original-task
           (get-task-details options "a09ee9d3-c393-4ef4-98c4-e47fb4f43597"))
         (identity original-task)
         (update-task options (merge original-task {:status "IN_PROGRESS"}))
         (batch-poll-tasks-by-type options "cool_clj_task_b" {})
         (poll-for-task-type options "cool_clj_task_b")
         (def cool-b-task
           {:name "cool_clj_task_b",
            :description "some description",
            :ownerEmail "mail@gmail.com",
            :retryCount 3,
            :timeoutSeconds 300,
            :responseTimeoutSeconds 180})
         (def worker
           {:name "cool_clj_task_b",
            :execute (fn [d]
                       ;; (Thread/sleep 1000)
                       [:completed {"message" "Something silly"}])})
         (def worker2
           {:name "cool_clj_task_x",
            :execute (fn [d]
                       ;; (Thread/sleep 1000)
                       (log/info "I got executed with the following params " d)
                       [:failed {"message" "Something silly"}])})
         (-> (apply (:execute worker) {:p 123})
             first
             name
             string/upper-case)
         (poll-for-task-type options (:name worker) {"domain" "some_domain"}))
