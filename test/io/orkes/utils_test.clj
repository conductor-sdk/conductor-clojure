(ns io.orkes.utils-test
  (:require [io.orkes.utils :as ut]
            [clojure.test :as t]))

(def switch-task-example
  {:name "api_decision",
   :taskReferenceName "api_decision_ref",
   :inputParameters {"case_value_param" "${workflow.input.type}"},
   :type "SWITCH",
   :caseValueParam "case_value_param",
   :defaultCase [],
   :evaluatorType "javascript",
   :expression "POST",
   :decisionCases
   {"POST" [{:name "get-posts",
             :taskReferenceName "get_posts_ref",
             :inputParameters
             {"http_request"
              {"uri" "https://jsonplaceholder.typicode.com/posts/1",
               "method" "GET"}},
             :type "HTTP"}],
    "COMMENT"
    [{:name "get_posts_comments",
      :taskReferenceName "get_post_comments_ref",
      :inputParameters
      {"http_request"
       {"uri" "https://jsonplaceholder.typicode.com/comments?postId=1",
        "method" "GET"}},
      :type "HTTP"}],
    "USER" [{:name "get_user_posts",
             :taskReferenceName "get_user_posts_ref",
             :inputParameters
             {"http_request"
              {"uri"
               "https://jsonplaceholder.typicode.com/posts?userId=1",
               "method" "GET"}},
             :type "HTTP"}]}})

(def loop-example
  {:loopCondition
   "if ($.loopTask['iteration'] < $.value) { true; } else { false;} ",
   :joinOn [],
   :loopOver [{:joinOn [],
               :loopOver [],
               :defaultExclusiveJoinTask [],
               :name "cool_clj_task_z",
               :forkTasks [],
               :type "SIMPLE",
               :defaultCase [],
               :asyncComplete false,
               :optional false,
               :inputParameters {},
               :decisionCases {},
               :startDelay 0,
               :taskReferenceName "sample_task_name_1qewx_ref"}],
   :defaultExclusiveJoinTask [],
   :name "loopTask",
   :forkTasks [],
   :type "DO_WHILE",
   :defaultCase [],
   :asyncComplete false,
   :optional false,
   :inputParameters {:value "${workflow.input.loop}"},
   :decisionCases {},
   :startDelay 0,
   :taskReferenceName "loopTask"})

(def loop-over-wf-example
  {:ownerEmail "james.stuart@orkes.io",
   :description
   "Edit or extend this sample workflow. Set the workflow name to get started",
   :name "testing_loop_iterations",
   :outputParameters {},
   :variables {},
   :updateTime 1657561521874,
   :tasks
   [{:joinOn [],
     :loopOver [],
     :defaultExclusiveJoinTask [],
     :name "sample_task_name_set_variable",
     :forkTasks [],
     :type "SET_VARIABLE",
     :defaultCase [],
     :asyncComplete false,
     :optional false,
     :inputParameters {:loop "3"},
     :decisionCases {},
     :startDelay 0,
     :taskReferenceName "sample_task_name_sr1ge_ref"}
    loop-example
    {:joinOn [],
     :loopOver [],
     :defaultExclusiveJoinTask [],
     :name "cool_clj_task_b",
     :forkTasks [],
     :type "SIMPLE",
     :defaultCase [],
     :asyncComplete false,
     :optional false,
     :inputParameters {},
     :decisionCases {},
     :startDelay 0,
     :taskReferenceName "sample_task_name_iznrb_ref"}],
   :timeoutSeconds 0,
   :inputParameters [],
   :timeoutPolicy "ALERT_ONLY",
   :workflowStatusListenerEnabled false,
   :version 1,
   :inputTemplate {},
   :restartable false,
   :schemaVersion 2})

(def exclusive-join-workflow
  {:name "exclusive_join"
   :description "Exclusive Join Example"
   :version 1
   :tasks [switch-task-example,
           {:name "notification_join",
            :taskReferenceName "notification_join_ref"
            :inputParameters {}
            :type "JOIN"
            :joinOn ["get_posts_ref" "get_post_comments_ref" "get_user_posts_ref"]}]

   :inputParameters []
   :outputParameters {:message "${clj_prog_task_ref.output.:message}"}
   :schemaVersion 2
   :restartable true
   :ownerEmail "mail@yahoo.com"
   :timeoutSeconds 0
   :timeoutPolicy "ALERT_ONLY"})
(def fork-task-example
  {:name "something",
   :taskReferenceName "other"
   :inputParameters {}
   :type "FORK_JOIN"
   :forkTasks [[{:name "cool_clj_task_z"
                 :taskReferenceName "cool_clj_task_z_ref"
                 :inputParameters {}
                 :type "SIMPLE"}]

               [{:name "cool_clj_task_x"
                 :taskReferenceName "cool_clj_task_x_ref"
                 :inputParameters {}
                 :type "SIMPLE"}]]})

