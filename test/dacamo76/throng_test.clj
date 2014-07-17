(ns dacamo76.throng-test
  (:require [clojure.test :refer :all]
            [dacamo76.throng :refer :all]
            [clojure.string :as s]))

(deftest sanity-checks
  (is (= base-url "https://api.linkedin.com/v1"))
  (is (= people-url "https://api.linkedin.com/v1/people/~"))
  (is (= default-profile-fields
         ["educations" "certifications" "bound-account-types" "im-accounts" "twitter-accounts" "primary-twitter-account" "positions" "email-address" "skills" "last-modified-timestamp" "specialties" "summary" "id" "first-name" "last-name" "headline" "location"]))
  (is (= default-connections-fields
         ["positions" "id" "first-name" "last-name" "headline" "location" "industry" "distance" "current-status" "api-standard-profile-request" "member-url-resources"])))

(deftest preparing-requests
  (testing "formatting fields"
    (let [fields ["field1" "field2"]
          join (partial s/join ",")
          csv-fields (join fields)
          sandwiched-fields (str ":(" csv-fields ")")]
      (testing "sandwiching"
        (is (= sandwiched-fields
               (sandwich csv-fields)
               (sandwich (unsandwich sandwiched-fields))))
      (testing "profile fields"
        (is (= (join default-profile-fields)
               (profile-fields)))
        (is (= (join fields)
               (apply profile-fields fields))))
      (testing "connections fields"
        (is (= (str "connections:(" (join default-connections-fields) ")")
               (connections-fields)))
        (is (= (str "connections:(" (join fields) ")")
               (apply connections-fields fields))))))))

(defn collections=
  [& coll]
  (apply = (map frequencies coll)))

(deftest coll-keys-test
  (testing "coll-keys"
    (let [data {:name "daniel"
                :skills {:values (range 3) :_total 3}
                :connections {:values (range 2) :_total 2}
                :empty-values {:values [] :_total 50}}]
      (is (collections= [:empty-values :connections :skills]
                        (coll-keys data))))))

(deftest update-values-test
  (testing "update-values"
    (let [m {:1 1, :2 2, :3 3, :4 4}]
      (is (= {:1 1, :2 3, :3 4, :4 4}
             (update-values m [:2 :3] inc)))
      (is (= {:1 11, :2 2, :3 13, :4 14}
             (update-values m [:1 :3 :4] + 10)))
      (is (= m (update-values m [] inc)))
      (is (= m (update-values m nil nil))))))
