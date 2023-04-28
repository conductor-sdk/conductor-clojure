(ns io.orkes.sdk)

(defn workflow [name tasks]
  {:name name
   :tasks tasks
   :version 1
   :inputParameters []
   :timeoutSeconds 0})

(defn- b-task [task-reference-name type rest-params]
  (merge {:name task-reference-name
          :taskReferenceName task-reference-name
          :type type} rest-params))

;; WAIT task

(defn- wait-task [task-reference-name rest]
  (b-task task-reference-name "WAIT" rest))

(defn wait-task-until [task-reference-name until]
  (wait-task task-reference-name {:inputParameters {:until until}}))

(defn wait-task-duration [task-reference-name duration]
  (wait-task task-reference-name {:inputParameters {:duration duration}}))
;;
;; END WAIT TASK

(defn terminate-task ([task-reference-name status terminationReason]
                      (b-task task-reference-name "TERMINATE" {:inputParameters {:terminationStatus status
                                                                                 :terminationReason terminationReason}}))

  ([task-reference-name status] (terminate-task task-reference-name status nil)))

(defn switch-task ([task-reference-name expression decision-cases default-case]
                   (b-task task-reference-name "SWITCH" {:inputParameters {:switchCaseValue expression}
                                                         :expression "switchCaseValue"
                                                         :defaultCase default-case
                                                         :decisionCases decision-cases})))

(defn subworkflow-task
  ([task-reference-name workflow-name version]
   (b-task task-reference-name "SUB_WORKFLOW" {:subWorfklowParam {:name workflow-name :version version}}))
  ([task-reference-name workflow-name]  (b-task task-reference-name "SUB_WORKFLOW" {:subWorfklowParam {:name workflow-name}})))

(defn simple-task [task-reference-name name input-parameters]
  (b-task task-reference-name "SIMPLE" {:name name :inputParameters input-parameters}))

(defn set-variable-task [task-reference-name input-parameters] (b-task task-reference-name "SET_VARIABLE" {:inputParameters input-parameters}))

(defn kafka-publish-task [task-reference-name kafka-request]
  (b-task task-reference-name "KAFKA_PUBLISH" {:inputParameters {:kafka_request kafka-request}}))

(defn json-jq-task
  ([task-reference-name script rest-parameters]
   (b-task task-reference-name "JSON_JQ_TRANSFORM" {:inputParameters (merge rest-parameters {:queryExpression script})}))
  ([task-reference-name script] (json-jq-task task-reference-name script {})))

(defn join-task [task-reference-name join-on]
  (b-task task-reference-name "JOIN" {:joinOn join-on}))

(defn inline-task
  ([task-reference-name script additional-params evaulator-type]
   (b-task task-reference-name "INLINE" {:inputParameters (merge additional-params {:evaluatorType evaulator-type :expression script})}))
  ([task-reference-name script additional-params] (inline-task task-reference-name script additional-params "graaljs"))
  ([task-reference-name script] (inline-task task-reference-name script {})))

(defn http-task
  ([task-reference-name input-parameters] (b-task task-reference-name "HTTP" {:inputParameters {"http_request" input-parameters}})))

(defn fork-task
  ([task-reference-name fork-tasks] (b-task task-reference-name "FORK_JOIN" {:forkTasks fork-tasks})))

(defn fork-task-join [task-reference-name fork-tasks] (vector (fork-task task-reference-name fork-tasks) (join-task (str task-reference-name "_join") [])))

;; event task

(defn event-task [task-reference-name event-prefix event-suffix]
  (b-task task-reference-name "EVENT" {:sink (str event-prefix ":" event-suffix)}))

(defn sqs-event-task [task-reference-name queue-name]
  (event-task task-reference-name "sqs" queue-name))

(defn conductor-event-task [task-reference-name event-name] (event-task task-reference-name "conductor" event-name))

(defn dynamic-fork-task [task-reference-name pre-fork-tasks dynamic-tasks-input] (b-task task-reference-name "FORK_JOIN_DYNAMIC" {:inputParameters {:dynamicTasks pre-fork-tasks
                                                                                                                                                    :dynamicTasksInput dynamic-tasks-input}
                                                                                                                                  :dynamicForkTasksParam "dynamicTasks"
                                                                                                                                  :dynamicForkInputParameters "dynamicTasksInput"}))
