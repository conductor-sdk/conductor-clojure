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
(ns conductor.conductor-clojure-test
  (:require [clojure.test :refer :all]
            [conductor.client :refer :all]
            [conductor.metadata :as metadata]
            ))
(def options {
              :url  "http://localhost:8080/api/"
              } )

(deftest workflow-creation
  (testing "Can register multiple tasks at once"
    (is (= nil (metadata/register-tasks options [{
                                                  :name "cool_clj_task_b"
                                                  :description "some description"
                                                  :owner-email "mail@gmail.com"
                                                  :retry-count 3
                                                  :timeout-seconds 300
                                                  :response-timeout-seconds 180 },
                                                 {
                                                  :name "cool_clj_task_z"
                                                  :description "some description"
                                                  :owner-email "mail@gmail.com"
                                                  :retry-count 3
                                                  :timeout-seconds 300
                                                  :response-timeout-seconds 180 }
                                                 {
                                                  :name "cool_clj_task_x"
                                                  :description "some description"
                                                  :owner-email "mail@gmail.com"
                                                  :retry-count 3
                                                  :timeout-seconds 300
                                                  :response-timeout-seconds 180 }
                                                 ]))))
  (testing "Can create a workflow with fork tasks"
    (is (= nil (metadata/register-workflow-def options {
                                                        :name "cool_clj_workflow_2"
                                                        :description "created programatically from clj"
                                                        :version 1
                                                        :tasks [ {
                                                                  :name "cool_clj_task_b"
                                                                  :task-reference-name "cool_clj_task_ref"
                                                                  :input-parameters {}
                                                                  :type :simple
                                                                  },
                                                                {
                                                                 :name "someting",
                                                                 :task-reference-name "other"
                                                                 :input-parameters {}
                                                                 :type :fork-join
                                                                 :fork-tasks [[
                                                                               {
                                                                                :name "cool_clj_task_z"
                                                                                :task-reference-name "cool_clj_task_z_ref"
                                                                                :input-parameters {}
                                                                                :type :simple
                                                                                }
                                                                               ]
                                                                              [
                                                                               {
                                                                                :name "cool_clj_task_x"
                                                                                :task-reference-name "cool_clj_task_x_ref"
                                                                                :input-parameters {}
                                                                                :type :simple
                                                                                }
                                                                               ]
                                                                              ]
                                                                 }
                                                                {
                                                                 :name "join"
                                                                 :type :join
                                                                 :task-reference-name "join_ref"
                                                                 :join-on [ "cool_clj_task_z", "cool_clj_task_x"]
                                                                 }
                                                                ]
                                                        :input-parameters []
                                                        :output-parameters {:message "${clj_prog_task_ref.output.:message}"}
                                                        :schema-version 2
                                                        :restartable true
                                                        :owner-email "mail@yahoo.com"
                                                        :timeout-seconds 0
                                                        :timeout-policy :alert-only
                                                        })))
    )
  (testing  "Can create a workflow with exclusive-join"
    (is (= nil (metadata/register-workflow-def options {
                                                        :name "exclusive_join"
                                                        :description "Exclusive Join Example"
                                                        :version 1
                                                        :tasks [ {
                                                                  :name "api_decision"
                                                                  :task-reference-name "api_decision_ref"
                                                                  :input-parameters {
                                                                                     "case_value_param" "${workflow.input.type}"
                                                                                     }
                                                                  :type :decision
                                                                  :case-value-param "case_value_param"
                                                                  :default-case []
                                                                  :decision-cases {
                                                                                   "POST" [{
                                                                                            :name "get-posts"
                                                                                            :task-reference-name "get_posts_ref"
                                                                                            :input-parameters {
                                                                                                               "http_request" {
                                                                                                                               "uri" "https://jsonplaceholder.typicode.com/posts/1"
                                                                                                                               "method" "GET"
                                                                                                                               }
                                                                                                               }
                                                                                            :type :http
                                                                                            }]
                                                                                   "COMMENT" [{
                                                                                               :name "get_posts_comments"
                                                                                               :task-reference-name "get_post_comments_ref"
                                                                                               :input-parameters {
                                                                                                                  "http_request" {
                                                                                                                                  "uri" "https://jsonplaceholder.typicode.com/comments?postId=1"
                                                                                                                                  "method" "GET"
                                                                                                                                  }

                                                                                                                  }
                                                                                               :type :http
                                                                                               }]
                                                                                   "USER" [{
                                                                                            :name "get_user_posts"
                                                                                            :task-reference-name "get_user_posts_ref"
                                                                                            :input-parameters {
                                                                                                               "http_request" {
                                                                                                                               "uri" "https://jsonplaceholder.typicode.com/posts?userId=1"
                                                                                                                               "method" "GET"
                                                                                                                               }

                                                                                                               }

                                                                                            :type :http
                                                                                            }]
                                                                                   }
                                                                  },
                                                                {
                                                                 :name "notification_join",
                                                                 :task-reference-name "notification_join_ref"
                                                                 :input-parameters {}
                                                                 :type :exclusive-join
                                                                 :join-on ["get_posts_ref" "get_post_comments_ref" "get_user_posts_ref"]
                                                                 }

                                                                ]
                                                        :input-parameters []
                                                        :output-parameters {:message "${clj_prog_task_ref.output.:message}"}
                                                        :schema-version 2
                                                        :restartable true
                                                        :owner-email "mail@yahoo.com"
                                                        :timeout-seconds 0
                                                        :timeout-policy :alert-only
                                                        })))
    )
  (testing "Can create a workflow with loop and decision"
    (is (= nil (metadata/register-workflow-def options {
                                                        :name "port_in_wf"
                                                        :description "Port In workflow"
                                                        :version 1
                                                        :output-parameters {:message "${clj_prog_task_ref.output.:message}"}
                                                        :schema-version 2
                                                        :restartable true
                                                        :owner-email "mail@yahoo.com"
                                                        :timeout-seconds 0
                                                        :timeout-policy :alert-only
                                                        :tasks [
                                                                {
                                                                 :name "Submit to itg"
                                                                 :task-reference-name  "submit_to_itg_with_retry"
                                                                 :input-parameters {
                                                                                    "value"  "${workflow.input.iterations}"
                                                                                    "terminate"  "${workflow.variables.terminate_loop}"
                                                                                    }
                                                                 :type :do-while
                                                                 :loop-condition "if ( ($.submit_to_itg_with_retry['iteration'] < $.value) && !$.terminate) { true; } else { false; }"
                                                                 :loop-over [{
                                                                              :name  "Submit to ITG"
                                                                              :task-reference-name  "submit_to_itg"
                                                                              :input-parameters {
                                                                                                 "http_request" {
                                                                                                                 "uri"  "https://jsonplaceholder.typicode.com/todos/${$.workflow.input.iterations}",
                                                                                                                 "method"  "GET",
                                                                                                                 },

                                                                                                 }
                                                                              :type :http

                                                                              }
                                                                             {
                                                                              :name "Check Status"
                                                                              :task-reference-name "check_status"
                                                                              :input-parameters {
                                                                                                 "prev_task_result"  "${submit_to_itg.output}"
                                                                                                 "switchCaseValue"  "${submit_to_itg.status}"
                                                                                                 }
                                                                              :type :decision
                                                                              :case-value-param "switchCaseValue"
                                                                              :decision-cases {
                                                                                               "COMPLETED" [{
                                                                                                             :name  "Complete request loop"
                                                                                                             :task-reference-name  "complete_loop_success"
                                                                                                             :input-parameters {
                                                                                                                                "terminate_loop"  true
                                                                                                                                "success" true
                                                                                                                                }
                                                                                                             :type :set-variable
                                                                                                             }]

                                                                                               "COMPLETED_WITH_ERRORS" [{
                                                                                                                         :name  "Retry Http"
                                                                                                                         :task-reference-name  "retry_http_request"
                                                                                                                         :input-parameters {
                                                                                                                                            "update_records_on_retry"  2
                                                                                                                                            }
                                                                                                                         :type :set-variable
                                                                                                                         }]

                                                                                               }
                                                                              :default-case [{
                                                                                              :name  "Permanent Failure"
                                                                                              :task-reference-name  "terminate_loop"
                                                                                              :input-parameters {
                                                                                                                 "update_records_on_retry"  2
                                                                                                                 }
                                                                                              :type :set-variable
                                                                                              }]
                                                                              }
                                                                             ]
                                                                 }
                                                                {
                                                                 :name "Wait for async message"
                                                                 :task-reference-name "wait_for_response"
                                                                 :input-parameters {}
                                                                 :type :wait
                                                                 }
                                                                ]
                                                        })))
    )
  (testing "Should be able to start a workflow"
    (is (not-empty (start-workflow options {:version 1 :input {} :name "cool_clj_workflow_2"} )) )
    )


  )
