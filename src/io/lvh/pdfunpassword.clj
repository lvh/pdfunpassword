(ns io.lvh.pdfunpassword
  (:require
   [clojure.edn :as edn]
   [babashka.process :as p]
   [clojure.string :as str]
   [babashka.fs :as fs]
   [babashka.cli :as cli]))

(defn load-config!
  []
  (-> "pdfunpassword.edn" slurp edn/read-string))

(defn get-onepassword-output!
  [config-entry]
  (let [{:keys [account vault name field-label] :or {field-label "password"}} config-entry]
    (-> (p/shell
         {:out :string}
         "op" "item" "get" "--reveal"
         "--account" account
         "--vault" vault
         "--fields" (str "label=" field-label)
         name)
        :out
        str/trim)))

(def filters
  {:digits (fn [s] (str/replace s #"[^\d]" ""))})

(defn get-password!
  [config-entry]
  (let [clean (->> config-entry :filters (map filters) (apply comp))]
    (-> config-entry get-onepassword-output! clean)))

(defn password-protected?
  "Check if a PDF requires a password. Returns true if password-protected, false otherwise."
  [pdf]
  (let [{:keys [exit]} (p/sh {:continue true :err :string} "qpdf" "--requires-password" (str pdf))]
    (zero? exit)))

(defn remove-password!
  [config pdf]
  (let [pdf (fs/path pdf)]
    (println "Processing:" (str pdf))
    (if-not (password-protected? pdf)
      (println "  Already passwordless, skipping")
      (loop [[entry & remaining] config]
        (println "  Trying entry" entry)
        (let [password (get-password! entry)
              argv ["qpdf"
                    (str "--password=" password)
                    "--decrypt"
                    (str pdf)
                    "--replace-input"]
              {:keys [exit]} (apply p/sh {:continue true :err :inherit} argv)]
          (cond
            (zero? exit) (println "  Success!")
            (seq remaining) (recur remaining)
            :else (throw (ex-info "Failed to remove password" {:pdf pdf}))))))))

(defn find-pdfs
  [dir recursive?]
  (let [dir (fs/expand-home dir)]
    (if recursive?
      ;; **/*.pdf doesn't match top-level files, so we need both patterns
      (concat (fs/glob dir "*.pdf")
              (fs/glob dir "**/*.pdf"))
      (fs/glob dir "*.pdf"))))

(defn -main
  [& args]
  (let [spec {:recursive {:default false
                          :coerce :boolean
                          :desc "Recursively process subdirectories"}}
        {:keys [opts args]} (cli/parse-args args {:spec spec})
        {:keys [recursive]} opts
        dirs args]
    (when (empty? dirs)
      (println "Usage: bb go [--recursive] <dir1> [dir2] ...")
      (System/exit 1))
    (doseq [dir dirs
            pdf (find-pdfs dir recursive)]
      (remove-password! (load-config!) pdf))))
