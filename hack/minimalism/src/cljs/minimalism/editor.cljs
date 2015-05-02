(ns minimalism.editor
  (:require-macros [cljs.core.async.macros :refer [go]])
       (:require [om.core :as om :include-macros true]
                 [sablono.core :as html :refer-macros [html]]
                 [cljs.core.async :refer [<!]]
                 [minimalism.keyboard :refer [keyboard]]))

(defn handle-key [state e]
  (om/transact! state (fn [s] (update-in s [:text 0] #(str % e)))))

(defn highlight [text]
  (.-value (js/hljs.highlight "markdown" text)))

(defn content [{:keys [text] :as app-state} owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (go (while true
            (handle-key app-state (<! keyboard)))))
    om/IDidMount
    (did-mount [_]
      (set! (.-innerHTML (om/get-node owner))
            (highlight (first text))))
    om/IDidUpdate
    (did-update
      [_ _ _]
      (set! (.-innerHTML (om/get-node owner))
            (highlight (first text))))
    om/IRenderState
    (render-state [state _]
      (html [:div.content.hljs.markdown
             (first text)]))))
