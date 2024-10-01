(ns io.lvh.pdfunpassword
  (:require
   [clojure.edn :as edn]
   [babashka.process :as p]
   [clojure.string :as str]
   [babashka.fs :as fs]))

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

(defn remove-password!
  [config pdf]
  (let [pdf (fs/path pdf)]
    (loop [[entry & remaining] config]
      (println "Trying entry" entry)
      (let [password (get-password! entry)
            argv ["qpdf"
                  (str "--password=" password)
                  "--decrypt"
                  (str pdf)
                  "--replace-input"]
            {:keys [exit]} (apply p/sh {:continue true :err :inherit} argv)]
        (cond
          (zero? exit) (println "Success!")
          (seq remaining) (recur remaining)
          :else (throw (ex-info "Failed to remove password" {:pdf pdf})))))))

(defn -main
  [& dirs]
  (doseq [dir dirs
          pdf (-> dir fs/expand-home (fs/glob "*.pdf"))]
    (remove-password! (load-config!) pdf)))
