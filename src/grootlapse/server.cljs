(ns grootlapse.server
  (:require ["express" :as express]
            ["http" :as http]
            ["fs" :as fs]
            ["pi-camera" :as camera]
            ["node-fetch" :as fetch]
            ["child_process" :as process]
            ["cors" :as cors]
            [clojure.string :as str]
            ["ws" :as ws]
            ["stream-split" :as stream-split]
            ["date-fns" :as date]))

(def server-name "axidraw")

(comment
  (->(fetch "http://" server-name ":3000/groopse"
            (clj->js
             {:method "POST"
              :headers {"Content-type" "application/json"}
              :body (js/JSON.stringify (clj->js {:name "flower"}))}))
     (.catch prn))
  (-> (fetch "http://" server-name ":3000/groopse/blume"
             (clj->js
             {:headers {"Content-type" "application/json"}}))
      (.then (fn [res] (.json res)))
      (.then prn)))

(defonce server-ref (volatile! nil))
(defonce state (atom {:active nil}))

(def public-folder "public")
(def image-folder "images")
(def video-folder "videos")
(defn imagefile-name [number]
  (.padStart (str number ".jpg") 9 "0"))
(defn videofile-name [number]
  (.padStart (str number ".mp4") 9 "0"))

(defn snap [path]
  (if (= server-name "localhost")
    (new js/Promise (fn [resolve reject]
                      (try
                        (fs/copyFileSync (str public-folder "/imgs/groot-sad.gif") path)
                        (catch js/Error e
                          (reject "Failed to snap" e)))
                      (js/setTimeout (fn [] (resolve)) 500)))
    (-> (.snap (new camera #js {:mode "photo"
                                :output path
                                :width 640
                                :height 480
                                :nopreview true}))
        (.then (fn [] (prn "snap" path))))))

(defn stop-active-groopse []
  (js/clearInterval (:interval (:active @state)))
  (js/clearTimeout (:timeout (:active @state)))
  (swap! state assoc :active nil))

(defn stop-active-groopse-handler [_ res]
  (stop-active-groopse)
  (.sendStatus ^js res 200))

