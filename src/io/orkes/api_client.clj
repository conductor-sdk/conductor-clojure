(ns io.orkes.api-client
  (:require [cheshire.core :as json]
            [org.httpkit.client :as http]))

(def authorization-header-key "X-AUTHORIZATION")
(def json-headers
  {"Content-Type" "application/json", "Accept" "application/json"})

(defn build-token
  [app-key app-secret url]
  (http/post (str url "token")
             {:body (json/generate-string {"keyId" app-key,
                                           "keySecret" app-secret}),
              :headers json-headers}))

(defn throw-error
  [res]
  (throw (Exception. (-> res
                         :body
                         (json/parse-string true)
                         :message))))

(defmulti response-parser (fn [{status :status}] status))
(defmethod response-parser 200
  [res]
  (let [content-type (-> res :headers :content-type)
        body-parser-fn (if (= "application/json" content-type) #(json/parse-string % true) #(if (empty? %) nil %))]
    (-> res
        :body
        (body-parser-fn))))
(defmethod response-parser 204 [_] nil)
(defmethod response-parser 500
  [res]
  (throw-error res))
(defmethod response-parser 400
  [res]
  (throw-error res))
(defmethod response-parser 409
  [res]
  (throw-error res))
(defmethod response-parser 429
  [res]
  (throw-error res))
(defmethod response-parser 404 [res] (throw (Exception. (str  "Not found" res))))
(defmethod response-parser :default [m] (println (str "Failed" m)))

(defn make-request
  [req]
  (response-parser
      ;; (identity
   @(http/request req))
  ;; (response-parser )
  )

(defn generic-client
  [{:keys [app-key app-secret url], :or {url "http://localhost:8080/api/"}}]
  (fn [endpoint &
       {:keys [method query-params body],
        :or {method :get, query-params {}, body {}}}]
    (let [original-request {:method method,
                            :headers json-headers,
                            :body (json/generate-string body),
                            :query-params query-params,
                            :url (str url endpoint),
                            :as :text}]
      (make-request
       (if app-key
         (update-in original-request
                    [:headers]
                    #(merge {authorization-header-key
                             (-> @(build-token app-key app-secret url)
                                 :body
                                 (json/parse-string true)
                                 :token)}
                            %))
         original-request)))))
