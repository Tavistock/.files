(ns minimalism.markdown
  (:require [clojure.string :as string]))

(declare ^{:dynamic true} formatter)

(declare ^{:dynamic true} *substring*)

(declare ^:dynamic *next-line*)

(defn escape-code [s]
  (-> s
      (string/replace #"&" "&amp;")
      (string/replace #"\*" "&#42;")
      (string/replace #"\^" "&#94;")
      (string/replace #"\_" "&#95;")
      (string/replace #"\~" "&#126;")
      (string/replace #"\<" "&lt;")
      (string/replace #"\>" "&gt;")
      ;(string/replace #"\/" "&frasl;") ;screws up ClojureScript compiling
      (string/replace #"\[" "&#91;")
      (string/replace #"\]" "&#93;")
      (string/replace #"\(" "&#40;")
      (string/replace #"\)" "&#41;")
      (string/replace #"\"" "&quot;")))

(defn escape-link [& xs]
  (->
    (string/join (apply concat xs))
    (string/replace #"\*" "&#42;")
    (string/replace #"\^" "&#94;")
    (string/replace #"\_" "&#95;")
    (string/replace #"\~" "&#126;")
    seq))

(defn escaped-chars [text state]
  [(if (or (:code state) (:codeblock state))
     text
     (-> text
         (string/replace #"\\\\" "&#92;")
         (string/replace #"\\`" "&#8216;")
         (string/replace #"\\\*" "&#42;")
         (string/replace #"\\_" "&#95;")
         (string/replace #"\\\{" "&#123;")
         (string/replace #"\\\}" "&#125;")
         (string/replace #"\\\[" "&#91;")
         (string/replace #"\\\]" "&#93;")
         (string/replace #"\\\(" "&#40;")
         (string/replace #"\\\)" "&#41;")
         (string/replace #"\\#" "&#35;")
         (string/replace #"\\\+" "&#43;")
         (string/replace #"\\-" "&#45;")
         (string/replace #"\\\." "&#46;")
         (string/replace #"\\!" "&#33;")))
   state])

(defn text->sym [line]
  (map #(hash-map :type #{} :span #{} :text % :used nil) (string/split line "")))

(defn add-span [sym span]
  (update-in sym [:span] #(conj % span)))

(defn map-span [span syms]
  (map #(add-span % span) syms))

(defn map-terminal-span [span syms]
  (->> syms
      (map-span span)
      (map #(assoc % :used true))))

(defn sym->text [syms]
  (apply str (map :text syms)))

(defn separator [line span separator state]
  (loop [out []
         buf []
         phrase []
         found false
         symbols line]
    (cond
      ;no tokens left to find
      (empty? symbols)
      [(conj out buf) state]

      ;found starting token
      found
      (if (= phrase separator)
        (recur (conj out (map-span span buf))
               []
               []
               false
               (rest symbols))
        (recur out
               (conj buf (first symbols))
               (if (>= (count phrase) (count phrase))
                 (into [] (conj (drop 1 phrase) (:text (first symbols))))
                 (conj phrase (:text (first symbols))))
               found
               (rest symbols)))

      ;looking for starting token
      (= phrase separator)
      (recur out buf [] true (rest symbols))

      ;nothing interesting keep on looking
      :default
      (recur (into out (first symbols))
             buf
             (if (>= (count phrase) (count separator))
               (into [] (conj (drop 1 phrase) (:text (first symbols))))
               (conj phrase (:text (first symbols))))
             found
             (rest symbols)))))

(defn strong [text state]
  (separator text :strong [\* \*] state))

(defn bold [text state]
  (separator text :bold [\_ \_] state))

(defn em [text state]
  (separator text :em [\*] state))

(defn italics [text state]
  (separator text :italics [\_] state))

(defn inline-code [text state]
  (separator text :code [\`] state))

(defn strikethrough [text state]
  (separator text :strikethrough [\~ \~] state))

(defn heading-text [text]
  (->> text
       (drop-while #(or (= \# %) (= \space %)))
       (take-while #(not= \# %))
       (string/join)
       (string/trim)))

(defn heading-level [text]
  (let [num-hashes (count (filter #(not= \space %) (take-while #(or (= \# %) (= \space %)) (seq text))))]
    (if (pos? num-hashes) num-hashes)))

(defn make-heading [text heading-anchors]
  (if-let [heading (heading-level text)]
    (let [text (heading-text text)]
      (str "{:type :h" heading " :text" text "}"))))

(defn heading [text state]
  (cond
    (or (:codeblock state) (:code state))
    [text state]

    [(str "<h1>" text "</h1>") (assoc state :heading true)]

    [(str "<h2>" text "</h2>") (assoc state :heading true)]

    :else
    (if-let [heading (make-heading text (:heading-anchors state))]
      [heading (assoc state :heading true)]
      [text state])))

(defn br [text {:keys [code lists] :as state}]
  [(if (and (= [\space \space] (take-last 2 text))
            (not (or code lists)))
     (str (apply str (drop-last 2 text)) "<br />")
     text)
   state])

(defn autourl-transformer [text state]
  [(if (:code state)
     text
     (clojure.string/replace
       text
       #"<https?://[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]>"
       #(let [url (subs % 1 (dec (count %)))]
          (str "<a href=\"" url "\">" url "</a>"))))
   state])

(defn paragraph-text [last-line-empty? text]
  (if (and (not last-line-empty?) (not-empty text))
    (str " " text) text))

(defn paragraph
  [text {:keys [eof heading hr code
                lists blockquote paragraph
                last-line-empty?] :as state}]
  (cond
    (or heading hr code lists blockquote)
    [text state]

    paragraph
    (if (or eof (empty? (string/trim text)))
      [(str (paragraph-text last-line-empty? text) "</p>") (assoc state :paragraph false)]
      [(paragraph-text last-line-empty? text) state])

    (and (not eof) last-line-empty?)
    [(str "<p>" text) (assoc state :paragraph true :last-line-empty? false)]

    :default
    [text state]))

(defn hr [text state]
  (if (:code state)
    [text state]
    (if (and
          (or (empty? (drop-while #{\* \space} text))
              (empty? (drop-while #{\- \space} text))
              (empty? (drop-while #{\_ \space} text)))
          (> (count (remove #{\space} text)) 2))
      [(str "<hr/>") (assoc state :hr true)]
      [text state])))

(defn blockquote [text {:keys [eof code codeblock lists] :as state}]
  (let [trimmed (string/trim text)]
    (cond
      (or code codeblock lists)
      [text state]

      (:blockquote state)
      (if (or eof (empty? trimmed))
        ["</p></blockquote>" (assoc state :blockquote false)]
        (if (= ">-" (subs trimmed 0 2))
          [(str "</p><footer>" (subs text 2) "</footer><p>") state]
          [(str text " ") state]))

      :default
      (if (= \> (first text))
        [(str "<blockquote><p>" (string/join (rest text)) " ") (assoc state :blockquote true)]
        [text state]))))

(defn close-lists [lists]
  (string/join
    (for [[list-type] lists]
      (str "</li></" (name list-type) ">"))))


(defn add-row [row-type list-type num-indents indents content state]
  (if list-type
    (cond
      (< num-indents indents)
      (let [lists-to-close (take-while #(> (second %) num-indents) (reverse (:lists state)))
            remaining-lists (vec (drop-last (count lists-to-close) (:lists state)))]

        [(apply str (close-lists lists-to-close) "</li><li>" content)
         (assoc state :lists (if (> num-indents (second (last remaining-lists)))
                               (conj remaining-lists [row-type num-indents])
                               remaining-lists))])

      (> num-indents indents)
      [(str "<" (name row-type) "><li>" content)
       (update-in state [:lists] conj [row-type num-indents])]

      (= num-indents indents)
      [(str "</li><li>" content) state])

    [(str "<" (name row-type) "><li>" content)
     (assoc state :lists [[row-type num-indents]])]))

(defn ul [text state]
  (let [[list-type indents] (last (:lists state))
        num-indents (count (take-while (partial = \space) text))
        content (string/trim (*substring* text (inc num-indents)))]
    (add-row :ul list-type num-indents indents (or (make-heading content false) content) state)))

(defn ol [text state]
  (let [[list-type indents] (last (:lists state))
        num-indents (count (take-while (partial = \space) text))
        content (string/trim (string/join (drop-while (partial not= \space) (string/trim text))))]
    (add-row :ol list-type num-indents indents (or (make-heading content false) content) state)))

(defn li [text {:keys [code codeblock last-line-empty? eof lists] :as state}]
  (cond

    (and last-line-empty? (string/blank? text))
    [(str (close-lists (reverse lists)) text)
     (-> state (dissoc :lists) (assoc :last-line-empty? false))]

    (and (not eof)
         lists
         (string/blank? text))
    [text (assoc state :last-line-empty? true)]

    :else
    (let [indents (if last-line-empty? 0 (count (take-while (partial = \space) text)))
          trimmed (string/trim text)
          in-list? (:lists state)]
      (cond
        (re-find #"^[\*\+-] " trimmed)
        (ul (if in-list? text trimmed) state)

        (re-find #"^[0-9]+\. " trimmed)
        (ol (if in-list? text trimmed) state)

        (pos? indents)
        [text state]

        (and (or eof last-line-empty?)
             (not-empty lists))
        [(close-lists (reverse lists))
         (assoc state :lists [] :buf text)]

        :else
        [text state]))))

(def transformer-vector
  [escaped-chars
   hr
   li
   heading
   italics
   em
   strong
   bold
   strikethrough
   blockquote
   paragraph])

(defn apply-transform
  [[text state] transformer]
  (transformer text state))

(defn- init-transformer []
  (fn [edn line next-line state]
    (binding [*next-line* next-line]
      (let [[text new-state] (reduce apply-transform [line state] transformer-vector)]
        [(conj edn text) new-state]))))

(defn format "Removed from cljs.core 0.0-1885, Ref. http://goo.gl/su7Xkj"
  [fmt & args] (apply goog.string/format fmt args))

(defn md->edn
  ""
  [text]
  (binding [*substring* (fn [s n] (apply str (drop n s)))
            formatter format]
    (let [lines       (.split text "\n")
          transformer (init-transformer)]
      (loop [[line & more] lines
             edn []
             state {:last-line-empty? true}]
        (let [state
              (if (:buf state)
                (transformer edn (:buf state) (first more) (-> state
                                                               (dissoc :buf :lists)
                                                               (assoc :last-line-empty? true)))
                state)]
          (if (first more)
            (let [[next-edn next-state] (transformer edn line (first more) state)]
              (recur
                more
                next-edn
                (assoc next-state :last-line-empty? (empty? line))))
            (transformer edn line "" (assoc state :eof true))))))))

