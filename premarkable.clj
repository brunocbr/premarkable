#!/usr/bin/env bb
(ns ^{:author "Bruno Loureiro Conte"
      :doc "A simple previewer for Markdown and other plain text files"}
 premarkable.core
  (:require [org.httpkit.server :as http]
            [ruuter.core :as ruuter]
            [babashka.process :refer [shell]]
            [babashka.fs :as fs]
            [clojure.tools.cli :as cli]
            [hiccup2.core :as h]
            [clojure.string :as str]))

(defonce config (atom {}))

(def default-pandoc-args ["env" "pandoc" "-f" "markdown" "-t" "html" "-s"])

;; Atom to store the processed content
(def pandoc-content (atom nil))

;; Atom to store the timestamp of the input file
(def input-file-timestamp (atom nil))

;; Function to check if the file has been modified
(defn file-modified? [file]
  (let [last-modified (fs/last-modified-time file)]
    (not= @input-file-timestamp last-modified)))

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

(defn process-pandoc
  [{:keys [source processor-args]}]
  (let [result (shell {:out :string}
                      (str/join " " (concat processor-args [source])))]
    (when (zero? (:exit result))  ;; Checks if Pandoc executed correctly
      (reset! pandoc-content (:out result))
      (reset! input-file-timestamp (fs/last-modified-time source)))))

;; Function to monitor the file and update the content every 5 seconds
(defn monitor-file [input]
  (while true
    (if (file-modified? input)
      (do
        (println "File modified, updating...")
        (process-pandoc @config)
        (println "Content updated")))
    (Thread/sleep 5000)))  ;; Waits 5 seconds before checking again

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
            @pandoc-content)]]])))

(def routes [{:path "/"
              :method :get
              :response (fn [_]
                          (if @pandoc-content
                            {:status 200
                             :body (render-html-document @config)}
                            {:status 200
                             :body "Content not processed yet"}))}])

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
          (future (monitor-file source))
          @(promise))))))

(apply -main *command-line-args*)
