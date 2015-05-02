(ns minimalism.core
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [minimalism.editor :refer [content]]
            [minimalism.code-mirror :refer [code-mirror-editor]]))

(def markdown
  "#title
 Item 1
* Item 2
= Heading 1
== Heading 2
=== Heading 3
==== Heading 4

**bold**
//italic//

* Bullet list
* Second item
** Sub item

# Numbered list
# Second item
## Sub item

[[link]]
{{image}}
<<video>>

|= table |= hdr |=
| table | row |
| table | row |

> blockquote

{{{ unformatted text }}}
--- horizontal rule")

(defonce app-state (atom
                     {:text [markdown]}))

(defn example [data owner]
  (reify
    om/IRender
    (render [_]
      (om/build code-mirror-editor
                {:value (:text data)
                 :style  {:border "10px solid black"}
                 :text-area-class  "form-control"
                 :text-area-style  {:minHeight "10em"}
                 :code-mirror-options {:mode "markdown"
                                      :theme "solarized dark"}}))))

(defn main []
  (om/root
    example
    app-state
    {:target (. js/document (getElementById "app"))}))
