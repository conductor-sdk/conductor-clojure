(ns io.orkes.taskrunner
  (:require [io.orkes.task-resource :as resource]
            [clojure.core.async :as a :refer [alt! chan close! thread go-loop]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn- execute-worker
  [{execute :execute worker-name :name} input-data]
  (try
    (let [execution-result  (execute input-data)]
      (log/info "Executing worker for " worker-name)
      (log/info "Wokflow executed returned status" (:status execution-result))
      execution-result)
    (catch Exception e
      (log/error "Error executing returning failed")
      {:logs [{"taskId" (:taskId input-data)
               "createdTime" 0
               "log" (str  "Error running worker " (.getMessage e))}]
       :status "FAILED"})))

(defn run-poll-routine
  [f]
  (let [;; out (chan) What should we do with the result
        exit-chan (chan)]
    (go-loop
     []
      (alt! (thread (f))
            ([result]
             (when result (log/info "Found work " result))
             (recur))
            exit-chan :stop))
    exit-chan))

(defn poll-for-work-execute-worker-with-client
  [client worker filters]
  (log/info "Polling for work")
  (if-some [maybe-work
            (resource/poll-for-task-type-with-client client (:name worker) filters)]
    (let [execution-result (execute-worker worker maybe-work)]
      (log/info "Running worker " worker)
      (resource/update-task-with-client
       client
       (merge {:workflowInstanceId (:workflowInstanceId maybe-work),
               :taskId (:taskId maybe-work),
               :outputData {}
               :status "COMPLETED"}
              execution-result)))
    nil))

(defn runner-executer-for-workers-with-client
  ([client workers thread-count filters]
   (let [shutdown-channels
         (flatten
          (mapv (fn [w]
                  (repeat thread-count
                          (run-poll-routine
                           #(poll-for-work-execute-worker-with-client client
                                                                      w
                                                                      filters))))
                workers))]
     (fn []  (doseq [c shutdown-channels]  (close! c)))))
  ([client workers thread-count] (runner-executer-for-workers-with-client client workers thread-count {}))
  ([client workers] (runner-executer-for-workers-with-client client workers 1 {})))

(defn runner-executer-for-workers
  ([options workers thread-count filters]
   (let [client (resource/task-client options)]
     (runner-executer-for-workers-with-client client
                                              workers
                                              thread-count
                                              filters)))
  ([options workers thread-count]
   (runner-executer-for-workers options workers thread-count {}))
  ([options workers] (runner-executer-for-workers options workers 1 {})))

(comment (def options
           {:app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d",
            :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean",
            :url "http://localhost:8080/api/"})
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
                       {:status "COMPLETED"
                        :outputData (:inputData d)})})

         (def stop-polling-fn (runner-executer-for-workers options [worker] 1))
         (stop-polling-fn)
         ;; (def worker-result (poll-for-work options worker {}))
         ;; (printer worker-result)
         ;; (close! (first worker-result) )
         ;; (close! (last worker-result))
         ;; (def worker-updater-fn (update-task-with-worker-result options
         ;; worker) )
         ;; (def result (worker-updater-fn (first worker-result) ))
         ;; (printer result)
         (def worker2
           {:name "cool_clj_task_x",
            :execute (fn [d]
                       ;; (Thread/sleep 1000)
                       (log/info "I got executed with the following params " d)
                       {:status "COMPLETED"
                        :outputData {"message" "Something silly"}})})
         (-> (apply (:execute worker) {:p 123})
             first
             name
             string/upper-case)
          ;; (worker-executor options worker)
         (def re (runner-executer-for-workers options [worker]))
         (re)
         (def constants
           {:get-user-info "get_user_info"
            :send-email "send_email"
            :send-sms "send_sms"
            :workflow-name "user_notifications"})
         (defn create-workers
           "Returns workers for the workflow"
           []
           [{:name (:get-user-info constants)
             :execute (fn [data]
                        {:status "COMPLETED"
                         :outputData {"email" (str (get-in data [:inputData "userId"]) "@email.com")}})}

            {:name (:send-email constants)
             :execute (fn [__]
                        {:status "COMPLETED"})}

            {:name (:send-sms constants)
             :execute (fn [__]
                        {:status "COMPLETED"})}])

;; (def interval-chan (set-interval #(worker-executor options worker)
         ;; 1000) )
         ;; (close! interval-chan)
         ;; (def interval-chan2 (set-interval #(worker-executor options worker2)
         ;; 1000) )
         ;; (close! interval-chan2)
         )