(defn do-while-task [task-reference-name termination-condition tasks input-parameters]
  (b-task task-reference-name "DO_WHILE" {:loopCondition termination-condition
                                          :inputParameters input-parameters
                                          :loopOver tasks}))

(defn loop-for-conidtion [task-reference-name value-key] (str "if ( $." task-reference-name "['iteration'] < $." value-key ") {true;} else {false;}"))

(defn new-loop-task [task-reference-name iterations tasks] (do-while-task task-reference-name (loop-for-conidtion task-reference-name "value") tasks {:value iterations}))

(comment
  (workflow "my_workflow" [])
;; =>
;; {:name "my_workflow",
;;     :tasks [],
;;     :version 1,
;;     :inputParameters [],
;;     :timeoutSeconds 0}
  (wait-task-until "wait-until-example" "2023-04-12 00:06 GMT-03:00")
;; =>
;; {:name "wait-until-example",
;;     :taskReferenceName "wait-until-example",
;;     :type "WAIT",
;;     :inputParameters {:until "2023-04-12 00:06 GMT-03:00"}}
  (wait-task-duration "wait-task-duration" "1 days 1 minutes 1 seconds")
;; =>
;; {:name "wait-task-duration",
;;     :taskReferenceName "wait-task-duration",
;;     :type "WAIT",
;;     :inputParameters {:duration "1 days 1 minutes 1 seconds"}}
  (terminate-task "terminate-task" "COMPLETED" "terminationReason")
;; =>
;; {:name "terminate-task",
;;     :taskReferenceName "terminate-task",
;;     :type "TERMINATE",
;;     :inputParameters
;;     {:terminationStatus "COMPLETED", :terminationReason "terminationReason"}}
  (terminate-task "terminate-task" "COMPLETED")
;; => {:name "terminate-task",
;;     :taskReferenceName "terminate-task",
;;     :type "TERMINATE",
;;     :inputParameters {:terminationStatus "COMPLETED", :terminationReason nil}}
  (switch-task "switch-task-ref" "true" {"case1" [] "case2" []} [])
;; =>
;; {:name "switch-task-ref",
;;     :taskReferenceName "switch-task-ref",
;;     :type "SWITCH",
;;     :inputParameters {:switchCaseValue "true"},
;;     :expression "switchCaseValue",
;;     :defaultCase [],
;;     :decisionCases {"case1" [], "case2" []}}
  (subworkflow-task "sub-workflow-rf" "my-workflow" 1)
;; =>
;; {:name "sub-workflow-rf",
;;     :taskReferenceName "sub-workflow-rf",
;;     :type "SUB_WORKFLOW",
;;     :subWorfklowParam {:name "my-workflow", :version 1}}
  (simple-task "task-ref-name" "name" {"myinput" "param"})
;; =>
;; {:name "name",
;;     :taskReferenceName "task-ref-name",
;;     :type "SIMPLE",
;;     :inputParameters {"myinput" "param"}}
  (set-variable-task "task-ref-name" {"input" "value"})
;; =>
;; {:name "task-ref-name",
;;     :taskReferenceName "task-ref-name",
;;     :type "SET_VARIABLE",
;;     :inputParameters {"input" "value"}}
  (kafka-publish-task "kafka-ref-task" {"topic" "userTopic" "value" "message to publish" "bootStrapServers" "localhost:9092" "headers" {"X-Auth" "Auth-key"} "key" {"key" "valuekey"} "keySerializer" "org.apache.kafka.common.serialization.IntegerSerializer"})
;; =>
;; {:name "kafka-ref-task",
;;     :taskReferenceName "kafka-ref-task",
;;     :type "KAFKA_PUBLISH",
;;     :inputParameters
;;     {:kafka_request
;;      {"topic" "userTopic",
;;       "value" "message to publish",
;;       "bootStrapServers" "localhost:9092",
;;       "headers" {"X-Auth" "Auth-key"},
;;       "key" {"key" "valuekey"},
;;       "keySerializer" "org.apache.kafka.common.serialization.IntegerSerializer"}}}
  (json-jq-task "json-ref-task" ".persons | map({user:{email,id}})" {"persons" [{"name" "jim" "id" 1 "email" "jim@email.com"}]})
;; =>
;; {:name "json-ref-task",
;;     :taskReferenceName "json-ref-task",
;;     :type "JSON_JQ_TRANSFORM",
;;     :inputParameters
;;     {"persons" [{"name" "jim", "id" 1, "email" "jim@email.com"}],
;;      :queryExpression ".persons | map({user:{email,id}})"}}
  (join-task "join-task-ref" [])
;; =>
;; {:name "join-task-ref",
;;     :taskReferenceName "join-task-ref",
;;     :type "JOIN",
;;     :joinOn []}
  (inline-task "inline-task-ref" "(function(){ return $.value1 + $.value2;})()" {"value1" 2 "value2" 3})
;; =>
;; {:name "inline-task-ref",
;;     :taskReferenceName "inline-task-ref",
;;     :type "INLINE",
;;     :inputParameters
;;     {"value1" 2,
;;      "value2" 3,
;;      :evaluatorType "graaljs",
;;      :expression "(function(){ return $.value1 + $.value2;})()"}}
  (http-task "http-task-ref" {:uri "https://orkes-api-tester.orkesconductor.com/api" :method "GET" :connectionTimeout 3000})
;; =>
;; {:name "http-task-ref",
;;     :taskReferenceName "http-task-ref",
;;     :type "HTTP",
;;     :inputParameters
;;     {"http_request"
;;      {:uri "https://orkes-api-tester.orkesconductor.com/api",
;;       :method "GET",
;;       :connectionTimeout 3000}}}
  (fork-task "fork-task-ref" [])
;; =>
;; {:name "fork-task-ref",
;;     :taskReferenceName "fork-task-ref",
;;     :type "FORK_JOIN",
;;     :forkTasks []}
;;
  (fork-task-join "fork-task-ref" [])
;; =>
;; [{:name "fork-task-ref",
;;      :taskReferenceName "fork-task-ref",
;;      :type "FORK_JOIN",
;;      :forkTasks []}
;;     {:name "fork-task-ref_join",
;;      :taskReferenceName "fork-task-ref_join",
;;      :type "JOIN",
;;      :joinOn []}]
  (event-task "event-task-ref" "prefix" "suffix")
;; =>
;; {:name "event-task-ref",
;;     :taskReferenceName "event-task-ref",
;;     :type "EVENT",
;;     :sink "prefix:suffix"}
  (sqs-event-task "sqs-event-ref" "someQueue")
;; =>
;; {:name "sqs-event-ref",
;;     :taskReferenceName "sqs-event-ref",
;;     :type "EVENT",
;;     :sink "sqs:someQueue"}
  (conductor-event-task "conductor-event-task" "some-event")
;; =>
 ;; {:name "conductor-event-task",
 ;;     :taskReferenceName "conductor-event-task",
 ;;     :type "EVENT",
 ;;     :sink "conductor:some-event"}
  (dynamic-fork-task "dynamic-task-ref" "" "")
;; =>
;; {:name "dynamic-task-ref",
;;     :taskReferenceName "dynamic-task-ref",
;;     :type "FORK_JOIN_DYNAMIC",
;;     :inputParameters {:dynamicTasks "", :dynamicTasksInput ""},
;;     :dynamicForkTasksParam "dynamicTasks",
;;     :dynamicForkInputParameters "dynamicTasksInput"}
  (do-while-task "do-while-ref" "" [] {})
;; =>
;; {:name "do-while-ref",
;;     :taskReferenceName "do-while-ref",
;;     :type "DO_WHILE",
;;     :loopCondition "",
;;     :inputParameters {},
;;     :loopOver []}
  (new-loop-task "loop-sample-ref" 3 [])
;; =>
;; {:name "loop-sample-ref",
;;     :taskReferenceName "loop-sample-ref",
;;     :type "DO_WHILE",
;;     :loopCondition
;;     "if ( $.loop-sample-ref['iteration'] < $.value) {true;} else {false;}",
;;     :inputParameters {:value 3},
;;     :loopOver []}
  )
