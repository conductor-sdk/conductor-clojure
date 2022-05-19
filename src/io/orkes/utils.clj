(ns io.orkes.utils
  (:require [clojure.spec.alpha :as s]))

(s/def :task/name string?)
(s/def :task/taskReferenceName string?)
(s/def :task/inputParameters map?)
(s/def :task/forkTasks list?) ;; TODO it has to be a list of list of tasks
(s/def :task/joinOn list?);; TODO list of strings with no spaces
(s/def :task/loopCondition string?);; TODO non empty string
(s/def :task/loopOver list?);; TODO list of tasks
(s/def :task/decisionCases map?)
(s/def :task/defaultCase list?)
(s/def :task/evaluatorType string?);;TODO non empty string
(s/def :task/expression string?);; TODO non empty
(s/def :task/type string?);; TODO non empty
(s/def :task/sink string?)
(s/def :task/dynamicForkTasksParam string?)
(s/def :task/dynamicForkTasksInputParamName string?)
(s/def :task/subWorkflowParam map?)

(s/def :task/simple-task
  (s/keys :req [:task/name :task/taskReferenceName :task/inputParameters :task/type]
          :opt []))

(s/def :task/fork-join
  (s/keys :req [:task/name :task/taskReferenceName :task/inputParameters :task/forkTasks :task/type]
          :opt []))

(s/def :task/do-while-schema
  (s/keys :req [:task/name :task/taskReferenceName :task/inputParameters :task/type :task/loopOver :task/loopCondition]
          :opt []))

(s/def :task/event-task
  (s/keys :req [:task/name :task/taskReferenceName :task/inputParameters :task/type :task/sink]
          :opt []))

(s/def :task/join-task
  (s/keys :req [:task/name :task/taskReferenceName :task/inputParameters :task/type :task/joinOn]
          :opt []))

(s/def :task/wait-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type]
          :opt []))

(s/def :task/fork-join-dynamic-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type :task/inputParameters :task/dynamicForkTasksParam :task/dynamicForkTasksInputParamName]
          :opt []))

(s/def :task/dynamic-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type]
          :opt []))

(s/def :task/inline-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type]
          :opt []))

(s/def :task/switch-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type :task/evaulatorType :task/expression :task/decisionCases :task/defaultCase]
          :opt []))

(s/def :task/kafka-request-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type :task/inputParameters]
          :opt []))

(s/def :task/http-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type :task/inputParameters]
          :opt []))

(s/def :task/json-jq-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type :task/inputParameters]
          :opt []))

(s/def :task/terminate-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type]
          :opt []))

(s/def :task/set-variable-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type :task/inputParameters]
          :opt []))

(s/def :task/sub-workflow-param-task
  (s/keys :req [:task/name :task/taskReferenceName :task/type :task/inputParameters :task/subWorkflowParam]
          :opt []))

(defn- name-task-reference
  [task-name]
  {:name task-name, :task-reference-name (str task-name "-ref")});; TODO replace spaces for dashes

(defn- generic-task
  ([task-name req-props additional]
   (-> (name-task-reference task-name)
       (merge req-props additional)))
  ([task-name req-props additional spec]
   (s/assert spec (generic-task task-name req-props additional))))

(defn simple-task
  [{task-name :name, :as task-properties}]
  (generic-task task-name
                {:type "SIMPLE" :input-parameters {}}
                task-properties :task/simple-task))

(defn fork-join
  [{task-name :name, :as task-properties}]
   (generic-task task-name {:type :fork-join :input-parameters {} :fork-tasks []} task-properties :task/fork-join))

(defn join
  [{task-name :name, :as task-properties}]
   (generic-task task-name {:type :join, :join-on []} task-properties))

(defn fork-join-join [{task-name :name, :as task-properties}]
  (let [fork-join-task (fork-join task-properties)
        fork-tasks-names (map #(-> %
                                   :task-reference-name)
                              (:fork-tasks fork-join-task)
                           )]
    [fork-join-task (join  {:name (str task-name "-join") :join-on fork-tasks-names})]))

(defn do-while
  [{task-name :name, :as task-properties}]
   (generic-task task-name {:type :do-while,
               :loop-condition ""
               :input-parameters {},
               :loop-over []}
        task-properties))

(defn wait
  [{task-name :name, :as task-properties}]
  (generic-task task-name {:type :wait} task-properties))

