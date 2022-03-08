(ns conductor-sdk.utils
  (:import
   (com.netflix.conductor.client.worker Worker)
   (com.netflix.conductor.common.metadata.tasks TaskResult TaskResult$Status)))


(defn status->task-result-status [status]
  (case status
    :in-progress TaskResult$Status/IN_PROGRESS
    :failed TaskResult$Status/FAILED
    :failed-with-terminal-error TaskResult$Status/FAILED_WITH_TERMINAL_ERROR
    :completed TaskResult$Status/COMPLETED))

(defn clj-worker->Worker
  "Returns a Worker instance for a worker provided in the form of a map"
  [worker]
  (reify Worker
    (getTaskDefName [this]
      (str (:name worker)))
    (execute [this task]
      (let [task-result (TaskResult. task)
            input-data (.getInputData task)
            executor-fn (:execute worker)
            [result-status result-output-data] (executor-fn input-data)]
        (.setStatus task-result (status->task-result-status result-status))
        (.setOutputData task-result result-output-data)
        task-result
        )
      )))
