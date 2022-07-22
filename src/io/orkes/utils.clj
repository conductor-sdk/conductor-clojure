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
(ns io.orkes.utils
(:require [clojure.zip :as zip])
  )

(defmulti task-children (fn [{task-type :type}] task-type))

(defmethod task-children "FORK_JOIN"
  [m]
  (flatten (map-indexed (fn [idx item] (map #(with-meta % {:fork-postion idx}) item ) ) (:forkTasks m)) ))

(defmethod task-children "DO_WHILE"
  [m]
  (:loopOver m))

(defmethod task-children "SWITCH"
  [m]
  (into
    []
    (concat
      (reduce-kv (fn [vec k v]
                   (into vec (map (fn [t] (with-meta t {:switch-branch k})) v)))
                 []
                 (:decisionCases m))
      (map #(with-meta % {:switch-branch :defaultCase}) (:defaultCase m)))))

(defmethod task-children :default [_] [])




(defmulti reconstruct-children (fn [{task-type :type} _] task-type))

(defmethod reconstruct-children "FORK_JOIN"
  [m children]
  (let [task-position (group-by #(-> %
                                     meta
                                     :fork-postion)
                                children)]
    (assoc m
      :forkTasks (reduce (fn [arr pos] (into arr [(get task-position pos)])) []
                   (range 0
                          (-> task-position
                              keys
                              count))))))

(defmethod reconstruct-children "DO_WHILE"
  [m children]
  (assoc m :loopOver children))

(defmethod reconstruct-children "SWITCH"
  [m children]
  (let [all-decision-cases (group-by #(-> %
                                          meta
                                          :switch-branch)
                                     children)
        default-case-tasks (:defaultCase all-decision-cases)]
    (-> m
        (assoc :decisionCases (dissoc all-decision-cases :defaultCase))
        (assoc :defaultCase default-case-tasks))))

(defn branch? [n]
      (or (= (:type n) "FORK_JOIN")
          (= (:type n) "SWITCH")
          (= (:type n) "DO_WHILE")
          (sequential? n)
          ))

(defn create-zipper-for-tasks
  [tasks]
  (zip/zipper
    branch?
    (fn [n] (if (sequential? n) n (task-children n)))
    (fn [node children]
      (if (map? node) (reconstruct-children node children) (into [] children)))
    tasks))

(defn flatten-task [task] (tree-seq branch? task-children task))

(defn iter-zip
  [zipper]
  (->> zipper
       (iterate zip/next)
       (take-while (complement zip/end?))))

(defn flatten-tasks
  [tasks]
(rest (tree-seq branch? (fn [n] (if (sequential? n) n (task-children n))) tasks) ))

;; (tree-seq branch? (fn [n] (if (sequential? n) n (task-children n))) tasks)

(defn map-task
  [func tasks]
  (loop [loc (create-zipper-for-tasks tasks)]
    (if (zip/end? loc)
      (zip/node loc)
      (if (-> loc
              zip/node
              map?)
        (recur (zip/next (zip/edit loc #(func %))))
        (recur (zip/next loc))))))

(defn map-wf-tasks [func wf] (merge wf {:tasks (map-task func (:tasks wf))}))

(defn filter-task [pred tasks]
  (loop [loc (create-zipper-for-tasks tasks)]
    (if (zip/end? loc)
      (zip/node loc)
      (if (-> loc
              zip/node
              map?)
        (recur (zip/next (if (-> loc zip/node pred) loc (zip/remove loc))))
        (recur (zip/next loc))))))

(defn filter-wf-tasks [func wf] (merge wf {:tasks (filter-task func (:tasks wf))}))

(comment
  (map meta
    (reduce-kv (fn [vec k v]
                 (into vec (map (fn [t] (with-meta t {:switch-branch k})) v)))
               []
               {:ape [{:name "jimmy"}]}))

  (defn iter-zip
    [zipper]
    (->> zipper
         zip/down
         (iterate zip/next)
         (take-while (complement zip/end?))))

  (map-task #(if (= (:type %) "SIMPLE") (assoc % :name "jimmy") %)
            [{:name "cool_clj_task_b",
              :taskReferenceName "cool_clj_task_ref",
              :inputParameters {},
              :type "SIMPLE"}
             {:name "something",
              :taskReferenceName "other",
              :inputParameters {},
              :type "FORK_JOIN",
              :forkTasks [[{:name "cool_clj_task_z",
                            :taskReferenceName "cool_clj_task_z_ref",
                            :inputParameters {},
                            :type "SIMPLE"}]
                          [{:name "cool_clj_task_x",
                            :taskReferenceName "cool_clj_task_x_ref",
                            :inputParameters {},
                            :type "SIMPLE"}]]}
             {:name "join",
              :type "JOIN",
              :taskReferenceName "join_ref",
              :joinOn ["cool_clj_task_z" "cool_clj_task_x"]}])

  (filter-task #(not= (:name %) "something")
               [{:name "cool_clj_task_b",
                 :taskReferenceName "cool_clj_task_ref",
                 :inputParameters {},
                 :type "SIMPLE"}
                {:name "something",
                 :taskReferenceName "other",
                 :inputParameters {},
                 :type "FORK_JOIN",
                 :forkTasks [[{:name "cool_clj_task_z",
                               :taskReferenceName "cool_clj_task_z_ref",
                               :inputParameters {},
                               :type "SIMPLE"}]
                             [{:name "cool_clj_task_x",
                               :taskReferenceName "cool_clj_task_x_ref",
                               :inputParameters {},
                               :type "SIMPLE"}]]}
                {:name "join",
                 :type "JOIN",
                 :taskReferenceName "join_ref",
                 :joinOn ["cool_clj_task_z" "cool_clj_task_x"]}])

  (def test-zipper
    (zip/zipper branch?
                (fn [n] (if (sequential? n) n (task-children n)))
                (fn [node children]
                  (if (map? node)
                    (reconstruct-children node children)
                    (into [] children)))
                [{:name "cool_clj_task_b",
                  :taskReferenceName "cool_clj_task_ref",
                  :inputParameters {},
                  :type "SIMPLE"}
                 {:name "something",
                  :taskReferenceName "other",
                  :inputParameters {},
                  :type "FORK_JOIN",
                  :forkTasks [[{:name "cool_clj_task_z",
                                :taskReferenceName "cool_clj_task_z_ref",
                                :inputParameters {},
                                :type "SIMPLE"}]
                              [{:name "cool_clj_task_x",
                                :taskReferenceName "cool_clj_task_x_ref",
                                :inputParameters {},
                                :type "SIMPLE"}]]}
                 {:name "join",
                  :type "JOIN",
                  :taskReferenceName "join_ref",
                  :joinOn ["cool_clj_task_z" "cool_clj_task_x"]}]))
  (defn find-first [pred coll] (some (fn [x] (when (pred x) x)) coll))
  (defn up-name [n] (assoc n :name "jimmy"))
  (-> test-zipper
      iter-zip
      (zip/edit (fn [l] (println l) l))
      zip/root)
  (loop [loc test-zipper]
    (if (zip/end? loc)
      (zip/node loc)
      (if (-> loc
              zip/node
              map?)
        (recur (zip/next (zip/edit loc #(assoc % :name "jimmy"))))
        (recur (zip/next loc)))))
  ;; (map zip/node (find-first zip/node ) (iter-zip test-zipper) )
  (task-children
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
                 {"uri"
                    "https://jsonplaceholder.typicode.com/comments?postId=1",
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
  (map meta
    (task-children
      {:name "api_decision",
       :taskReferenceName "api_decision_ref",
       :inputParameters {"case_value_param" "${workflow.input.type}"},
       :type "SWITCH",
       :caseValueParam "case_value_param",
       :defaultCase [{:name "cool_clj_task_b",
                      :taskReferenceName "cool_clj_task_ref",
                      :inputParameters {},
                      :type "SIMPLE"}],
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
                   {"uri"
                      "https://jsonplaceholder.typicode.com/comments?postId=1",
                    "method" "GET"}},
              :type "HTTP"}],
          "USER"
            [{:name "get_user_posts",
              :taskReferenceName "get_user_posts_ref",
              :inputParameters
                {"http_request"
                   {"uri" "https://jsonplaceholder.typicode.com/posts?userId=1",
                    "method" "GET"}},
              :type "HTTP"}]}}))
  (reconstruct-children
    {:name "something",
     :taskReferenceName "other",
     :inputParameters {},
     :type "FORK_JOIN",
     :forkTasks []}
    (task-children {:name "something",
                    :taskReferenceName "other",
                    :inputParameters {},
                    :type "FORK_JOIN",
                    :forkTasks [[{:name "cool_clj_task_z",
                                  :taskReferenceName "cool_clj_task_z_ref",
                                  :inputParameters {},
                                  :type "SIMPLE"}]
                                [{:name "cool_clj_task_x",
                                  :taskReferenceName "cool_clj_task_x_ref",
                                  :inputParameters {},
                                  :type "SIMPLE"}]]}))
  (task-children {:name "something",
                  :taskReferenceName "other",
                  :inputParameters {},
                  :type "FORK_JOIN",
                  :forkTasks [[{:name "cool_clj_task_z",
                                :taskReferenceName "cool_clj_task_z_ref",
                                :inputParameters {},
                                :type "SIMPLE"}]
                              [{:name "cool_clj_task_x",
                                :taskReferenceName "cool_clj_task_x_ref",
                                :inputParameters {},
                                :type "SIMPLE"}]]})
  (tree-seq
    branch?
    task-children
    {:name "something",
     :taskReferenceName "other",
     :inputParameters {},
     :type "FORK_JOIN",
     :forkTasks [[{:name "cool_clj_task_z",
                   :taskReferenceName "cool_clj_task_z_ref",
                   :inputParameters {},
                   :type "SIMPLE"}]
                 [{:name "cool_clj_task_x",
                   :taskReferenceName "cool_clj_task_x_ref",
                   :inputParameters {},
                   :type "SIMPLE"}
                  {:name "something2",
                   :taskReferenceName "other",
                   :inputParameters {},
                   :type "FORK_JOIN",
                   :forkTasks [[{:name "cool_clj_task_z2",
                                 :taskReferenceName "cool_clj_task_z_ref",
                                 :inputParameters {},
                                 :type "SIMPLE"}]
                               [{:name "cool_clj_task_x2",
                                 :taskReferenceName "cool_clj_task_x_ref",
                                 :inputParameters {},
                                 :type "SIMPLE"}]]}]]})
  (rest (tree-seq branch?
                  (fn [n] (if (vector? n) n (task-children n)))
                  [{:name "cool_clj_task_b",
                    :taskReferenceName "cool_clj_task_ref",
                    :inputParameters {},
                    :type "SIMPLE"}
                   {:name "something",
                    :taskReferenceName "other",
                    :inputParameters {},
                    :type "FORK_JOIN",
                    :forkTasks [[{:name "cool_clj_task_z",
                                  :taskReferenceName "cool_clj_task_z_ref",
                                  :inputParameters {},
                                  :type "SIMPLE"}]
                                [{:name "cool_clj_task_x",
                                  :taskReferenceName "cool_clj_task_x_ref",
                                  :inputParameters {},
                                  :type "SIMPLE"}]]}
                   {:name "join",
                    :type "JOIN",
                    :taskReferenceName "join_ref",
                    :joinOn ["cool_clj_task_z" "cool_clj_task_x"]}]))
  (flatten-tasks [{:name "cool_clj_task_b",
                   :taskReferenceName "cool_clj_task_ref",
                   :inputParameters {},
                   :type "SIMPLE"}
                  {:name "something",
                   :taskReferenceName "other",
                   :inputParameters {},
                   :type "FORK_JOIN",
                   :forkTasks [[{:name "cool_clj_task_z",
                                 :taskReferenceName "cool_clj_task_z_ref",
                                 :inputParameters {},
                                 :type "SIMPLE"}]
                               [{:name "cool_clj_task_x",
                                 :taskReferenceName "cool_clj_task_x_ref",
                                 :inputParameters {},
                                 :type "SIMPLE"}]]}
                  {:name "join",
                   :type "JOIN",
                   :taskReferenceName "join_ref",
                   :joinOn ["cool_clj_task_z" "cool_clj_task_x"]}])
  (flatten (map flatten-tasks
             [{:name "cool_clj_task_b",
               :taskReferenceName "cool_clj_task_ref",
               :inputParameters {},
               :type "SIMPLE"}
              {:name "something",
               :taskReferenceName "other",
               :inputParameters {},
               :type "FORK_JOIN",
               :forkTasks [[{:name "cool_clj_task_z",
                             :taskReferenceName "cool_clj_task_z_ref",
                             :inputParameters {},
                             :type "SIMPLE"}]
                           [{:name "cool_clj_task_x",
                             :taskReferenceName "cool_clj_task_x_ref",
                             :inputParameters {},
                             :type "SIMPLE"}]]}
              {:name "join",
               :type "JOIN",
               :taskReferenceName "join_ref",
               :joinOn ["cool_clj_task_z" "cool_clj_task_x"]}]))
  (seq {:a 1}))

;; (comment
;; (def options
;;     {:app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d",
;;      :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean",
;;      :url "http://localhost:8080/api/"})

;; (def tasks (-> (io.orkes.metadata/get-workflow-def options "Github_star_workflow" 1) :tasks ) )
;; (count (map :type (flat-task (first tasks)) ) )
;; (count (flatten (map #(-> % flat-task ) tasks) ) )
;; (count (tasks-filter #(= (:type %) "HTTP") tasks) )
;; )
