(ns grootlapse.app
  (:require [grootlapse.components :refer [spinner]]
            [reagent.core :as r]
            [reagent.dom :as dom]
            ["react-router-dom" :as router]
            [clojure.string :as s]
            [fork.reagent :as fork]
            [vlad.core :as vlad]))

(defonce stream-server (atom nil))
(defonce state (r/atom {:groopse "LOADING"}))
(def server-name js/location.host)
(def server js/location.origin)

(defn icon [path size color]
  (let [sizes {:small 12 :medium 24 :large 48}]
    [:svg {:width (size sizes) :height (size sizes) :viewBox "0 0 24 24"}
     [:path {:d path :fill color}]]))

(defn tree-icon
  ([] (icon "M12.297 5.574l-.005-.005s1.703-1.754 2.388-2.259c1.181-.871 2.743-.339 2.743-.339s.021 1.465-.936 2.06c-.913.568-1.788.333-1.882.307.355-.5.853-1.165 1.521-1.498-.493-.028-1.09.246-1.557.681-.38.353-1.26 1.257-1.57 1.576v3.1c1.068-.86 3.035-2.439 3.603-2.877 1.568-1.212 3.691-.522 3.691-.522s.028 1.963-1.254 2.76c-1.223.76-2.753.298-2.878.263.605-.668 1.563-1.277 2.457-1.722-.898-.122-1.614.157-2.749 1.06-.789.629-2.491 2.013-2.87 2.321v3.201c1.229-.929 3.753-2.811 4.9-3.452 2.85-1.593 5.101-.284 5.101-.284s-.263 2.57-2.061 3.417c-1.714.808-3.837-.076-3.996-.14.893-.782 2.197-1.467 3.585-1.816-1.316-.209-2.626-.195-4.415 1.213-.95.747-2.665 1.997-3.114 2.323l.001 9.08h-2l-.001-9.08c-.45-.328-2.162-1.576-3.112-2.323-1.789-1.408-3.099-1.422-4.415-1.213 1.388.349 2.692 1.034 3.585 1.816-.159.064-2.282.948-3.996.14-1.798-.847-2.061-3.417-2.061-3.417s2.251-1.309 5.101.284c1.146.641 3.67 2.521 4.898 3.452v-3.198c-.37-.301-2.081-1.693-2.874-2.324-1.135-.903-1.851-1.182-2.749-1.06.895.445 1.853 1.054 2.457 1.722-.125.035-1.655.497-2.877-.263-1.282-.797-1.254-2.76-1.254-2.76s2.122-.69 3.691.522c.568.439 2.539 2.021 3.606 2.88v-3.105c-.311-.321-1.189-1.222-1.568-1.574-.467-.435-1.064-.709-1.557-.681.668.333 1.166.998 1.521 1.498-.094.026-.969.261-1.882-.307-.957-.595-.936-2.06-.936-2.06s1.562-.532 2.743.339c.685.505 2.388 2.259 2.388 2.259l-.006.007.297.276.298-.278zm2.895 9.386c.389-.138 2.022-.636 3.583.12 1.788.866 2.037 3.461 2.037 3.461s-2.178 1.42-3.978.549c-1.717-.831-2.297-2.637-2.347-2.801 1.177.203 2.363.618 3.494 1.305-.847-1.062-1.783-1.926-2.789-2.634zm-6.384 0c-.389-.138-2.022-.636-3.583.12-1.788.866-2.037 3.461-2.037 3.461s2.178 1.42 3.978.549c1.717-.831 2.297-2.637 2.347-2.801-1.177.203-2.363.618-3.494 1.305.847-1.062 1.783-1.926 2.789-2.634zm3.664-10.973c.175-.175.866-.929.861-1.969-.005-1.19-1.344-1.996-1.344-1.996s-1.332.811-1.326 2.009c.006 1.143.832 1.924.908 1.993.194-.689.276-1.437.198-2.227.355.734.58 1.463.703 2.19z" :medium "black"))
  ([size] (icon "M12.297 5.574l-.005-.005s1.703-1.754 2.388-2.259c1.181-.871 2.743-.339 2.743-.339s.021 1.465-.936 2.06c-.913.568-1.788.333-1.882.307.355-.5.853-1.165 1.521-1.498-.493-.028-1.09.246-1.557.681-.38.353-1.26 1.257-1.57 1.576v3.1c1.068-.86 3.035-2.439 3.603-2.877 1.568-1.212 3.691-.522 3.691-.522s.028 1.963-1.254 2.76c-1.223.76-2.753.298-2.878.263.605-.668 1.563-1.277 2.457-1.722-.898-.122-1.614.157-2.749 1.06-.789.629-2.491 2.013-2.87 2.321v3.201c1.229-.929 3.753-2.811 4.9-3.452 2.85-1.593 5.101-.284 5.101-.284s-.263 2.57-2.061 3.417c-1.714.808-3.837-.076-3.996-.14.893-.782 2.197-1.467 3.585-1.816-1.316-.209-2.626-.195-4.415 1.213-.95.747-2.665 1.997-3.114 2.323l.001 9.08h-2l-.001-9.08c-.45-.328-2.162-1.576-3.112-2.323-1.789-1.408-3.099-1.422-4.415-1.213 1.388.349 2.692 1.034 3.585 1.816-.159.064-2.282.948-3.996.14-1.798-.847-2.061-3.417-2.061-3.417s2.251-1.309 5.101.284c1.146.641 3.67 2.521 4.898 3.452v-3.198c-.37-.301-2.081-1.693-2.874-2.324-1.135-.903-1.851-1.182-2.749-1.06.895.445 1.853 1.054 2.457 1.722-.125.035-1.655.497-2.877-.263-1.282-.797-1.254-2.76-1.254-2.76s2.122-.69 3.691.522c.568.439 2.539 2.021 3.606 2.88v-3.105c-.311-.321-1.189-1.222-1.568-1.574-.467-.435-1.064-.709-1.557-.681.668.333 1.166.998 1.521 1.498-.094.026-.969.261-1.882-.307-.957-.595-.936-2.06-.936-2.06s1.562-.532 2.743.339c.685.505 2.388 2.259 2.388 2.259l-.006.007.297.276.298-.278zm2.895 9.386c.389-.138 2.022-.636 3.583.12 1.788.866 2.037 3.461 2.037 3.461s-2.178 1.42-3.978.549c-1.717-.831-2.297-2.637-2.347-2.801 1.177.203 2.363.618 3.494 1.305-.847-1.062-1.783-1.926-2.789-2.634zm-6.384 0c-.389-.138-2.022-.636-3.583.12-1.788.866-2.037 3.461-2.037 3.461s2.178 1.42 3.978.549c1.717-.831 2.297-2.637 2.347-2.801-1.177.203-2.363.618-3.494 1.305.847-1.062 1.783-1.926 2.789-2.634zm3.664-10.973c.175-.175.866-.929.861-1.969-.005-1.19-1.344-1.996-1.344-1.996s-1.332.811-1.326 2.009c.006 1.143.832 1.924.908 1.993.194-.689.276-1.437.198-2.227.355.734.58 1.463.703 2.19z" size "black"))
  ([size color] (icon "M12.297 5.574l-.005-.005s1.703-1.754 2.388-2.259c1.181-.871 2.743-.339 2.743-.339s.021 1.465-.936 2.06c-.913.568-1.788.333-1.882.307.355-.5.853-1.165 1.521-1.498-.493-.028-1.09.246-1.557.681-.38.353-1.26 1.257-1.57 1.576v3.1c1.068-.86 3.035-2.439 3.603-2.877 1.568-1.212 3.691-.522 3.691-.522s.028 1.963-1.254 2.76c-1.223.76-2.753.298-2.878.263.605-.668 1.563-1.277 2.457-1.722-.898-.122-1.614.157-2.749 1.06-.789.629-2.491 2.013-2.87 2.321v3.201c1.229-.929 3.753-2.811 4.9-3.452 2.85-1.593 5.101-.284 5.101-.284s-.263 2.57-2.061 3.417c-1.714.808-3.837-.076-3.996-.14.893-.782 2.197-1.467 3.585-1.816-1.316-.209-2.626-.195-4.415 1.213-.95.747-2.665 1.997-3.114 2.323l.001 9.08h-2l-.001-9.08c-.45-.328-2.162-1.576-3.112-2.323-1.789-1.408-3.099-1.422-4.415-1.213 1.388.349 2.692 1.034 3.585 1.816-.159.064-2.282.948-3.996.14-1.798-.847-2.061-3.417-2.061-3.417s2.251-1.309 5.101.284c1.146.641 3.67 2.521 4.898 3.452v-3.198c-.37-.301-2.081-1.693-2.874-2.324-1.135-.903-1.851-1.182-2.749-1.06.895.445 1.853 1.054 2.457 1.722-.125.035-1.655.497-2.877-.263-1.282-.797-1.254-2.76-1.254-2.76s2.122-.69 3.691.522c.568.439 2.539 2.021 3.606 2.88v-3.105c-.311-.321-1.189-1.222-1.568-1.574-.467-.435-1.064-.709-1.557-.681.668.333 1.166.998 1.521 1.498-.094.026-.969.261-1.882-.307-.957-.595-.936-2.06-.936-2.06s1.562-.532 2.743.339c.685.505 2.388 2.259 2.388 2.259l-.006.007.297.276.298-.278zm2.895 9.386c.389-.138 2.022-.636 3.583.12 1.788.866 2.037 3.461 2.037 3.461s-2.178 1.42-3.978.549c-1.717-.831-2.297-2.637-2.347-2.801 1.177.203 2.363.618 3.494 1.305-.847-1.062-1.783-1.926-2.789-2.634zm-6.384 0c-.389-.138-2.022-.636-3.583.12-1.788.866-2.037 3.461-2.037 3.461s2.178 1.42 3.978.549c1.717-.831 2.297-2.637 2.347-2.801-1.177.203-2.363.618-3.494 1.305.847-1.062 1.783-1.926 2.789-2.634zm3.664-10.973c.175-.175.866-.929.861-1.969-.005-1.19-1.344-1.996-1.344-1.996s-1.332.811-1.326 2.009c.006 1.143.832 1.924.908 1.993.194-.689.276-1.437.198-2.227.355.734.58 1.463.703 2.19z" size color)))

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
  ([{:keys [on-click pending class style type]} children]
   [:button.bg-green-700.text-white.p-2.rounded-xl.shadow.font-bold
    {:on-click (when-not pending on-click) :type (or type "button")
     :class class :style style}
    (if pending "Lade" children)]))

