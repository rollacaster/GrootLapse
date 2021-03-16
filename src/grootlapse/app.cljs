(ns grootlapse.app
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            ["react-router-dom" :as router]))

(defonce stream-server (atom nil))
(defonce state (r/atom {}))
(def server-name "axidraw")
(def server (str "http://" server-name ":3000"))

(defn button
  ([children]
   [button {} children])
  ([{:keys [on-click pending]} children]
   [:button.bg-green-700.text-white.p-2.rounded-xl.shadow.font-bold
    {:on-click (when-not pending on-click) :type "button"}
    (if pending "Lade" children)]))

(defn groopse-overview [{:keys [name image]}]
  [:<> {:key image}
   [:> (.-Link router) {:class "w-1/2 p-4" :to (str "/" name)}
    [:img.rounded-xl.mb-1 {:src image}]
    [:div.font-bold.pl-1 name]]])

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
             videos))]]
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

(defn app []
  (-> (js/fetch (str server "/groopse"))
      (.then (fn [res] (.json res)))
      (.then (fn [json] (swap! state assoc :groopse (js->clj json :keywordize-keys true))))
      (.catch prn))
  (let [new-name (r/atom "")]
    (fn []
      [:> (.-BrowserRouter router)
       [:div.flex.flex-col.h-full
        [:header.bg-green-700.text-white
         [:div.max-w-4xl.mx-auto.w-full.text-center.py-2.text-lg "GrootLapse"]]
        [:main.flex-1.relative
         [:button.absolute.bg-green-800.text-white.px-2.py-4.text-3xl.rounded-full.shadow-xl.border.border-black
          {:style {:right "1rem" :bottom "1rem"}}
          "Add"]
         [:> (.-Route router)
          {:path "/" :exact true}
          [:div.flex.flex-wrap
           (map groopse-overview (:groopse @state))]]
         [:> (.-Route router)
          {:path "/:name"
           :render (fn [props] (r/as-element [groopse-details (js->clj props :keywordize-keys true)]))}]
         (comment
           [:button {:on-click
                     (fn []
                       (.playStream ^js @stream-server)
                       (swap! state assoc :new-groopse true))}
            "Start stream"]
           [:button {:on-click
                     (fn []
                       (.stopStream ^js @stream-server)
                       (swap! state assoc :new-groopse false))}
            "Stop stream"]
           [:input {:value @new-name
                    :on-change (fn [e] (reset! new-name ^js (.-target.value e)))}]
           [:button {:on-click
                     (fn []
                       (->(js/fetch (str "http://" server-name ":3000/groopse")
                                    (clj->js
                                     {:method "POST"
                                      :headers {"Content-type" "application/json"}
                                      :body (js/JSON.stringify (clj->js {:name "flower"}))}))
                          (.catch prn)))}
            "Create groopse"]
           [:button {:on-click
                     (fn []
                       (-> (js/fetch (str "http://" server-name ":3000/groopse/" @new-name)
                                     (clj->js
                                      {:headers {"Content-type" "application/json"}}))
                           (.then (fn [res] (.json res)))
                           (.then (fn [images]
                                    (swap! state update :groopse assoc @new-name images)))))}
            "Load groopse"]
           [:canvas#canvas
            {:style {:display (if (:new-groopse @state) "block" "none")}
             :ref (fn [ref]
                    (when (and ref (= nil @stream-server))
                      (let [uri (str "ws://axidraw:8080")
                            wsavc (js/window.WSAvcPlayer. ref "webgl" 1 35)]
                        (.connect wsavc uri)
                        (reset! stream-server wsavc))))}]
           [:div "New GrootLapse"])
         (map (fn [img]
                [:img {:key img :src img}])
              (get (:groopse @state ) @new-name))]
        [:footer.bg-green-700.text-white
         [:div.max-w-4xl.mx-auto.w-full.text-center.py-1 "Birthday edition"]]]])))


(dom/render [app] (js/document.getElementById "app"))
(defn init [])

(comment
  (->(js/fetch (str "http://" server-name ":3000/groopse")
               (clj->js
                {:method "POST"
                 :headers {"Content-type" "application/json"}
                 :body (js/JSON.stringify (clj->js {:name "flower3"}))}))
     (.catch prn))
  (->(js/fetch (str "http://" server-name ":3000/groopse/flower3/stitch")
               (clj->js
                {:headers {"Content-type" "application/json"}}))
     (.then (fn [res] (.json res)))
     (.then prn)
     (.catch prn)))
