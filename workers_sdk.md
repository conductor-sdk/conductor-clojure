# Writing Workers with the Javascript SDK

A worker is responsible for executing a task.
Operator and System tasks are handled by the Conductor server, while user defined tasks needs to have a worker created that awaits the work to be scheduled by the server for it to be executed.

Worker framework provides features such as polling threads, metrics and server communication.

### Design Principles for Workers

Each worker embodies design pattern and follows certain basic principles:

1. Workers are stateless and do not implement a workflow specific logic.
2. Each worker executes a very specific task and produces well-defined output given specific inputs.
3. Workers are meant to be idempotent (or should handle cases where the task that is partially executed gets rescheduled due to timeouts etc.)
4. Workers do not implement the logic to handle retries etc, that is taken care by the Conductor server.

### Creating Task Workers

Task worker is implemented using a function that confirms to the following function

```clojure
(def worker
           {:name "cool_clj_task_b",
            :execute (fn [d]
                       [:completed (:inputData d)])})
```

Worker returns a map that can be serialized to json 
If an `error` is returned, the task is marked as `FAILED`

#### Task worker that returns an object

```clojure
(def worker
           {:name "cool_clj_task_b",
            :execute (fn [d]
                       { :status  "COMPLETED"
                       :outputData {"someKey" "someValue"} })})
```

#### Controlling execution for long-running tasks

For the long-running tasks you might want to spawn another process/routine and update the status of the task at a later point and complete the
execution function without actually marking the task as `COMPLETED`. Use `TaskResult` Interface that allows you to specify more fined grained control.

Here is an example of a task execution function that returns with `IN_PROGRESS` status asking server to push the task again in 60 seconds.

```typescript
(def worker
           {:name "cool_clj_task_b",
            :execute (fn [d]
                       { :status  "COMPLETED"
                       :outputData {"someKey" "someValue"}
                       :status "IN_PROGRESS"
                       :callbackAfterSeconds 60})})
```

## Starting Workers

`TaskRunner` interface is used to start the workers, which takes care of polling server for the work, executing worker code and updating the results back to the server.

```clojure
(:require 
            [io.orkes.taskrunner :refer :all])

;; Will  poll for tasks
(def shutdown-task-runner (runner-executer-for-workers options [worker]))

;; Stops polling for tasks
(shutdown-task-runner )



```

## Task Management APIs

### Get Task Details

```clojure
(:require 
    [io.orkes.task-resource :refer :all])
            
(get-task-details options any-task-id)
            
```

### Updating the Task result outside the worker implementation

#### Update task by Reference Name

```clojure
(:require 
    [io.orkes.task-resource :refer :all])
(update-task-by-reference-name options workflow-id task-reference-name status some-update-req)
```

#### Update task by id

```clojure

(:require 
    [io.orkes.task-resource :refer :all])
(update-task options task-result-changes)
```

### Next: [Create and Execute Workflows](workflow_sdk.md)