(defn groopse-overview [{:keys [name image]}]
  [:<> {:key (str name image)}
   [:> (.-Link router) {:class "w-1/2 p-4" :to (str "/" name)}
    [:img.rounded-2xl.mb-1 {:src image}]
    [:div.font-semibold.pl-1 name]]])
(def canvas-ref (atom nil))

(defn load-groopse []
  (-> (js/fetch (str "/groopse"))
      (.then (fn [res] (.json res)))
      (.then (fn [json] (swap! state merge (js->clj json :keywordize-keys true))))
      (.catch (fn [] (swap! state assoc :groopse "ERROR")))))

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
         [:div.flex.justify-between.mb-16
          [:h1.text-2xl.mb-4 (:name (:params (:match props)))]
          (when (= name (:name (:active @state)))
            [:div
             [:div.mb-2
              [:span.text-xl.pr-3 (case (:state (:active @state))
                                    "RUNNING" "Live"
                                    "WAITING" "Warte")]
              [button {:on-click (fn []
                                   (->(js/fetch (str "/groopse/active/stop")
                                                (clj->js
                                                 {:method "POST"
                                                  :headers {"Content-type" "application/json"}}))
                                      (.then (fn [] (swap! state assoc :active nil)))))} "Stop"]]
             [:div (case (:state (:active @state))
                                    "RUNNING" (str "Nächstes Foto: " (:next (:active @state)))
                                    "WAITING" (str "Start um: " (:start (:active @state)))) ]])]
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
                      (->(js/fetch (str "/groopse/" name "/stitch")
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
           (->> images
                reverse
                (take (if @show-all-images (count images) max-images))
                (map
                 (fn [image]
                   [:div.relative {:key image :style {:width "49%"}}
                    [:img.mb-2.border {:src image}]
                    [button {:class "absolute" :style {:top "0.5rem" :right "0.5rem"}
                             :on-click (fn [] (reset! delete-mode {:type :image
                                                                  :label (str "Bild " (s/join "/" (take-last 2 (s/split image #"/"))))
                                                                  :name (apply str (take-last 1 (s/split image #"/")))}))}
                     [trash-icon :small "white"]]])))]
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
                                                :groopse (str "/groopse/" name)
                                                :video (str "/groopse/" name "/video/" (:name @delete-mode))
                                                :image (str "/groopse/" name "/image/" (:name @delete-mode)))
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

(defn input [{:keys [value on-change on-blur id name type class on-click ref placeholder]}]
  [:input.border.rounded.w-full.p-2.font-bold
   {:id id :type type :name name :class class
    :value value :on-change on-change
    :placeholder placeholder
    :on-blur on-blur
    :on-click on-click :ref ref}])

(def groopse-new
  (with-meta
    (fn [props]
      [fork/form {:initial-values {"name" ""
                                   "interval" 10
                                   "start" ""}
                  :prevent-default? true
                  :validation #(vlad/field-errors
                                (vlad/join (vlad/attr ["name"] (vlad/present {:message "Bitte Name eingeben."}))
                                           (vlad/attr ["start"] (vlad/matches #"(^$|^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$)"
                                                                              {:message "Format HH:MM"})))
                                %)
                  :on-submit (fn [{:keys [state path values]}]
                               (when @stream-server
                                 (.stopStream ^js @stream-server)
                                 (reset! stream-server nil))
                               (swap! state #(fork/set-submitting % path true))
                               (->(js/fetch (str "/groopse")
                                            (clj->js
                                             {:method "POST"
                                              :headers {"Content-type" "application/json"}
                                              :body (js/JSON.stringify (clj->js values))}))
                                  (.then (fn [res]
                                           (swap! state #(fork/set-submitting % path false))
                                           (if (.-ok res)
                                             (do
                                               (load-groopse)
                                               ((:push (:history props)) "/"))
                                             (fork/set-server-message state path "Etwas ist schiefgelaufen!"))))
                                  (.catch (fn []
                                            (swap! state (fn [state]
                                                           (-> state
                                                               (fork/set-server-message path "Kein Verbindung zu Groot")
                                                               (fork/set-submitting path false))))))))}
       (let [preview (r/atom false)]
         (fn [{:keys [form-id values handle-submit handle-change handle-blur submitting? errors
                     on-submit-server-message touched]}]
           [:form.px-4.py-6
            {:id form-id
             :on-submit handle-submit}
            [:h1.text-lg.pl-2.pb-6.font-bold "Neuen Groopse erstellen"]
            [:div.pb-6
             [:label.block.mb-2.pl-2 {:for "groopse-name"}
              "Name"]
             [input
              {:id "groopse-name" :value (values "name") :on-change handle-change :on-blur handle-blur
               :name "name"}]
             (when (touched "name")
               [:div.text-red-400 (first (get errors (list "name")))])]
            [:div.pb-6.flex
             [:div.pr-3 {:class "w-1/2"}
              [:label.block.mb-2.pl-2 {:for "groopse-interval"}
               "Foto Frequenz"]
              [:div.relative
               [input
                {:id "groopse-interval" :type "number" :name "interval" :class "text-right pr-12"
                 :value (values "interval") :on-change handle-change :on-blur handle-blur}]
               [:span.absolute.font-light.text-sm {:style {:top "50%" :right "1rem" :transform "translateY(-50%)"}} "mins"]]]
             [:div.pl-3 {:class "w-1/2"}
              [:label.block.mb-2.pl-2 {:for "groopse-start"}
               "Start"]
              [:div
               [input {:id "groopse-start" :class "text-right" :type "time" :value (values "start")
                       :on-change handle-change
                       :placeholder "HH:MM"
                       :name "start"}]
               (when (touched "start")
               [:div.text-red-400 (first (get errors (list "start")))])]]]
            [:div.relative.pb-16
             [:div.mb-2.pl-2 "Preview"]
             [button {:class "absolute"
                      :style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"
                              :display (when @preview "none")}
                      :on-click (fn []
                                  (when (and @canvas-ref (= nil @stream-server))
                                    (let [uri (str "ws" (when (= js/location.protocol "https:") "s") "://" server-name)
                                          wsavc (js/window.WSAvcPlayer. @canvas-ref "webgl" 1 35)]
                                      (.connect wsavc uri)
                                      (reset! stream-server wsavc)))
                                  (js/setTimeout
                                   (fn []
                                     (.playStream ^js @stream-server)
                                     (reset! preview true))
                                   1000))}
              "Preview"]
             [:canvas.w-full.border.rounded.bg-white
              {:ref (fn [ref] (reset! canvas-ref ref))}]]
            ;; interval?
            ;; duration?
            ;; space calculation?
            [:div.w-full.flex.items-center
             {:class (if on-submit-server-message "justify-between" "justify-end")}
             (when on-submit-server-message
              [:div.text-red-400 on-submit-server-message])
             [button
              {:pending submitting? :type "submit"}
              "Erstellen"]]]))])
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
                              (->(js/fetch (str "/groopse/active/stop")
                                           (clj->js
                                            {:method "POST"
                                             :headers {"Content-type" "application/json"}}))
                                 (.then (fn [] (swap! state assoc :active nil)))))
                  :class "w-1/3"} "Stop"]])
      [:hr.w-full]])
   (case (:groopse @state)
     "ERROR"
     [:div.w-full.flex.justify-center.items-center.flex-col.py-4
      [:div.text-xl.mb-3 "Wo ist Groot?"]
      [:img {:src "/imgs/groot-sad.gif" :class "w-3/4 rounded"}]]
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
       [:div.max-w-4xl.mx-auto.w-full.text-center.py-2.text-lg.flex.justify-center
        [:span.pr-1 [tree-icon :medium "white"]]
        "GrootLapse"]]
      [:main.flex-1.relative.pt-4
       {:style {:background-color "#f3fff9"
                :background-image "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 40' width='80' height='40'%3E%3Cpath fill='%23a7f3d0' fill-opacity='0.4' d='M0 40a19.96 19.96 0 0 1 5.9-14.11 20.17 20.17 0 0 1 19.44-5.2A20 20 0 0 1 20.2 40H0zM65.32.75A20.02 20.02 0 0 1 40.8 25.26 20.02 20.02 0 0 1 65.32.76zM.07 0h20.1l-.08.07A20.02 20.02 0 0 1 .75 5.25 20.08 20.08 0 0 1 .07 0zm1.94 40h2.53l4.26-4.24v-9.78A17.96 17.96 0 0 0 2 40zm5.38 0h9.8a17.98 17.98 0 0 0 6.67-16.42L7.4 40zm3.43-15.42v9.17l11.62-11.59c-3.97-.5-8.08.3-11.62 2.42zm32.86-.78A18 18 0 0 0 63.85 3.63L43.68 23.8zm7.2-19.17v9.15L62.43 2.22c-3.96-.5-8.05.3-11.57 2.4zm-3.49 2.72c-4.1 4.1-5.81 9.69-5.13 15.03l6.61-6.6V6.02c-.51.41-1 .85-1.48 1.33zM17.18 0H7.42L3.64 3.78A18 18 0 0 0 17.18 0zM2.08 0c-.01.8.04 1.58.14 2.37L4.59 0H2.07z'%3E%3C/path%3E%3C/svg%3E\")"}}
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
       [:div.max-w-4xl.mx-auto.w-full.text-center.py-1 "Made with ❤️"]]]]))


(dom/render [app] (js/document.getElementById "app"))
(defn init [])

(comment
  (->(js/fetch (str "/groopse")
               (clj->js
                {:method "POST"
                 :headers {"Content-type" "application/json"}
                 :body (js/JSON.stringify (clj->js {:name "flower5"}))}))
     (.catch prn))
  (->(js/fetch (str "/groopse/flower3/stitch")
               (clj->js
                {:headers {"Content-type" "application/json"}}))
     (.then (fn [res] (.json res)))
     (.then prn)
     (.catch prn)))
