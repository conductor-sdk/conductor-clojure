(ns io.orkes.sdk-test
  (:require [io.orkes.sdk :as sut]
            [clojure.test :as t]))
(t/deftest sdk-factroy-functions
  (t/testing "Should create a workflow with default params"
    (t/is (= (sut/workflow "my_workflow" [])
             {:name "my_workflow",
              :tasks [],
              :version 1,
              :inputParameters [],
              :timeoutSeconds 0})))

  (t/testing "Should create a wait until task"
    (t/is (= (sut/wait-task-until "wait-until-example" "2023-04-12 00:06 GMT-03:00")
             {:name "wait-until-example",
              :taskReferenceName "wait-until-example",
              :type "WAIT",
              :inputParameters {:until "2023-04-12 00:06 GMT-03:00"}})))

  (t/testing "Should create a wait duration task"
    (t/is (= (sut/wait-task-duration "wait-task-duration" "1 days 1 minutes 1 seconds")
             {:name "wait-task-duration",
              :taskReferenceName "wait-task-duration",
              :type "WAIT",
              :inputParameters {:duration "1 days 1 minutes 1 seconds"}})))

  (t/testing "Should create a terminate task"
    (t/is (= (sut/terminate-task "terminate-task" "COMPLETED" "terminationReason")
             {:name "terminate-task",
              :taskReferenceName "terminate-task",
              :type "TERMINATE",
              :inputParameters
              {:terminationStatus "COMPLETED", :terminationReason "terminationReason"}})))

  (t/testing "Should create a terminate task"
    (t/is (= (sut/terminate-task "terminate-task" "COMPLETED" "terminationReason")
             {:name "terminate-task",
              :taskReferenceName "terminate-task",
              :type "TERMINATE",
              :inputParameters
              {:terminationStatus "COMPLETED", :terminationReason "terminationReason"}})))

  (t/testing "Should create a switch task"
    (t/is (= (sut/switch-task "switch-task-ref" "true" {"case1" [] "case2" []} [])
             {:name "switch-task-ref",
              :taskReferenceName "switch-task-ref",
              :type "SWITCH",
              :inputParameters {:switchCaseValue "true"},
              :evaluatorType "value-param"
              :expression "switchCaseValue",
              :defaultCase [],
              :decisionCases {"case1" [], "case2" []}})))

  (t/testing "Should create a subworkflow task"
    (t/is (= (sut/subworkflow-task "sub-workflow-rf" "my-workflow" 1)
             {:name "sub-workflow-rf",
              :taskReferenceName "sub-workflow-rf",
              :type "SUB_WORKFLOW",
              :subWorfklowParam {:name "my-workflow", :version 1}})))

  (t/testing "Should create a simple-task task"
    (t/is (= (sut/simple-task "task-ref-name" "name" {"myinput" "param"})
             {:name "name",
              :taskReferenceName "task-ref-name",
              :type "SIMPLE",
              :inputParameters {"myinput" "param"}})))

  (t/testing "Should create a set-variable-task task"
    (t/is (= (sut/set-variable-task "task-ref-name" {"input" "value"})
             {:name "task-ref-name",
              :taskReferenceName "task-ref-name",
              :type "SET_VARIABLE",
              :inputParameters {"input" "value"}})))

  (t/testing "Should create a kafka-ref-task"
    (t/is (= (sut/kafka-publish-task "kafka-ref-task" {"topic" "userTopic" "value" "message to publish" "bootStrapServers" "localhost:9092" "headers" {"X-Auth" "Auth-key"} "key" {"key" "valuekey"} "keySerializer" "org.apache.kafka.common.serialization.IntegerSerializer"})
             {:name "kafka-ref-task",
              :taskReferenceName "kafka-ref-task",
              :type "KAFKA_PUBLISH",
              :inputParameters
              {:kafka_request
               {"topic" "userTopic",
                "value" "message to publish",
                "bootStrapServers" "localhost:9092",
                "headers" {"X-Auth" "Auth-key"},
                "key" {"key" "valuekey"},
                "keySerializer" "org.apache.kafka.common.serialization.IntegerSerializer"}}})))

  (t/testing "Should create a JSON_JQ task"
    (t/is (= (sut/json-jq-task "json-ref-task" ".persons | map({user:{email,id}})" {"persons" [{"name" "jim" "id" 1 "email" "jim@email.com"}]})
             {:name "json-ref-task",
              :taskReferenceName "json-ref-task",
              :type "JSON_JQ_TRANSFORM",
              :inputParameters
              {"persons" [{"name" "jim", "id" 1, "email" "jim@email.com"}],
               :queryExpression ".persons | map({user:{email,id}})"}})))

  (t/testing "Should create a JOIN task"
    (t/is (= (sut/join-task "join-task-ref" [])
             {:name "join-task-ref",
              :taskReferenceName "join-task-ref",
              :type "JOIN",
              :joinOn []})))

  (t/testing "Should create a INLINE task"
    (t/is (= (sut/inline-task "inline-task-ref" "(function(){ return $.value1 + $.value2;})()" {"value1" 2 "value2" 3})
             {:name "inline-task-ref",
              :taskReferenceName "inline-task-ref",
              :type "INLINE",
              :inputParameters
              {"value1" 2,
               "value2" 3,
               :evaluatorType "graaljs",
               :expression "(function(){ return $.value1 + $.value2;})()"}})))

  (t/testing "Should create a HTTP task"
    (t/is (= (sut/http-task "http-task-ref" {:uri "https://orkes-api-tester.orkesconductor.com/api" :method "GET" :connectionTimeout 3000})
             {:name "http-task-ref",
              :taskReferenceName "http-task-ref",
              :type "HTTP",
              :inputParameters
              {"http_request"
               {:uri "https://orkes-api-tester.orkesconductor.com/api",
                :method "GET",
                :connectionTimeout 3000}}})))

  (t/testing "Should create a fork task"
    (t/is (= (sut/fork-task "fork-task-ref" [])
             {:name "fork-task-ref",
              :taskReferenceName "fork-task-ref",
              :type "FORK_JOIN",
              :forkTasks []})))

  (t/testing "Should create a fork-join tasks"
    (t/is (= (sut/fork-task-join "fork-task-ref" [])
             [{:name "fork-task-ref",
               :taskReferenceName "fork-task-ref",
               :type "FORK_JOIN",
               :forkTasks []}
              {:name "fork-task-ref_join",
               :taskReferenceName "fork-task-ref_join",
               :type "JOIN",
               :joinOn []}])))

  (t/testing "Should create an event task"
    (t/is (= (sut/event-task "event-task-ref" "prefix" "suffix")
             {:name "event-task-ref",
              :taskReferenceName "event-task-ref",
              :type "EVENT",
              :sink "prefix:suffix"})))

  (t/testing "Should create an sqs-event task"
    (t/is (= (sut/sqs-event-task "sqs-event-ref" "someQueue")
             {:name "sqs-event-ref",
              :taskReferenceName "sqs-event-ref",
              :type "EVENT",
              :sink "sqs:someQueue"})))

  (t/testing "Should create an conductor-event task"
    (t/is (= (sut/conductor-event-task "conductor-event-task" "some-event")
             {:name "conductor-event-task",
              :taskReferenceName "conductor-event-task",
              :type "EVENT",
              :sink "conductor:some-event"})))

  (t/testing "Should create an dynamic-task task"
    (t/is (= (sut/dynamic-fork-task "dynamic-task-ref" "" "")
             {:name "dynamic-task-ref",
              :taskReferenceName "dynamic-task-ref",
              :type "FORK_JOIN_DYNAMIC",
              :inputParameters {:dynamicTasks "", :dynamicTasksInput ""},
              :dynamicForkTasksParam "dynamicTasks",
              :dynamicForkInputParameters "dynamicTasksInput"})))

  (t/testing "Should create a do-while task"
    (t/is (= (sut/do-while-task "do-while-ref" "" [] {})
             {:name "do-while-ref",
              :taskReferenceName "do-while-ref",
              :type "DO_WHILE",
              :loopCondition "",
              :inputParameters {},
              :loopOver []})))

  (t/testing "Should create a do-while loop iteration "
    (t/is (= (sut/new-loop-task "loop-sample-ref" 3 [])
             {:name "loop-sample-ref",
              :taskReferenceName "loop-sample-ref",
              :type "DO_WHILE",
              :loopCondition
              "if ( $.loop-sample-ref['iteration'] < $.value) {true;} else {false;}",
              :inputParameters {:value 3},
              :loopOver []}))))
