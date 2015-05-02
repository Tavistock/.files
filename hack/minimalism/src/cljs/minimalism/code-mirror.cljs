(ns minimalism.code-mirror
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn handle-change [editor! {:keys [read-only on-change value] :as data}]
  (when (and (not read-only) on-change)
    (on-change value (.getValue editor!))))

(defn ref-cursor-on-change
  [ref-cursor value]
  (om/update! ref-cursor [0] value))

(defn code-mirror-editor
  [{:keys [value default-value on-change
           style      text-area-style
           class-name text-area-class-name
           read-only  force-text-area
           code-mirror-options]
    ;; dear god please forgive me, for I have sinned
    :or {on-change ref-cursor}
    :as data}
   owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (let [editor (.fromTextArea js/CodeMirror
                                  (om/get-node owner "editor")
                                  (clj->js code-mirror-options))]
        (do (om/update-state! owner #(merge %  {:editor editor}))
            (.on editor "change" #(handle-change editor data)))))
    om/IDidUpdate
    (did-update [_ _ _]
      (when read-only
        (.setValue (:editor (om/get-state owner)) default-value)))
    om/IRenderState
    (render-state [this state]
      (html [:div {:style     style
                   :className class-name}
             [:textarea {:ref          "editor"
                         :value        (nth value 0)
                         :readOnly     read-only
                         :defaultValue default-value
                         :style        text-area-style
                         :className    text-area-class-name}]]))))
