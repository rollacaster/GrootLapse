(ns grootlapse.raspberrypi.core
  (:require ["express" :as express]
            ["http" :as http]
            ["fs" :as fs]
            ["pi-camera" :as camera]
            ["node-fetch" :as fetch]
            ["cors" :as cors]))

(def server-name "192.168.178.20")

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

(defn create-groopse [req res]
  (let [image-number (atom 0)
        name (.-name (.-body req))
        path (str js/__dirname "/" image-folder "/" name "/" )]
    (fs/mkdirSync path #js {:recursive true})
    (let [interval (js/setInterval
                    (fn []
                      (-> (.snap (new camera #js {:mode "photo"
                                                  :output (str path @image-number ".jpg")
                                                  :width 640
                                                  :height 480
                                                  :nopreview true}))
                          (.then (fn []
                                   (prn "snap" (str path @image-number ".jpg"))
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

(defn load-all-groopse [req res]
    (.json res
         (clj->js (map (fn [folder-name]
                         {:name folder-name
                          :image (if (fs/accessSync (str image-folder "/" folder-name "/1.png"))
                                   (str "http://" server-name ":3000/" image-folder "/default.jpg")
                                   (str "http://" server-name ":3000/" image-folder "/" folder-name "/1.png"))})
                       (fs/readdirSync image-folder)))))

(defn init []
  (let [app (express)]
    (.use app (cors))
    (.use app (.json express))
    (.use app (.static express "."))
    (.post app "/groopse" create-groopse)
    (.get app "/groopse/:name" load-groopse)
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
