(ns thehex.youtube-api3.core
  (:require [thehex.youtube-api3.config :as config]
            [thehex.oauth.lib :as oauth]
            [clj-http.client :as http]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [thehex.notube.config :as notube-config]
            [taoensso.timbre :as log]))

;; use YOUTUBE_API_KEY environmental variable

;; - Comment threads for a video:
;; https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&videoId=UTXCu1VQDRw&key=

;; - Specific Comment by Id
;; https://www.googleapis.com/youtube/v3/comments?id=z124jjhzrlfitdvcw23xfzyrdya4ij0kj&part=snippet&key=

;; - Video info (statistics.. change part to get different info)
;; https://www.googleapis.com/youtube/v3/videos?part=statistics&id=UTXCu1VQDRw&key=

;; - Search all videos of pewdiepie's channel, ordered by date, newest first
;; https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&order=date&key=

;; With OAUTH and logged in w/ youtube user:
;; POST https://www.googleapis.com/youtube/v3/commenThreads?part=snippet&key=
;; post raw json body:
;; {
;;   "snippet": {
;;     "channelId": "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
;;     "topLevelComment": {
;;       "snippet": {
;;         "textOriginal": "comment3"
;;       }
;;     },
;;     "videoId": "TvxkKs2J0Vw"
;;   }
;; }


(def tokens (oauth/read-persisted-tokens))

(def api-key (-> (edn/read-string (slurp (clojure.java.io/resource "config.edn")))
                 :api-key))

(defn get-user-channels
  "
  Example...
  The API supports two ways to specify an access token:
  1. curl -H \"Authorization: Bearer ACCESS_TOKEN\" https://www.googleapis.com/youtube/v3/channels?part=id&mine=true
  2. curl https://www.googleapis.com/youtube/v3/channels?part=id&mine=true&access_token=ACCESS_TOKEN

  try #2 if option #1 doesnt work

  TODO: try catch 401. if 401, we nee to refresh the access token
  TODO: this call is not part of the oauth validation file, is an actual api call. move

  Use like: (get-user-channels (:access-token tokens))

  "
  [access-token]
  (let [url (str config/api-base "channels?part=id&mine=true")
        body (-> (http/get url
                      {:headers {:Authorization (str "Bearer " access-token)}
                       :as :json})
            :body ;; body is json
            ;; TODO: parse response json to actually get info
            ;; will receive a 401 HTTP unauthorize if the access token expired
            ;;:user ... old example on getting data...
            )
        ;; TODO: this is json at this point:
        as-json (json/read-str body)
        result-count (get (get as-json "pageInfo") "totalResults")]
    (log/debug (str "got body in get-user-channels: " body))

    (if (> result-count 0)
      (get as-json "items")
      nil)))


(defn get-channel-activity
  "Original Url: https://www.googleapis.com/youtube/v3/activities?part=snippet&channelId=UC-lHJZR3Gqxm24_Vd_AJ5Yw&key=

  Use like: `(get-channel-activity api-key 'UC-lHJZR3Gqxm24_Vd_AJ5Yw')`


  my channel= UC4-vzjcBolmvYWYP6ldbLbA
  another channel id = UC4u8goEsLgpPvDX2mKD70nQ
  "
  [api-key channel-id]
  (let [url (str config/api-base "activities?part=snippet&channelId=" channel-id "&key=" api-key)
        body (-> (http/get url {:as :json}) :body)]
    (log/trace (str "got activites: " body))
    body))

;; (get-user-channels "ya29.GlvzA_ODOrwscUER_kCPoJcNENgVxbPrbqeSq78_gCiwftNfd6GODfmqsAAvQMOAnAO_nRqJndAQeXX4FajYUO8l3WO0JBAqCLBGXqdM8-4O9NnDuTs662dSAUq9")
;; => "{\n \"kind\": \"youtube#channelListResponse\",\n \"etag\": \"\\\"uQc-MPTsstrHkQcRXL3IWLmeNsM/97Iyxf_peaO9sJsTg-Dx0zTmPcw\\\"\",\n \"pageInfo\": {\n  \"totalResults\": 1,\n  \"resultsPerPage\": 1\n },\n \"items\": [\n  {\n   \"kind\": \"youtube#channel\",\n   \"etag\": \"\\\"uQc-MPTsstrHkQcRXL3IWLmeNsM/xzn__LA2JEsGtk0Zo_TG8tb3c9U\\\"\",\n   \"id\": \"UC4-vzjcBolmvYWYP6ldbLbA\"\n  }\n ]\n}\n"