(defn start-time [start]
  (let [start-date (date/parse start "HH:mm" (new js/Date))]
    (if (date/isValid start-date)
      (Math/min
       (- (.getTime (date/add start-date #js {:days 1}))
          (.getTime (new js/Date)))
       (- (.getTime start-date)
          (.getTime (new js/Date))))
      0)))


(defn get-image-number [images-path]
  (->> (map
        #(js/parseInt (first (str/split % #"\.")))
        (fs/readdirSync images-path))
       (filter integer?)
       sort
       last
       inc))

(defn create-groopse [{:keys [name interval start]}]
  (let [path (str js/__dirname "/" public-folder "/" image-folder "/" name "/" )
        video-path (str js/__dirname "/" public-folder "/" video-folder "/" name "/" )]
    (fs/mkdirSync path #js {:recursive true})
    (fs/mkdirSync video-path #js {:recursive true})
    (let [start-offset (start-time start)]
      (swap! state assoc :active {:name name
                                  :state (if (> start-offset 0) "WAITING" "RUNNING")
                                  :start start})
      (prn "start in" (/ start-offset (* 1000 60)) "mins")
      (let [timeout-id
            (js/setTimeout
             (fn []
               (swap! state assoc-in [:active :state] "RUNNING")
               (-> (snap (str path (imagefile-name (get-image-number path))))
                   (.then (fn []
                            (prn "first photo" (str path (imagefile-name (get-image-number path))))
                            (swap! state assoc-in [:active :next] (date/format (date/add (new js/Date) #js {:minutes interval}) "HH:mm"))
                            (let [interval-id
                                  (js/setInterval
                                   (fn []
                                     (-> (snap (str path (imagefile-name (get-image-number path))))
                                         (.then (fn []
                                                  (swap! state assoc-in [:active :next]
                                                         (date/format (date/add (new js/Date) #js {:minutes interval}) "HH:mm"))
                                                  (prn "interval photo" (str path (imagefile-name (get-image-number path))))))
                                         (.catch (fn [e]
                                                   (prn "SNAP ERROR: "e)
                                                   (js/clearInterval (:interval (:active @state)))
                                                   (swap! state assoc :active :ERROR)))))
                                   (* 1000 60 interval))]
                              (swap! state assoc-in [:active :interval] interval-id))))
                   (.catch (fn [e]
                             (prn "SNAP ERROR: "e)
                             (js/clearInterval (:interval (:active @state)))
                             (swap! state assoc :active :ERROR)))))
             start-offset)]
        (swap! state assoc-in [:active :timeout] timeout-id)))))

(defn delete-groopse [req res]
  (let [name (.-name ^js (.-params req))
        path (str js/__dirname "/" public-folder "/" image-folder "/" name "/" )
        video-path (str js/__dirname "/" public-folder "/" video-folder "/" name "/" )]
    (when (= name (:name (:active @state)))
      (stop-active-groopse))
    (fs/rmdirSync path #js {:recursive true})
    (fs/rmdirSync video-path #js {:recursive true})
    (.sendStatus ^js res 200)))

(defn delete-video [req res]
  (let [name (.-name ^js (.-params req))
        video (.-video ^js (.-params req))
        video-path (str js/__dirname "/" public-folder "/" video-folder "/" name "/" video)]
    (fs/unlinkSync video-path)
    (.sendStatus ^js res 200)))

(defn delete-image [req res]
  (let [name (.-name ^js (.-params req))
        image (.-image ^js (.-params req))
        image-path (str js/__dirname "/" public-folder "/" image-folder "/" name "/" image)]
    (fs/unlinkSync image-path)
    (.sendStatus ^js res 200)))

(defn load-groopse [req res]
  (let [name (.-name ^js (.-params req))]
    (.json res (clj->js
                (->> (fs/readdirSync (str public-folder "/" image-folder "/" name))
                     (map (fn [file-name] (str "http://" server-name ":3000/" image-folder "/" name "/" file-name))))))))

(def stitch-interval (atom nil))
(defn stitch-groopse [req res]
  (let [name (.-name ^js (.-params req))
        path (str video-folder "/" name)]
    (fs/mkdirSync (str public-folder "/" path) #js {:recursive true})
    (let [video-nr (->> (fs/readdirSync (str public-folder "/" video-folder "/" name))
                        (map (fn [filename] (js/parseInt (str/replace filename ".mp4" ""))))
                        (filter (fn [video-nr] (not (js/isNaN video-nr))))
                        sort
                        last
                        inc)]
      (process/spawnSync
       "ffmpeg" (into-array ["-r" "10" "-i" (str public-folder "/" image-folder "/" name "/%5d.jpg") "-r" "10" "-vcodec" "libx264" "-crf" "20" "-g" "15"
                             (str public-folder "/" path "/" (videofile-name video-nr))]))
      (if (fs/accessSync (str public-folder "/" path "/" (videofile-name video-nr)))
        (reset! stitch-interval
                (js/setInterval (fn [] (try
                                        (fs/accessSync (str public-folder "/" path "/" (videofile-name video-nr)))
                                        (js/clearInterval @stitch-interval)
                                        (reset! stitch-interval nil)
                                        (.json res (str path "/" (videofile-name video-nr)))
                                        (catch js/Error e
                                          (prn e))))
                                1000))
        (.json res (str "http://" server-name ":3000/" path "/" (videofile-name video-nr)))))))

(defn load-all-groopse [req res]
    (.json res
         (clj->js
          {:active (when (get-in @state [:active :name])
                     (select-keys (:active @state) [:name :state :start :next]))
           :groopse
           (map (fn [folder-name]
                  {:name folder-name
                   :image (try
                            (fs/accessSync (str public-folder "/" image-folder "/" folder-name "/00001.jpg"))
                            (str "http://" server-name ":3000/" image-folder "/" folder-name "/00001.jpg")
                            (catch js/Error _
                                (str "http://" server-name ":3000/default.png")))
                   :images (map
                            (fn [file-name]
                              (str "http://" server-name ":3000/" image-folder "/" folder-name "/" file-name))
                            (fs/readdirSync (str public-folder "/" image-folder "/" folder-name)))
                   :videos (map
                            (fn [file-name]
                              (str "http://" server-name ":3000/" video-folder "/" folder-name "/" file-name))
                            (fs/readdirSync (str public-folder "/" video-folder "/" folder-name)))})
                (fs/readdirSync (str public-folder "/" image-folder)))})))


(def width 960)
(def height  540)
(def nal-separator (new js/Buffer #js [0 0 0 1]))
(def stream-server (atom {:wss nil :read-stream nil}))
(defn broadcast [data]
  (doseq [socket (.-clients (:wss @stream-server))]
    (when-not ^js (.-buzy socket)
      (set! ^js (.-buzy socket) true)
      (set! ^js (.-buzy socket) false)

      (.send socket
             (.concat js/Buffer #js [nal-separator data])
             #js {:binary true}
             (fn [] (set! ^js (.-buzy socket) false))))))
(defn start-stream []
  (let [fps 12
        command (str "raspivid -t 0 -o - -w " width " -h " height " -fps " fps " -pf baseline")
        [command & params] (str/split command #" ")
        streamer (process/spawn command (clj->js params))]
    (swap! stream-server assoc :read-stream streamer)
    (.on streamer "error" (fn [error] (prn "error raspivid" error)))
    (.on streamer "exit" (fn [code] (prn "exit raspivid" code)))
    (.on (.pipe (.-stdout streamer) (new stream-split nal-separator)) "data"
         broadcast)))

(defn pause-stream []
  (if (:read-stream @stream-server)
    ^js (.kill (:read-stream @stream-server))
    (prn "Trying to pause non existant stream")))

(defn end-stream []
  (if (:read-stream @stream-server)
    ^js (.kill (:read-stream @stream-server))
    (prn "Trying to end non existant stream")))

(defn new-client [socket]
  (.send socket (js/JSON.stringify #js {:action "init"
                                        :width width
                                        :height height}))
  ^js (.on socket "message"
       (fn [data]
         (let [[action] (str/split (str data) " ")]
           (case action
             "REQUESTSTREAM" (start-stream)
             "STOPSTREAM" (pause-stream)))))
  ^js (.on socket "close"
       (fn []
         (end-stream))))



(defn init []
  (let [app (express)]
    (.use app (cors))
    (.use app (.json express))
    (.use app (.static express "./public"))
    (.post app "/groopse"
           (fn [req res]
             (create-groopse (js->clj (.-body req) :keywordize-keys true))
             (.sendStatus ^js res 200)))
    (.post app "/groopse/active/stop" stop-active-groopse-handler)
    (.get app "/groopse/:name" load-groopse)
    (.delete app "/groopse/:name" delete-groopse)
    (.delete app "/groopse/:name/video/:video" delete-video)
    (.delete app "/groopse/:name/image/:image" delete-image)
    (.get app "/groopse/:name/stitch" stitch-groopse)
    (.get app "/groopse/" load-all-groopse)
    (.all app "/*" (fn [_ res] ^js (.sendFile res (str js/__dirname "/public/index.html"))))
    (let [server (.createServer http app)
          {:keys [wss]} (swap! stream-server assoc :wss (new (.-Server ws) #js {:server server}))]
      ^js (.on wss "connection" new-client)
      (.listen server 3000
               (fn [err]
                 (if err
                   (js/console.error "server start failed")
                   (js/console.info "http server running"))
                 ))
      (vreset! server-ref server))))

(defn start []
  (js/console.warn "start called")
  (init))

(defn stop [done]
  (js/console.warn "stop called")
  (when-some [srv @server-ref]
    (.close srv
            (fn [err]
              (js/console.log "stop completed" err)
              (done)))))
