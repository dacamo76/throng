# throng

A Clojure library to connect to the LinkedIn
[people](https://developer.linkedin.com/documents/people) and
[connections](https://developer.linkedin.com/documents/connections-api) APIs.

## Usage

```clojure
(require '[dacamo76.throng :as t])
```

Basic usage of Throng focuses on the functions ```full-request```
and ```connections-request```.

```full-request``` returns a user's full profile along with all their connections.
Connections is a lazy sequence that implements paging and makes a request
to the server as needed.

````connections-request```` returns only a user's connections.

Example usage of ```full-request```:
```clojure
user> (def token "MY_OAUTH2_TOKEN")
user> (def profile t/full-request token))
#'user/res'
user> (keys profile)
(:headline :emailAddress :firstName :summary :skills :id :lastName :connections :lastModifiedTimestamp :location :educations :positions)
user> (:lastName profile)
"Cañas"
user> (->> profile :connections count)
150
```

Example usage of ```connections-request```

```clojure
user> (def connections t/connections-request token))
#'user/connections'
user> (keys connections)
(:connections)
user> (->> connections :connections count)
150```

I think by default the API returns 5,000 connections
on each request.
This may result in a delay when making the original request,
if you don't need all connections, you can pass in an extra paramter
option to the request function.

````clojure
;;; Get connections in batches of 10.
user> (def connections (t/connections-request token :count 10))
#'user/connections

;;; Quickly get first 2 connections. Response only has 10 connections.
user> (->> connections :connections (take 2))

;;; DON'T DO THIS. This will make 15 API request for the 150 connections
user> (->> connections :connections count)
150
```

### Advanced usage

A low-level ```people-request``` function is available that returns
the [Ring-style response maps](https://github.com/ring-clojure/ring/blob/master/SPEC)
returned by [clj-http-lite](https://github.com/hiredman/clj-http-lite).
This can be combined with ```paging-body``` to return a map
Both ```full-request``` and ```connections-request``` build on it.

Example usage of ```people-request```. This is a basic connections-reuqets.
```clojure
user> (def peeps (let [fields (t/connections-fields)
     params {:headers {"x-li-format" "json"}
            :query-params {:oauth2_access_token token}}]
  (t/people-request params fields)))
user> (keys peeps)
(:headers :status :body)
user> (->> (t/paging-body peeps) keys)
(:connections)
user> (->> (t/paging-body peeps) :connections count)
150
```

## License

Copyright © 2014 Daniel Alberto Cañas

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
