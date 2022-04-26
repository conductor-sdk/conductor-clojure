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
(ns conductor.workflow-resource
  (:import
   (io.orkes.conductor.client.http OrkesWorkflowClient))
  (:require [clojure.tools.logging :as log]
            [conductor.mapper-utils :as mapperutils])
  )

(defn workflow-client
  "Returns an instance of TaskClient. when app-key and app-secret are provided
  Then returned instance will be Orkes Compatible"
  [{:keys [app-key app-secret url] :or {url "http://localhost:8080/api/"}} ]
  (let [client (OrkesWorkflowClient. )]
    (.setRootURI client url)
    (when app-key
      (.withCredentials client app-key app-secret)
      (log/debug "Creating client with authentication"))
    client))

(defn start-workflow-with-client
  "Takes a client and a start-request map, and starts a workflow"
  [client start-request]
  (.startWorkflow client (mapperutils/clj-start-workflow-request->StartWorkflowRequest start-request)))


(defn start-workflow
  "Takes a an option map and a start-request map and starts a workflow.
  Returns the id of a workflow execution"
  [options start-request]
  (-> (workflow-client options)
      (start-workflow-with-client start-request)))

(defn get-workflow-with-client
  ([client workflowId] (.getWorkflow client workflowId true))
  ([client workflowId include-tasks] (.getWorkflow client workflowId include-tasks)))

(defn get-workflow [options & args]
  (let [client-inst (workflow-client options)]
    (mapperutils/java-map->clj (apply get-workflow-with-client client-inst args))))

(defn terminate-workflow-with-client
  "Takes a client a workflow-id and an optional reason. will terminate a running workflow"
  ([client workflow-id reason] (.terminateWorkflow client workflow-id reason))
  ([client workflow-id] (.terminateWorkflow client workflow-id nil)))

(defn terminate-workflow
  "Terminates a running workflow. given an id and an optional reason"
  [options workflow-id & args]
(-> (workflow-client options)
    ( #(apply terminate-workflow-with-client % workflow-id args) )))

(comment
(def options {
              :app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d"
              :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean"
              :url "http://localhost:8080/api/"
              })

(def client
(workflow-client options)
  )
(.getWorkflow client  "8542dfe4-259b-4e65-99ca-4116a020524d" false)
(mapperutils/java-map->clj (get-workflow-with-client client "8542dfe4-259b-4e65-99ca-4116a020524d") )
(get-workflow options "8542dfe4-259b-4e65-99ca-4116a020524d" )


  )
