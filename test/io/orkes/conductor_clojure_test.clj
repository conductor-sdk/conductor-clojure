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
(ns io.orkes.conductor-clojure-test
  (:require [clojure.test :refer :all]
            [io.orkes.client :refer :all]
            [io.orkes.metadata :as metadata]
            [io.orkes.workflow-resource :as wresource]
            )
            (:import (com.netflix.conductor.sdk.testing WorkflowTestRunner)))

(def test-runner-instance (atom {}))

(defn start-fake-server
  []
  (reset! test-runner-instance (doto (WorkflowTestRunner. 8096 "3.8.0")
    (.init "com.netflix.conductor.testing.workflows")) ))

(defn stop-fake-server []
  (.shutdown @test-runner-instance))

(defn test-fixture [f]
  (start-fake-server)
  (f)
  (stop-fake-server))

(use-fixtures :once test-fixture)

(def options {
              :url  "http://localhost:8096/api/"
              } )

(deftest workflow-creation
  (def cool-b-task {
                    :name "cool_clj_task_b"
                    :description "some description"
                    :ownerEmail "mail@gmail.com"
                    :retryCount 3
                    :timeoutSeconds 300
                    :responseTimeoutSeconds 180
                    } )

  (def exclusive-join-workflow
    {
     :name "exclusive_join"
     :description "Exclusive Join Example"
     :version 1
     :tasks [ {
               :name "api_decision"
               :taskReferenceName "api_decision_ref"
               :inputParameters {
                                  "case_value_param" "${workflow.input.type}"
                                  }
               :type "SWITCH"
               :caseValueParam "case_value_param"
               :defaultCase []
               :evaluatorType "javascript"
               :expression "POST"
               :decisionCases {
                                "POST" [{
                                         :name "get-posts"
                                         :taskReferenceName "get_posts_ref"
                                         :inputParameters {
                                                            "http_request" {
                                                                            "uri" "https://jsonplaceholder.typicode.com/posts/1"
                                                                            "method" "GET"
                                                                            }
                                                            }
                                         :type "HTTP"
                                         }]
                                "COMMENT" [{
                                            :name "get_posts_comments"
                                            :taskReferenceName "get_post_comments_ref"
                                            :inputParameters {
                                                               "http_request" {
                                                                               "uri" "https://jsonplaceholder.typicode.com/comments?postId=1"
                                                                               "method" "GET"
                                                                               }

                                                               }
                                            :type "HTTP"
                                            }]
                                "USER" [{
                                         :name "get_user_posts"
                                         :taskReferenceName "get_user_posts_ref"
                                         :inputParameters {
                                                            "http_request" {
                                                                            "uri" "https://jsonplaceholder.typicode.com/posts?userId=1"
                                                                            "method" "GET"
                                                                            }

                                                            }

                                         :type "HTTP"
                                         }]
                                }
               },
             {
              :name "notification_join",
              :taskReferenceName "notification_join_ref"
              :inputParameters {}
              :type "JOIN"
              :joinOn ["get_posts_ref" "get_post_comments_ref" "get_user_posts_ref"]
              }

             ]
     :inputParameters []
     :outputParameters {:message "${clj_prog_task_ref.output.:message}"}
     :schemaVersion 2
     :restartable true
     :ownerEmail "mail@yahoo.com"
     :timeoutSeconds 0
     :timeoutPolicy "ALERT_ONLY"
     })

  (testing "Can register multiple tasks at once"
    (is (= nil (metadata/register-tasks options [cool-b-task
                                                 (assoc cool-b-task :name  "cool_clj_task_z")
                                                 (assoc cool-b-task :name  "cool_clj_task_x")]))))
  (testing "Can create a workflow with fork tasks"
    (is (= nil (metadata/register-workflow-def options {
                                                        :name "cool_clj_workflow_2"
                                                        :description "created programatically from clj"
                                                        :version 1
                                                        :tasks [ {
                                                                  :name "cool_clj_task_b"
                                                                  :taskReferenceName "cool_clj_task_ref"
                                                                  :inputParameters {}
                                                                  :type "SIMPLE"
                                                                  }
                                                                {
                                                                 :name "something",
                                                                 :taskReferenceName "other"
                                                                 :inputParameters {}
                                                                 :type "FORK_JOIN"
                                                                 :forkTasks [[
                                                                               {
                                                                                :name "cool_clj_task_z"
                                                                                :taskReferenceName "cool_clj_task_z_ref"
                                                                                :inputParameters {}
                                                                                :type "SIMPLE"
                                                                                }
                                                                               ]
                                                                              [
                                                                               {
                                                                                :name "cool_clj_task_x"
                                                                                :taskReferenceName "cool_clj_task_x_ref"
                                                                                :inputParameters {}
                                                                                :type "SIMPLE"
                                                                                }
                                                                               ]
                                                                              ]
                                                                 }
                                                                {
                                                                 :name "join"
                                                                 :type "JOIN"
                                                                 :taskReferenceName "join_ref"
                                                                 :joinOn [ "cool_clj_task_z", "cool_clj_task_x"]
                                                                 }
                                                                ]
                                                        :inputParameters []
                                                        :outputParameters {:message "${clj_prog_task_ref.output.:message}"}
                                                        :schemaVersion 2
                                                        :restartable true
                                                        :ownerEmail "mail@yahoo.com"
                                                        :timeoutSeconds 0
                                                        :timeoutPolicy "ALERT_ONLY"
                                                        })))
    )

  (testing  "Can create a workflow with exclusive-join"
    (is (= nil (metadata/register-workflow-def options exclusive-join-workflow))))


  (testing
  "Should be able to start a workflow"
  (let [wf-execution-id (wresource/start-workflow
                          options
                          {:version 1, :input {}, :name "cool_clj_workflow_2"})]
    (is (not-empty wf-execution-id))
    (is (not-empty (wresource/get-workflow options wf-execution-id)))))

  (testing "Should be able to get workflow defintion"
    (let [workflow-name (:name exclusive-join-workflow)
          workflow-version (:version exclusive-join-workflow)
          workflow-defintion (metadata/get-workflow-def options workflow-name 1)]
      (is (nil? (metadata/register-workflow-def options (assoc workflow-defintion :version (inc workflow-version) ))))
      (testing "Should be able to unregister a workflow"
        (is (nil? (metadata/unregister-workflow-def options workflow-name workflow-version))))
      )
    )

  (testing "Should be able to get a task definition by name"
         (let [task-name (:name cool-b-task)
               existing-task (metadata/get-task-def options task-name)]
           (is (not-empty existing-task))
           (testing "Should be able to update task properties"
                    (is (nil? (metadata/update-task-definition
                                options
                                (assoc existing-task
                                  :owner-email "othermaila@mail.com")))))
           (testing "Should be able to unregister task"
                    (is (nil? (metadata/unregister-task options task-name))))))

        (testing "Should be able to update an exisiting workflow"
         (is (nil? (metadata/update-workflows-def
                     options
                     [(assoc exclusive-join-workflow
                        :version (inc (:version exclusive-join-workflow)))]))))
  )
