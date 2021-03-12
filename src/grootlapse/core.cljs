(ns grootlapse.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            ["react-router-dom" :as router]))

(defonce stream-server (atom nil))
(defonce state (r/atom {}))
(def server-name "192.168.178.20")
(def server (str "http://" server-name ":3000"))

(defn groopse-overview [{:keys [name image]}]
  [:<> {:key image}
   [:> (.-Link router) {:class "w-1/2 p-4" :to (str "/" name)}
    [:img.rounded-xl.mb-1 {:src image}]
    [:div.font-bold.pl-1 name]]])


(defn groopse-details [props]
  [:div.p-4
   [:button.mb-4 {:on-click (:goBack (:history props))} "< Back"]
   [:h1.text-2xl (:name (:params (:match props)))]])

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

