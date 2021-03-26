(ns grootlapse.app
  (:require [grootlapse.components :refer [spinner]]
            [reagent.core :as r]
            [reagent.dom :as dom]
            ["react-router-dom" :as router]
            [clojure.string :as s]))

(defonce stream-server (atom nil))
(defonce state (r/atom {:groopse "LOADING"}))
(def server-name "axidraw")
(def server (str "http://" server-name ":3000"))

(defn icon [path size color]
  (let [sizes {:small 12 :medium 24 :large 48}]
    [:svg {:width (size sizes) :height (size sizes) :viewBox "0 0 24 24"}
     [:path {:d path :fill color}]]))

(defn plus-icon
  ([] (icon "M24 10h-10v-10h-4v10h-10v4h10v10h4v-10h10z" :medium "black"))
  ([size] (icon "M24 10h-10v-10h-4v10h-10v4h10v10h4v-10h10z" size "black"))
  ([size color] (icon "M24 10h-10v-10h-4v10h-10v4h10v10h4v-10h10z" size color)))

(defn trash-icon
  ([] (icon "M3 6v18h18v-18h-18zm5 14c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm4-18v2h-20v-2h5.711c.9 0 1.631-1.099 1.631-2h5.315c0 .901.73 2 1.631 2h5.712z" :medium "black"))
  ([size] (icon "M3 6v18h18v-18h-18zm5 14c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm4-18v2h-20v-2h5.711c.9 0 1.631-1.099 1.631-2h5.315c0 .901.73 2 1.631 2h5.712z" size "black"))
  ([size color] (icon "M3 6v18h18v-18h-18zm5 14c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm4-18v2h-20v-2h5.711c.9 0 1.631-1.099 1.631-2h5.315c0 .901.73 2 1.631 2h5.712z" size color)))

(defn button
  ([children]
   [button {} children])
  ([{:keys [on-click pending class style]} children]
   [:button.bg-green-700.text-white.p-2.rounded-xl.shadow.font-bold
    {:on-click (when-not pending on-click) :type "button"
     :class class :style style}
    (if pending "Lade" children)]))

(defn groopse-overview [{:keys [name image]}]
  [:<> {:key image}
   [:> (.-Link router) {:class "w-1/2 p-4" :to (str "/" name)}
    [:img.rounded-xl.mb-1 {:src image}]
    [:div.font-bold.pl-1 name]]])
(def canvas-ref (atom nil))

(defn load-groopse []
  (-> (js/fetch (str server "/groopse"))
      (.then (fn [res] (.json res)))
      (.then (fn [json] (swap! state merge (js->clj json :keywordize-keys true))))
      (.catch (fn [e]
                (prn e)
                (swap! state assoc :groopse "ERROR")))))

