(ns grootlapse.server
  (:require ["express" :as express]
            ["http" :as http]
            ["fs" :as fs]
            ["pi-camera" :as camera]
            ["node-fetch" :as fetch]
            ["child_process" :as process]
            ["cors" :as cors]))

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
(def image-folder "images")
(def video-folder "videos")
(defn imagefile-name [number]
  (.padStart (str number ".jpg") 9 "0"))
(defn videofile-name [number]
  (.padStart (str number ".mp4") 9 "0"))
(defn create-groopse [req res]
  (let [image-number (atom 1)
        name (.-name (.-body req))
        path (str js/__dirname "/" image-folder "/" name "/" )]
    (fs/mkdirSync path #js {:recursive true})
    (let [interval (js/setInterval
                    (fn []
                      (-> (.snap (new camera #js {:mode "photo"
                                                  :output (str path (imagefile-name @image-number))
                                                  :width 640
                                                  :height 480
                                                  :nopreview true}))
                          (.then (fn []
                                   (prn "snap" (str path (imagefile-name @image-number)))
                                   (swap! image-number inc)))
                          (.catch (fn [e]
                                    (prn e)
                                    (js/clearInterval (:active @state))
                                    (swap! state assoc :active :ERROR)))))
                    10000)]
      (swap! state assoc :active interval)))
  (.sendStatus ^js res 200))

(defn load-groopse [req res]
  (let [name (.-name ^js (.-params req))]
    (.json res (clj->js
                (->> (fs/readdirSync (str image-folder "/" name))
                     (map (fn [file-name] (str "http://" server-name ":3000/" image-folder "/" name "/" file-name))))))))

(defn stitch-groopse [req res]
  (let [name (.-name ^js (.-params req))
        path (str video-folder "/" name)]
    (fs/mkdirSync path #js {:recursive true})
    (let [res (process/spawn
               "ffmpeg" (into-array ["-r" "10" "-i" (str image-folder "/" name "/%5d.jpg") "-r" "10" "-vcodec" "libx264" "-crf" "20" "-g" "15"
                                     (str path "/" (videofile-name 0))]))]
      (prn (.toString (.-stderr res))))
    (prn "end")
    (.json res name)))

(defn load-all-groopse [req res]
    (.json res
         (clj->js (map (fn [folder-name]
                         {:name folder-name
                          :image (if (fs/accessSync (str image-folder "/" folder-name "/00001.jpg"))
                                   (str "http://" server-name ":3000/" image-folder "/default.jpg")
                                   (str "http://" server-name ":3000/" image-folder "/" folder-name "/00001.jpg"))
                          :images (map
                                   (fn [file-name]
                                     (str "http://" server-name ":3000/" image-folder "/" folder-name "/" file-name))
                                   (fs/readdirSync (str image-folder "/" folder-name)))})
                       (fs/readdirSync image-folder)))))

(defn init []
  (let [app (express)]
    (.use app (cors))
    (.use app (.json express))
    (.use app (.static express "."))
    (.post app "/groopse" create-groopse)
    (.get app "/groopse/:name" load-groopse)
    (.get app "/groopse/:name/stitch" stitch-groopse)
    (.get app "/groopse/" load-all-groopse)
    (let [server (.createServer http app)]
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
