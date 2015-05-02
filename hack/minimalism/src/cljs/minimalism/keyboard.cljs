(ns minimalism.keyboard
  (:require
    [goog.events :as events]
    [goog.events.EventType]
    [cljs.core.async :refer  [put! chan]]))

(def keyboard (chan))

(def keyword->event-type
  {:keyup goog.events.EventType.KEYUP
   :keydown goog.events.EventType.KEYDOWN
   :keypress goog.events.EventType.KEYPRESS
   :click goog.events.EventType.CLICK
   :dblclick goog.events.EventType.DBLCLICK
   :mousedown goog.events.EventType.MOUSEDOWN
   :mouseup goog.events.EventType.MOUSEUP
   :mouseover goog.events.EventType.MOUSEOVER
   :mouseout goog.events.EventType.MOUSEOUT
   :mousemove goog.events.EventType.MOUSEMOVE
   :focus goog.events.EventType.FOCUS
   :blur goog.events.EventType.BLUR})

(defn charcode->char
  [code]
  (js/String.fromCharCode code))

(defonce kb
  (events/listen js/document
                 (keyword->event-type :keypress)
                 #(->> %
                       (.-keyCode)
                       (charcode->char)
                       (put! keyboard))))
