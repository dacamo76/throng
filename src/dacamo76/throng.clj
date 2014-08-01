(ns dacamo76.throng
  (:require [clojure.data.json :as json]
            [clojure.string :as s]
            [clj-http.lite.client :as client]))

(def base-url "https://api.linkedin.com/v1")

(def people-url (s/join "/" [base-url "people/~"]))

(def default-profile-fields
  ["educations" "certifications" "bound-account-types" "im-accounts"
   "twitter-accounts" "primary-twitter-account" "positions" "email-address"
   "skills" "last-modified-timestamp" "specialties" "summary" "id" "first-name"
   "last-name" "headline" "location"])

(def default-connections-fields
  ["positions" "id" "first-name" "last-name" "headline" "location" "industry"
   "distance" "current-status" "api-standard-profile-request"
   "member-url-resources"])

(defn sandwich [s] (str ":(" s ")"))

(defn unsandwich [s] (subs s 2 (dec (count s))))

(defn profile-fields
  [& fields]
  (s/join "," (or fields default-profile-fields)))

(defn connections-fields
  [& fields]
  (->> (or fields default-connections-fields)
       (s/join ",")
       sandwich
       (str "connections")))

(defn update-values
  [m ks f & args]
  (reduce #(apply update-in %1 [%2] f args) m ks))

(defn coll-keys
  [m]
  (keep (fn [[k v]] (when (:_total v) k)) m))

(defn body
  [response]
  (let [data (-> response
              :body
              (json/read-str :key-fn keyword))
        {:keys [headers status]} response
        response-meta {:headers headers, :status status}
        request-meta (meta response)
        colls (coll-keys data)]
    (with-meta
      (update-values data colls #(with-meta (:values % []) (dissoc % :values)))
      {:request request-meta
       :response response-meta})))

(defn people-request
  [opts & fields]
  ;(prn "calling external API")
  (let [fields (sandwich
                (if-let [[f] fields]
                  (profile-fields f)
                  (profile-fields)))
        url (str people-url fields)]
    (with-meta
      (client/get url opts)
      {:opts opts, :url url, :fields fields})))

(defn pagination
  [k response]
  (lazy-seq
   (let [coll (k response)
         metadata (-> coll meta)
         count (:_count metadata)
         total (:_total metadata)
         start (:_start metadata)
         ;_ (prn "count" count "total" total "start" start)
         {:keys [opts fields]} (-> response meta :request)]
     (if (and count total start (< (+ start count) total))
       (lazy-cat
        coll
        (pagination k
                    (body
                     (people-request
                      (assoc-in opts [:query-params :start] (+ start count))
                      (unsandwich fields)))))
       coll))))

(defn paging-body
  [response]
  (let [m (body response)
        colls (for [[k v] m
                    :when (coll? v)]
                k)]
    (reduce #(assoc %1 %2 (pagination %2 m)) m colls)))

(defn connections-request
  [oauth2-token & {:as params}]
  (let [fields (connections-fields)
        opts {:headers {"x-li-format" "json"}
              :query-params (merge params {:oauth2_access_token oauth2-token})}]
    (paging-body (people-request opts fields))))

(defn full-request
  [oauth2-token & {:as params}]
  (let [fields (s/join ","
                       [(profile-fields)
                        (connections-fields)])
        opts {:headers {"x-li-format" "json"}
              :query-params (merge params {:oauth2_access_token oauth2-token})}]
    (paging-body (people-request opts fields))))