(def wf-fork-example
  {:name "cool_clj_workflow_2"
   :description "created programatically from clj"
   :version 1
   :tasks [{:name "cool_clj_task_b"
            :taskReferenceName "cool_clj_task_ref"
            :inputParameters {}
            :type "SIMPLE"}
           fork-task-example

           {:name "join"
            :type "JOIN"
            :taskReferenceName "join_ref"
            :joinOn ["cool_clj_task_z", "cool_clj_task_x"]}]

   :inputParameters []
   :outputParameters {:message "${clj_prog_task_ref.output.:message}"}
   :schemaVersion 2
   :restartable true
   :ownerEmail "mail@yahoo.com"
   :timeoutSeconds 0
   :timeoutPolicy "ALERT_ONLY"})
(t/deftest tesing-utils
  (t/testing "Should be able to extract tasks from a fork task"
    (t/is (= (ut/task-children fork-task-example)
             (list {:inputParameters {},
                    :name "cool_clj_task_z",
                    :taskReferenceName "cool_clj_task_z_ref",
                    :type "SIMPLE"}
                   {:inputParameters {},
                    :name "cool_clj_task_x",
                    :taskReferenceName "cool_clj_task_x_ref",
                    :type "SIMPLE"}))))

  (t/testing
   "Should be able to extract tasks from a switch-task"
    (t/is
     (= (ut/task-children switch-task-example)
        (list
         {:name "get-posts",
          :taskReferenceName "get_posts_ref",
          :inputParameters {"http_request"
                            {"uri"
                             "https://jsonplaceholder.typicode.com/posts/1",
                             "method" "GET"}},
          :type "HTTP"}
         {:name "get_posts_comments",
          :taskReferenceName "get_post_comments_ref",
          :inputParameters
          {"http_request"
           {"uri" "https://jsonplaceholder.typicode.com/comments?postId=1",
            "method" "GET"}},
          :type "HTTP"}
         {:name "get_user_posts",
          :taskReferenceName "get_user_posts_ref",
          :inputParameters
          {"http_request"
           {"uri" "https://jsonplaceholder.typicode.com/posts?userId=1",
            "method" "GET"}},
          :type "HTTP"}))))

  (t/testing "Should be able to extract tasks from a loop task"
    (t/is (= (ut/task-children loop-example)
             (list {:joinOn [],
                    :loopOver [],
                    :defaultExclusiveJoinTask [],
                    :name "cool_clj_task_z",
                    :forkTasks [],
                    :type "SIMPLE",
                    :defaultCase [],
                    :asyncComplete false,
                    :optional false,
                    :inputParameters {},
                    :decisionCases {},
                    :startDelay 0,
                    :taskReferenceName "sample_task_name_1qewx_ref"}))))

  (t/testing "Should return a sequence of all available tasks"
    (t/is (= (ut/flatten-tasks (:tasks wf-fork-example))
             (list {:name "cool_clj_task_b",
                    :taskReferenceName "cool_clj_task_ref",
                    :inputParameters {},
                    :type "SIMPLE"}
                   fork-task-example
                   {:name "cool_clj_task_z",
                    :taskReferenceName "cool_clj_task_z_ref",
                    :inputParameters {},
                    :type "SIMPLE"}
                   {:name "cool_clj_task_x",
                    :taskReferenceName "cool_clj_task_x_ref",
                    :inputParameters {},
                    :type "SIMPLE"}
                   {:name "join",
                    :type "JOIN",
                    :taskReferenceName "join_ref",
                    :joinOn ["cool_clj_task_z" "cool_clj_task_x"]}))))

  (t/testing
   "Should be able to apply a function to all workflow tasks"
    (t/is (true? (let [mod-wf-name (ut/map-wf-tasks #(assoc % :name "fakeName")
                                                    wf-fork-example)]
                   (every? #(= "fakeName" (:name %))
                           (ut/flatten-tasks (:tasks mod-wf-name)))))))

  (t/testing "Should be able to filter tasks accoridng to predicate"
    (t/is (let [mod-wf-remove-task
                (ut/filter-wf-tasks
                 #(= (:taskReferenceName %) "cool_clj_task_ref")
                 wf-fork-example)]
            (every? #(= "cool_clj_task_ref" (:taskReferenceName %))
                    (ut/flatten-tasks (:tasks mod-wf-remove-task)))))))
(comment
  (ut/flatten-tasks (:tasks wf-fork-example)))
