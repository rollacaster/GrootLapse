(ns grootlapse.core
  (:require [reagent.dom :as dom]
            [reagent.core :as r]))
(defonce stream-server (atom nil))
(defonce state (r/atom {:groopse {}}))
(def server-name "axidraw")

(defn app []
  (let [new-name (r/atom "")]
    (fn []
      [:div
       [:header "GrootLapse"]
       [:main
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
        [:div "New GrootLapse"]
        (map (fn [img]
               [:img {:key img :src img}])
             (get (:groopse @state ) @new-name))]])))


(dom/render [app] (js/document.getElementById "app"))
(defn init [])

