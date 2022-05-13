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
(ns conductor.mapper-utils
  (:import
           (com.netflix.conductor.common.metadata.workflow WorkflowDef$TimeoutPolicy)
           (com.netflix.conductor.common.metadata.tasks TaskResult TaskResult$Status)
           (com.netflix.conductor.client.worker Worker))

  (:require [clojure.string :as string]
            [clojure.java.data :as j]
            [clojure.walk :as w]))

(defn camel->kebab [k]
  (->> (string/split (name k) #"(?<=[a-z])(?=[A-Z])")
       (map string/lower-case)
       (interpose \-)
       string/join
       keyword))

(defn kebab->camel [k]
  (keyword (string/replace (name k) #"-(\w)"
                          #(string/upper-case (second %1))) ))


(defn kebab->capitalizedash [w]
  (-> (string/upper-case w)
       (string/replace #"-" "_")))

(defn kebab-all-map-keys [o]
  (w/postwalk
              (fn [s] (if (keyword? s)(camel->kebab s)s))
              (j/from-java-deep o {:exceptions {:return true}}) ))

(defprotocol MapToClojure
  (->clj [o]))

(extend-protocol MapToClojure
  java.util.Map
  (->clj [o] (let [entries (.entrySet o)]
               (reduce (fn [m [^String k v]]
                         (assoc m (keyword k) (->clj v)))
                       {} entries)))

  java.util.List
  (->clj [o] (vec (map ->clj o)))

  java.lang.Object
  (->clj [o] o)

  ;; com.netflix.conductor.common.run.Workflow
  ;; (->clj [o] (kebab-all-map-keys o))

  ;; com.netflix.conductor.common.metadata.tasks.TaskDef
  ;; (->clj [o] (kebab-all-map-keys o))

  ;; com.netflix.conductor.common.metadata.workflow.WorkflowDef
  ;; (->clj [o](kebab-all-map-keys o))

  ;; com.netflix.conductor.common.model.BulkResponse
  ;; (->clj [o](kebab-all-map-keys o))

  ;; com.netflix.conductor.common.run.WorkflowSummary
  ;; (->clj [o](kebab-all-map-keys o))

  ;; com.netflix.conductor.common.run.SearchResult
  ;; (->clj [o](kebab-all-map-keys o))

  ;; com.netflix.conductor.common.metadata.tasks.Task
  ;; (->clj [o](kebab-all-map-keys o))

  nil
  (->clj [_] nil))

(defn java-map->clj
  [m]
  (->clj m))


;; (defn clj-task->TaskDef [{:keys [name description owner-email retry-count timeout-seconds response-timeout-seconds]}]
;;   (TaskDef. name description owner-email retry-count timeout-seconds response-timeout-seconds))


;; (defprotocol TaskTypeEnum
;;   (->task-type [tp] ))

;; (extend-protocol TaskTypeEnum
;;   String
;;   (->task-type [tp] (TaskType/valueOf tp) )
;;   clojure.lang.Keyword
;;   (->task-type [tp] (->task-type (kebab->capitalizedash (name tp)))))


;; (defn clj-task-type->TaskType [type]
;; (->task-type type))

;; (defn clj-workflow-task->WorkflowTask [{:keys [
;;                                                name task-reference-name description
;;                                                input-parameters type fork-tasks join-on
;;                                                decision-cases default-case loop-condition
;;                                                loop-over dynamic-fork-tasks-input-param-name dynamic-fork-tasks-param
;;                                                dynamic-task-name-param async-complete case-value-param
;;                                                ]}]
;;   (doto (WorkflowTask.)
;;     (.setName name)
;;     (.setTaskReferenceName task-reference-name)
;;     (.setDescription description)
;;     (.setInputParameters input-parameters)
;;     (.setType (.name (clj-task-type->TaskType type) ))
;;     (#(when fork-tasks (.setForkTasks % (map (fn [inner] (map clj-workflow-task->WorkflowTask inner)) fork-tasks))))
;;     (#(when join-on (.setJoinOn % join-on)))
;;     (#(when decision-cases (.setDecisionCases % (update-vals decision-cases (fn [dtasks] (map clj-workflow-task->WorkflowTask dtasks))))))
;;     (#(when default-case (.setDefaultCase % (map clj-workflow-task->WorkflowTask default-case))))
;;     (#(when loop-condition (.setLoopCondition % loop-condition)))
;;     (#(when loop-over (.setLoopOver % (map clj-workflow-task->WorkflowTask loop-over))))
;;     (#(when dynamic-fork-tasks-input-param-name (.setDynamicForkTasksInputParamName % dynamic-fork-tasks-input-param-name)))
;;     (#(when dynamic-fork-tasks-param (.setDynamicForkTasksParam % dynamic-fork-tasks-param)))
;;     (#(when dynamic-task-name-param (.setDynamicTaskNameParam % dynamic-task-name-param)))
;;     (#(when async-complete (.setAsyncComplete % async-complete)))
;;     (#(when case-value-param (.setCaseValueParam % case-value-param)))))

(defprotocol TimeoutPolicy
  (->timeout-policy [tp] ))

(extend-protocol TimeoutPolicy
  String
  (->timeout-policy [tp] (WorkflowDef$TimeoutPolicy/valueOf tp) )
  clojure.lang.Keyword
  (->timeout-policy [tp] (->timeout-policy (kebab->capitalizedash (name tp)))))


(defn timeout-policy->TimeoutPolicy [timeout-policy]
  (->timeout-policy timeout-policy))

;; (comment
;; (timeout-policy->TimeoutPolicy "TIME_OUT_WF")
;; (timeout-policy->TimeoutPolicy :time-out-wf)
;; ( WorkflowDef$TimeoutPolicy/valueOf "TIME_OUT_WF" )

;;   )

;; (defn clj-workflow->WorkflowDef
;;   [{:keys [name description version tasks
;;            input-parameters output-parameters schema-version
;;            restartable owner-email timeout-policy timeout-seconds] :or {restartable true, timeout-policy :alert-only }}]
;;   (doto (WorkflowDef.)
;;     (.setName name)
;;     (.setDescription description)
;;     (.setVersion version)
;;     (.setTasks (map clj-workflow-task->WorkflowTask tasks) )
;;     (.setInputParameters input-parameters)
;;     (.setOutputParameters output-parameters)
;;     (.setSchemaVersion schema-version)
;;     (.setRestartable restartable)
;;     (.setOwnerEmail owner-email)
;;     (.setTimeoutPolicy (timeout-policy->TimeoutPolicy timeout-policy))
;;     (.setTimeoutSeconds timeout-seconds))
;;   )

(defprotocol TaskResultStatus
  (->task-result-status [trs]))
(extend-protocol TaskResultStatus
  String
  (->task-result-status [tp] (TaskResult$Status/valueOf tp) )
  clojure.lang.Keyword
  (->task-result-status [tp] (->task-result-status (kebab->capitalizedash (name tp))))
  )

(defn status->task-result-status [s] (->task-result-status s))

(defn clj-worker->Worker
  "Returns a Worker instance for a worker provided in the form of a map"
  [worker]
  (reify Worker
    (getTaskDefName [__]
      (str (:name worker)))
    (execute [__ task]
      (let [task-result (TaskResult. task)
            input-data (.getInputData task)
            executor-fn (:execute worker)
            [result-status result-output-data] (executor-fn input-data)]
        (.setStatus task-result (status->task-result-status result-status))
        (.setOutputData task-result result-output-data)
        task-result))))

;; (defn clj-start-workflow-request->StartWorkflowRequest
;;   "Returns a Start workflow request"
;;   [{:keys [name version correlation-id external-input-payload-storage-path
;;                                                                 priority input task-domain workflow-def]}]
;;   (doto (StartWorkflowRequest.)
;;     (.setName name)
;;     (#(when version (.setVersion % (int version))))
;;     (#(when correlation-id (.setCorrelationId % correlation-id)))
;;     (#(when external-input-payload-storage-path (.setExternalInputPayloadStoragePath % external-input-payload-storage-path)))
;;     (#(when priority (.setPriority % priority)))
;;     (#(when input (.setInput % input)))
;;     (#(when task-domain (.setTaskDomain % task-domain)))
;;     (#(when workflow-def (.setWorkflowDef % (clj-workflow->WorkflowDef workflow-def))))))

;; (defn clj-rerun-workflow-request->RerunWorkflowRequest [rerun-workflow-request]
;;   (-> (j/to-java RerunWorkflowRequest (update-keys rerun-workflow-request kebab->camel))))

;; (defn clj-task-result->TaskResult [task-result]
;;   (-> (j/to-java TaskResult (update-keys task-result kebab->camel))))



;; (comment
;; (.getReRunFromWorkflowId (clj-rerun-workflow-request->RerunWorkflowRequest {:workflow-input {} :re-run-from-workflow-id "someId"}) )
;;   )
