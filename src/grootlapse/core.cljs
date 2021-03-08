(ns grootlapse.core
  (:require [reagent.dom :as dom]
            [reagent.core :as r]))
(defonce server (atom nil))
(defonce state (r/atom nil))

(defn app []
  [:div
   [:header "GrootLapse"]
   [:main
    [:button {:on-click
              (fn []
                (.playStream ^js @server)
                (swap! state assoc :new-groopse true))}
     "Start"]
    [:button {:on-click
              (fn []
                (.stopStream ^js @server)
                (swap! state assoc :new-groopse false))}
     "stop"]
    [:canvas#canvas
     {:style {:display (if (:new-groopse @state) "block" "none")}
      :ref (fn [ref] (prn ref)
             (when (and ref (= nil @server))
               (let [uri (str "ws://axidraw:8080")
                     wsavc (js/window.WSAvcPlayer. ref "webgl" 1 35)]
                 (.connect wsavc uri)
                 (reset! server wsavc))))}]
    [:div "New GrootLapse"]]])


(dom/render [app] (js/document.getElementById "app"))
(defn init [])