(comment

  )

(comment

(metadata/register-workflow-def options {
                                                        :name "wf_to_wait"
                                                        :description "created programatically from clj"
                                                        :version 1
                                                        :tasks [ {
                                                                                :name "cool_clj_task_z"
                                                                                :task-reference-name "cool_clj_task_z_ref"
                                                                                :input-parameters {}
                                                                                :type :simple
                                                                  },
                                                                {
                                                                                :name "cool_clj_task_x"
                                                                                :task-reference-name "cool_clj_task_x_ref"
                                                                                :input-parameters {}
                                                                                :type :simple
                                                                                }
                                                                ]
                                                        :input-parameters []
                                                        :output-parameters { "message" "${cool_clj_task_x_ref.output.message}"}
                                                        :schema-version 2
                                                        :restartable true
                                                        :owner-email "mail@yahoo.com"
                                                        :timeout-seconds 0
                                                        :timeout-policy :alert-only
                                                        })

(def instance (runner-executor-for-workers [{:name "cool_clj_task_z"
                                             :execute (fn [d]
                                                        (Thread/sleep 15000)
                                                        [:completed {"message" "Something silly"}]
                                                        )
                                             }
                                            {:name "cool_clj_task_x"
                                             :execute (fn [d]
                                                        (Thread/sleep 15000)
                                                        [:completed {"message" "Something silly 2"}]
                                                        )
                                             }
                                            ] {:url "http://localhost:12345/api/"}))
  (.shutdown instance)

  (def wf-id (wresource/start-workflow options {:version 1 :input {} :name "wf_to_wait" :correlation-id "super-cool"}) )

  (wresource/terminate-workflow options wf-id)

  (wresource/get-workflows options "wf_to_wait" "super-cool" :include-closed true :include-tasks true)

  (wresource/delete-workflow options "bbb9d385-04f1-4e5d-8c28-24c5c616e2fe")


    (wresource/terminate-workflows options  (repeatedly 5 #(wresource/start-workflow options {:version 1 :input {} :name "wf_to_wait" :correlation-id "super-cool"})))

  (repeatedly 5 #(wresource/start-workflow options {:version 1 :input {} :name "wf_to_wait" :correlation-id "super-cool"}))

  (wresource/get-running-workflow options "wf_to_wait" 1)

  (wresource/get-workflow-by-time-period options "wf_to_wait" 1 1648518013000 1651196413000)

  (wresource/pause-workflow options wf-id)

  (wresource/resume-workflow options wf-id)
(metadata/register-workflow-def options {
                                                        :name "wf_to_wait_long"
                                                        :description "created programatically from clj"
                                                        :version 1
                                                        :tasks [ {
                                                                                :name "cool_clj_task_z"
                                                                                :task-reference-name "cool_clj_task_z_ref"
                                                                                :input-parameters {}
                                                                                :type :simple
                                                                  },
                                                                {
                                                                                :name "cool_clj_task_x"
                                                                                :task-reference-name "cool_clj_task_x_ref"
                                                                                :input-parameters {}
                                                                                :type :simple
                                                                                }
{
                                                                                :name "cool_clj_task_x"
                                                                                :task-reference-name "cool_clj_task_c_ref"
                                                                                :input-parameters {}
                                                                                :type :simple
                                                                                }
{
                                                                                :name "cool_clj_task_x"
                                                                                :task-reference-name "cool_clj_task_b_ref"
                                                                                :input-parameters {}
                                                                                :type :simple
                                                                                }
                                                                ]
                                                        :input-parameters []
                                                        :output-parameters { "message" "${cool_clj_task_x_ref.output.message}"}
                                                        :schema-version 2
                                                        :restartable true
                                                        :owner-email "mail@yahoo.com"
                                                        :timeout-seconds 0
                                                        :timeout-policy :alert-only
                                                        })

    (def wf-id-long (wresource/start-workflow options {:version 1 :input {} :name "wf_to_wait_long" :correlation-id "super-cool"}) )
    (wresource/skip-task-from-workflow options wf-id-long "cool_clj_task_c_ref")
    (wresource/rerun-workflow options wf-id-long { :workflow-input {} :re-run-from-task-id "19b37b92-2e75-4c7c-81e5-25f978ebb1e7" :correlation-id "super-cool"})
    (wresource/retry-last-failed-task options "d212a1d0-293f-4ab9-9359-be33fff8d94d")

  )

(comment
(metadata/get-task-def)

  )
