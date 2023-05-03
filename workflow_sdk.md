# Authoring Workflows with the clojure SDK

## A simple three-step workflow

```clojure


(defn create-tasks
  "Returns workflow tasks"
  []
  (vector (sdk/simple-task (:get-user-info constants) (:get-user-info constants) {:userId "${workflow.input.userId}"})
          (sdk/switch-task "emailorsms" "${workflow.input.notificationPref}" {"email" [(sdk/simple-task (:send-email constants) (:send-email constants) {"email" "${get_user_info.output.email}"})]
                                                                              "sms" [(sdk/simple-task (:send-sms constants) (:send-sms constants) {"phoneNumber" "${get_user_info.output.phoneNumber}"})]} [])))

(defn create-workflow
  "Returns a workflow with tasks"
  [tasks]
  (merge (sdk/workflow (:workflow-name constants) tasks) {:inputParameters ["userId" "notificationPref"]}))
  
;; creates a workflow with tasks 
(-> (create-tasks) (create-workflow))

```

### Execute Workflow

#### Start a previously registered workflow

```clojure
(def workflow-request {:name "SomeWFName"
                       :version 1
                       :input {"userId" "jim"
                               "notificationPref" "sms"}})

(wr/start-workflow options workflow-request)
```

#### Execute a workflow and get the output as a result

```clojure
(def wf-output (wr/run-workflow-sync options (:workflow-name constants) 1 "requestId" workflow-request) )

```

### More Examples

You can find more examples at the following GitHub repository:
https://github.com/conductor-sdk/clojure-sdk-examples
