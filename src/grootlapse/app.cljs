(ns grootlapse.app
  (:require [grootlapse.components :refer [spinner]]
            [reagent.core :as r]
            [reagent.dom :as dom]
            ["react-router-dom" :as router]))

(defonce stream-server (atom nil))
(defonce state (r/atom {:groopse "LOADING"}))
(def server-name "axidraw")
(def server (str "http://" server-name ":3000"))

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
(defn groopse-details []
  (let [video-errors (r/atom #{})
        details-state (r/atom nil)]
    (fn [props]
      (let [name (:name (:params (:match props)))
            {:keys [images videos]} (some (fn [groopse] (when (= name (:name groopse)) groopse)) (:groopse @state))]
        [:div.p-4
         [:button.mb-4 {:on-click (:goBack (:history props))} "< Back"]
         [:h1.text-2xl.mb-4 (:name (:params (:match props)))]
         [:div.mb-8
          (when (> (count videos) 0)
            [:<>
             [:h2.text-xl.mb-2 "Video"]
             [:div.flex.flex-wrap.justify-between
              (doall
               (map
                (fn [video]
                  (when-not (@video-errors video)
                    [:video.mb-2.border.w-100
                     {:key video :src video :controls true
                      :onError (fn [] (prn "error"
                                          (swap! video-errors conj video)))}]))
                videos))]])]
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
              [:img.mb-2.border {:key image :src image :style {:width "49%"}}])
            images)]]]))))

(defn load-groopse []
  (-> (js/fetch (str server "/groopse"))
      (.then (fn [res] (.json res)))
      (.then (fn [json] (swap! state merge (js->clj json :keywordize-keys true))))
      (.catch (fn [e]
                (prn e)
                (swap! state assoc :groopse "ERROR")))))
(def groopse-new
  (with-meta
    (fn []
      (let [preview (r/atom false)
            name (r/atom "")
            error (r/atom "")]
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
             {:on-click (fn []
                          (if (> (count @name) 0)
                            (do
                              (when @stream-server
                                (.stopStream ^js @stream-server)
                                (reset! stream-server nil))
                              (->(js/fetch (str "http://" server-name ":3000/groopse")
                                           (clj->js
                                            {:method "POST"
                                             :headers {"Content-type" "application/json"}
                                             :body (js/JSON.stringify (clj->js {:name @name}))}))
                                 (.then (fn [res]
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
     :class "absolute bg-green-800 text-white px-2 py-4 text-3xl rounded-full shadow-xl border border-black"
     :to "/new"}
    "Add"]
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

