(ns grootlapse.core
  (:require [reagent.dom :as dom]))
(defonce server (atom nil))

(defn app []
  [:div
   [:header "GrootLapse"]
   [:main
    [:button {:on-click
              (fn []
                (.playStream ^js @server))}
     "Create"]
    [:canvas {:ref (fn [ref] (prn ref)
                     (when (and ref (= nil @server))
                       (let [uri (str "ws://axidraw:8080")
                             wsavc (js/window.WSAvcPlayer. ref "webgl" 1 35)]
                         (.connect wsavc uri)
                         (reset! server wsavc))))}]
    [:div "New GrootLapse"]]])


(dom/render [app] (js/document.getElementById "app"))
(defn init [])