(defn switch
  [{task-name :name, :as task-properties}]
  (generic-task task-name {:type :switch,
               :input-parameters {},
               :decision-cases {}
               :default-case [],
               :evaluator-type ""
               :expression ""} task-properties))

(defn dynamic-fork
[{task-name :name, :as task-properties}]
  (generic-task task-name {
                                :input-parameters {}
                                :dynamic-fork-tasks-param ""
                                :dynamic-fork-tasks-input-param-name ""
                                :type :fork-join-dynamic
                                } task-properties)
  )

(defn terminate
  [{task-name :name, :as task-properties}]
  (generic-task task-name
                {:type :terminate,
                 :input-parameters {"terminationStatus" "COMPLETED"}}
                task-properties))


(defmulti dilatory-task (fn [{task-type :type}] task-type))
(defmethod dilatory-task :simple [m] (simple-task m))

(defmethod dilatory-task :fork-join [m] (fork-join m))
;; (defmethod dilatory-task :fork-join-join
;;   [m]
;;   (fork-join-join m))
(defmethod dilatory-task :do-while [m] (do-while m))
(defmethod dilatory-task :dynamic-fork [m] (wait m))
(defmethod dilatory-task :switch [m] (switch m))
(defmethod dilatory-task :terminate [m] (terminate m))
(defmethod dilatory-task :wait [m] (wait m))



(defmulti task-map (fn [{task-type :type} __] task-type))

(defmethod task-map :default [m mfn] (mfn m))

(defmethod task-map :fork-join
  [m mfn]
  (-> (mfn m)
    (update-in [:fork-tasks]
               #(map (fn [iv] (map (fn [im] (task-map im mfn)) iv)) %))))

;; (defmethod task-map :fork-join-join
;;   [m]
;;   (fork-join-join m))
(defmethod task-map :do-while
  [m mfn]
  (-> (mfn m)
      (update-in [:loop-over] #(map (fn [im] (task-map im mfn)) %))))
(defmethod task-map :switch
  [m mfn]
  (-> (mfn m)
      (update-in
        [:switch-case]
        #(update-vals % (fn [tvals] (map (fn [im] (task-map im mfn)) tvals))))
      (update-in
        [:default-case]
        #(update-vals % (fn [tvals] (map (fn [im] (task-map im mfn)) tvals))))))

(defn workflow
  ([name other-properties] (-> (merge {:name name, :version 1 :tasks []} other-properties)
                               (update-in [:tasks] #(map (fn [t] (task-map dilatory-task)) %))))
  ([name] (workflow name {})))

(comment
  (workflow
  "some-wf"
  {:version 1,
   :tasks
     (into []
           (map dilatory-task
             [{:name "loop",
               :type :do-while,
               :loop-over
                 (map dilatory-task
                   [{:name "submit-to-solid", :type :simple}
                    {:name "loop", :type :wait}
                    {:name "did-solid-accept",
                     :type :switch,
                     :switch-case
                       {"Yes" (dilatory-task {:name "submit-acceptance",
                                              :type :simple}),
                        "need_more_docs" (dilatory-task {:name "need_more_docs",
                                                         :type :simple})},
                     :default-case [(dilatory-task {:name "solid-refejection",
                                                    :type :terminate})]}])}]))})
    (dilatory-task {:type :simple :name "test"})

    (task-map {:name "did-solid-accept",
                     :type :switch,
                     :switch-case
                       {"Yes" [{:name "submit-acceptance",
                                              :type :simple} ],
                        "need_more_docs" [ {:name "need_more_docs",
                                                         :type :simple} ]},
                     :default-case [{:name "solid-refejection",
                                                    :type :terminate}]}
              dilatory-task)

    (task-map {:name "loop", :type :wait} dilatory-task)

(workflow
  ""
  {:version 1,
   :tasks {:name "loop",
               :type :do-while,
               :loop-over
                   [{:name "submit-to-solid", :type :simple}
                    {:name "wait-for-solid-response", :type :wait}
                    {:name "did-solid-accept",
                     :type :switch,
                     :switch-case
                       {"Yes" [{:name "submit-acceptance",
                                              :type :simple}],
                        "need_more_docs" [{:name "need_more_docs",
                                                         :type :simple}]},
                     :default-case [{:name "solid-refejection",
                                                    :type :terminate}]}]}
     })



  )
