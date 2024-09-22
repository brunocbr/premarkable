#!/usr/bin/env bb
(ns ^{:author "Bruno Loureiro Conte"
      :doc "A simple previewer for Markdown and other plain text files"}
 premarkable.core
  (:require [org.httpkit.server :as http]
            [ruuter.core :as ruuter]
            [babashka.process :refer [process check]]
            [clojure.tools.cli :as cli]
            [hiccup2.core :as h]
            [clojure.string :as str]))

(defonce config (atom {}))

(def default-pandoc-args ["env" "pandoc" "-f" "markdown" "-t" "html" "-s"])

(defn parse-args [arg-str]
  (re-seq #"\"[^\"]*\"|\S+" arg-str))

(defn clean-arg [arg]
  (if (and (str/starts-with? arg "\"") (str/ends-with? arg "\""))
    (subs arg 1 (dec (count arg)))
    arg))

(defn build-command [arg-str]
  (mapv clean-arg (parse-args arg-str)))

(def cli-options
  [["-S" "--css CSS" "Pathname for a CSS file"]
   ["-P" "--processor CMD" "Processor command and arguments"
    :default default-pandoc-args :parse-fn build-command]
   ["-p" "--http-port PORT" "Port number where to run the server"
    :default 8082 :parse-fn #(Integer/parseInt %)]
   ["-h" "--help" "Show help"]
   ["-w" "--max-width N" "Limit text width in Preview (px)"
    :default nil :parse-fn #(Integer/parseInt %)]])

(defn markdown-to-html-stream
  "Converts a Markdown file to HTML using Pandoc and returns the HTML as a stream."
  [{:keys [source processor-args]}]
  (let [pandoc-process (process (concat processor-args
                                        [source])
                                {:out :string})]
    (-> @pandoc-process :out)))

(defn render-html-document [{:keys [css max-width] :as options}]
  (str (h/html
        (h/raw "<!DOCTYPE html>")
        [:html
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
          [:title "Preview"]
          [:style (h/raw (slurp css))]]
         [:body.normal
          [:div#wrapper {:style
                         (str
                          (when max-width (format "max-width: %dpx; " max-width))
                          "margin: 0 auto")}
           (h/raw
            (markdown-to-html-stream options))]]])))

(def routes [{:path "/"
              :method :get
              :response (fn [_]
                          {:status 200
                           :body (render-html-document @config)})}])

(defn -main [& args]
  (println "premarkable - A simple Markdown previewer")
  (println "=========================================")
  (let [{:keys [options arguments summary]}
        (cli/parse-opts args cli-options)]
    (if (:help options)
      (println summary)
      (do
        (let [source (first arguments)
              {:keys [css processor http-port max-width]} options]
          (println "- CSS path:" css)
          (println "- Source path:" source)
          (println "- Processor arguments:" processor)

          (reset! config {:source source
                          :css css
                          :max-width max-width
                          :processor-args processor})
          (http/run-server #(ruuter/route routes %) {:port http-port})
          (println "The server is now running on" (format "http://localhost:%s/" http-port))
          @(promise))))))

(apply -main *command-line-args*)
