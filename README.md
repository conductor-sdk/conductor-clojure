# Conductor OSS Clojure SDK

The `conductor-clojure` repository provides the client SDKs to build task workers in clojure 

Building the task workers in clojure mainly consists of the following steps:

1. Setup conductor-clojure package
2. [Create and run task workers](workers_sdk.md)
3. [Create workflows using code](workflow_sdk.md)
4. [Api Docs](docs/api/README.md)
   
### Setup Conductor Clojure Package

* Get the package from clojars

```clojure
:deps {org.clojure/clojure {:mvn/version "1.11.0"}
        io.orkes/conductor-clojure {:mvn/version "0.3.1"}}
```

## Configurations

### Authentication Settings (Optional)
Configure the authentication settings if your Conductor server requires authentication.
* keyId: Key for authentication.
* keySecret: Secret for the key.

### Access Control Setup
See [Access Control](https://orkes.io/content/docs/getting-started/concepts/access-control) for more details on role-based access control with Conductor and generating API keys for your environment.

### Configure API Client options

```clojure
;;; define options
{:app-key "some-key",
            :app-secret "some-secret",
            :url "http://localhost:8080/api/"}

```

### Next: [Create and run task workers](workers_sdk.md)

# Netflix Conductor SDK - Clojure

Software Development Kit for Netflix Conductor, written on and providing support for Clojure.