(defn groopse-details []
  (let [video-errors (r/atom #{})
        details-state (r/atom nil)
        delete-mode (r/atom nil)
        deleting (r/atom nil)
        show-all-images (r/atom nil)
        show-all-videos (r/atom nil)]
    (fn [props]
      (let [name (:name (:params (:match props)))
            {:keys [images videos]} (some (fn [groopse] (when (= name (:name groopse)) groopse)) (:groopse @state))
            max-videos 1
            max-images 5]
        [:div.p-4.relative
         [:button.mb-4 {:on-click (:goBack (:history props))} "< Back"]
         [:div.flex.justify-between
          [:h1.text-2xl.mb-4 (:name (:params (:match props)))]
          (when (= name (:name (:active @state)))
            [:div
             [:span.text-xl.pr-3 "Live"]
             [button {:on-click (fn []
                                  (->(js/fetch (str "http://" server-name ":3000/groopse/active/stop")
                                               (clj->js
                                                {:method "POST"
                                                 :headers {"Content-type" "application/json"}}))
                                     (.then (fn [] (swap! state assoc :active nil)))))} "Stop"]])]
         [:div.mb-8
          (when (> (count videos) 0)
            [:<>
             [:h2.text-xl.mb-2 "Video"]
             [:div.flex.flex-wrap.justify-between
              (doall
               (map
                (fn [video]
                  (when-not (@video-errors video)
                    [:div.relative {:key video}
                     [:video.mb-2.border.w-100
                      {:src video :controls true
                       :onError (fn [] (prn "error"
                                           (swap! video-errors conj video)))}]
                     [button {:class "absolute" :style {:top "1rem" :right "1rem"}
                              :on-click (fn [] (reset! delete-mode {:type :video
                                                                   :label (str "Video " (s/join "/" (take-last 2 (s/split video #"/"))))
                                                                   :name (apply str (take-last 1 (s/split video #"/")))}))}
                      [trash-icon :medium "white"]]]))
                (take-last (if @show-all-videos (count videos) max-videos) videos)))]
             [:div.w-full.flex.justify-center.pb-5
              (when (and (not @show-all-videos) (> (count videos) max-videos))
                [button {:on-click (fn [] (reset! show-all-videos true))}
                 "alle anzeigen"])]])]
         [:div
          [:div.flex.items-center.justify-between.mb-6
           [:h2.text-xl "Bilder"]
           [button {:pending (= @details-state "STITCHING")
                    :on-click
                    (fn []
                      (reset! details-state "STITCHING")
                      (->(js/fetch (str "http://" server-name ":3000/groopse/" name "/stitch")
                                   (clj->js
                                    {:headers {"Content-type" "application/json"}}))
                         (.then (fn [res] (.json res)))
                         (.then (fn [video]
                                  (reset! details-state nil)
                                  (swap! state update :groopse (fn [groopses] (map (fn [groopse]
                                                                                    (if (= (:name groopse) name)
                                                                                      (update groopse :videos conj video)
                                                                                      groopse))
                                                                                  groopses)))))))}
            "Video erstellen"]]
          [:div.flex.flex-wrap.justify-between
           (map
            (fn [image]
              [:div.relative {:key image :style {:width "49%"}}
               [:img.mb-2.border {:src image}]
               [button {:class "absolute" :style {:top "0.5rem" :right "0.5rem"}
                        :on-click (fn [] (reset! delete-mode {:type :image
                                                             :label (str "Bild " (s/join "/" (take-last 2 (s/split image #"/"))))
                                                             :name (apply str (take-last 1 (s/split image #"/")))}))}
                [trash-icon :small "white"]]])
            (take (if @show-all-images (count images) max-images) images))]
          [:div.w-full.flex.justify-center.pb-5
           (when (and (not @show-all-images) (> (count images) max-images))
             [button {:on-click (fn [] (reset! show-all-images true))}
              "alle anzeigen"])]]
         (when @delete-mode
           [:div.px-5.fixed.w-full
            {:style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"}}
            [:div.bg-green-100.rounded.p-4
             [:div.pb-8
              (str (:label @delete-mode) " sicher löschen?")]
             [:div.flex.w-full
              [button {:class "w-1/2 mr-1"
                       :pending @deleting
                       :on-click (fn []
                                   (reset! deleting true)
                                   (->
                                    (js/fetch (case (:type @delete-mode)
                                                :groopse (str "http://" server-name ":3000/groopse/" name)
                                                :video (str "http://" server-name ":3000/groopse/" name "/video/" (:name @delete-mode))
                                                :image (str "http://" server-name ":3000/groopse/" name "/image/" (:name @delete-mode)))
                                                (clj->js
                                                 {:headers {"Content-type" "application/json"}
                                                  :method "DELETE"}))
                                      (.then (fn [res]
                                               (reset! deleting false)
                                               (if (.-ok res)
                                                 (do
                                                   (when (= (:type @delete-mode) :groopse)
                                                     ((:push (:history props)) "/"))
                                                   (load-groopse)
                                                   (reset! delete-mode nil))
                                                 (prn  "Etwas ist schiefgelaufen!"))))))}
               "OK"]
              [button {:class "w-1/2 ml-1"
                       :on-click (fn [] (reset! delete-mode nil))}
               "Abbrechen"]]]])
         [button {:on-click (fn [] (reset! delete-mode {:type :groopse
                                                       :label (str "Groopse " name)}))}
          "Löschen"]]))))


(def groopse-new
  (with-meta
    (fn []
      (let [preview (r/atom false)
            name (r/atom "")
            error (r/atom "")
            creating (r/atom nil)]
        (fn [props]
          [:form.p-4
           [:div.pb-8
            [:label.block.mb-2.pl-2 {:for "groopse-name"}
             "Name"]
            [:input.border.rounded.w-full.p-2.font-bold
             {:id "groopse-name" :value @name :on-change (fn [e] (reset! name ^js (.-target.value e)))}]]
           [:div.relative.pb-16
            [:div.mb-2.pl-2 "Preview"]
            [button {:class "absolute"
                     :style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"
                             :display (when @preview "none")}
                     :on-click (fn []
                                 (when (and @canvas-ref (= nil @stream-server))
                                   (let [uri (str "ws://" server-name ":3000")
                                         wsavc (js/window.WSAvcPlayer. @canvas-ref "webgl" 1 35)]
                                     (.connect wsavc uri)
                                     (reset! stream-server wsavc)))
                                 (js/setTimeout
                                  (fn []
                                    (.playStream ^js @stream-server)
                                    (reset! preview true))
                                  1000))}
             "Preview"]
            [:canvas.w-full.border.rounded
             {:ref (fn [ref] (reset! canvas-ref ref))}]]
           ;; interval?
           ;; duration?
           ;; space calculation?
           [:div.w-full.flex.justify-end
            [button
             {:pending @creating
              :on-click (fn []
                          (if (> (count @name) 0)
                            (do
                              (when @stream-server
                                (.stopStream ^js @stream-server)
                                (reset! stream-server nil))
                              (reset! creating true)
                              (->(js/fetch (str "http://" server-name ":3000/groopse")
                                           (clj->js
                                            {:method "POST"
                                             :headers {"Content-type" "application/json"}
                                             :body (js/JSON.stringify (clj->js {:name @name}))}))
                                 (.then (fn [res]
                                          (reset! creating false)
                                          (if (.-ok res)
                                            (do
                                              (load-groopse)
                                              ((:push (:history props)) "/"))
                                            (reset! error "Etwas ist schiefgelaufen!"))))))
                            (reset! error "Bitte Namen Eintragen")))}
             "Erstellen"]]
           (when @error [:div.red @error])])))
    {:component-will-unmount (fn []
                               (when @stream-server
                                 (.stopStream ^js @stream-server)
                                 (reset! stream-server nil)))}))
(defn overview []
  [:div.flex.flex-wrap
   [:> (.-Link router)
    {:style {:right "1rem" :bottom "1rem"}
     :class "absolute bg-green-800 p-4 rounded-full shadow-xl"
     :to "/new"}
    [plus-icon :medium "white"]]
   (when (:active @state)
     [:<>
      [:h2.px-4.pt-4.pb-2.text-xl "Aktiv"]
      (let [active-name (:name (:active @state))]
        [:div.flex.items-center
         [groopse-overview {:name (:name (:active @state))
                            :image (some (fn [{:keys [name image]}]
                                           (when (= name active-name)
                                             image))
                                         (:groopse @state))}]
         [button {:on-click (fn []
                              (->(js/fetch (str "http://" server-name ":3000/groopse/active/stop")
                                           (clj->js
                                            {:method "POST"
                                             :headers {"Content-type" "application/json"}}))
                                 (.then (fn [] (swap! state assoc :active nil)))))
                  :class "w-1/3"} "Stop"]])])
   [:hr.w-full]
   (case (:groopse @state)
     "ERROR"
     [:div.w-full.flex.justify-center.items-center.flex-col.py-4
      [:div.text-xl.mb-3 "Wo ist Groot?"]
      [:img {:src "/images/groot-sad.gif" :class "w-3/4 rounded"}]]
     "LOADING"
     [:div.w-full.flex.justify-center.items-center.flex-col.py-4
      [:div.text-xl.mb-3 "Suche Groot..."]
      [spinner]]
     [:div.flex.flex-wrap
      (map groopse-overview (:groopse @state))])])

(defn app []
  (load-groopse)
  (fn []
    [:> (.-BrowserRouter router)
     [:div.flex.flex-col.h-full
      [:header.bg-green-700.text-white
       [:div.max-w-4xl.mx-auto.w-full.text-center.py-2.text-lg "GrootLapse"]]
      [:main.flex-1.relative
       [:> (.-Route router)
        {:path "/" :exact true}
        [overview]]
       [:> (.-Switch router)
        [:> (.-Route router)
         {:path "/new"
          :render (fn [props] (r/as-element [groopse-new (js->clj props :keywordize-keys true)]))}]
        [:> (.-Route router)
         {:path "/:name"
          :render (fn [props] (r/as-element [groopse-details (js->clj props :keywordize-keys true)]))}]]]
      [:footer.bg-green-700.text-white
       [:div.max-w-4xl.mx-auto.w-full.text-center.py-1 "Birthday edition"]]]]))


(dom/render [app] (js/document.getElementById "app"))
(defn init [])

(comment
  (->(js/fetch (str "http://" server-name ":3000/groopse")
               (clj->js
                {:method "POST"
                 :headers {"Content-type" "application/json"}
                 :body (js/JSON.stringify (clj->js {:name "flower5"}))}))
     (.catch prn))
  (->(js/fetch (str "http://" server-name ":3000/groopse/flower3/stitch")
               (clj->js
                {:headers {"Content-type" "application/json"}}))
     (.then (fn [res] (.json res)))
     (.then prn)
     (.catch prn)))

