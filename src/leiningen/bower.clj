(ns leiningen.bower
  (:require [leiningen.help :as help]
            [leiningen.core.main :as main]
            [leiningen.npm :refer
             [with-json-file environmental-consistency transform-deps]]
            [cheshire.core :as json]
            [leiningen.npm.deps :refer [resolve-node-deps]]
            [leiningen.npm.process :refer [exec]]
            [robert.hooke]
            [leiningen.deps]))

(def ^:dynamic *bower-package-file* "bower.json")
(def ^:dynamic *bower-config-file* ".bowerrc")

(defn project->bowerrc
  [project]
  (json/generate-string
   {:directory (get-in project [:bower :directory])}))

(defn project->component
  [project]
  (json/generate-string
   {:name (project :name)
    :description (project :description)
    :version (project :version)
    :dependencies (transform-deps
                   (resolve-node-deps :bower-dependencies project))}))

(defn- invoke
  [project & args]
  (exec (project :root) (cons "bower" args)))

(defmacro with-bower-env [project-sym & forms]
  `(binding [*bower-package-file* (get-in ~project-sym [:bower :package-file] *bower-package-file*)
             *bower-config-file* (get-in ~project-sym [:bower :config-file] *bower-config-file*)]
     (environmental-consistency ~project-sym *bower-package-file* *bower-config-file*)
     ~@forms))

(defmacro with-bower-files [project & forms]
  `(with-bower-env ~project
     (with-json-file
       *bower-package-file* (project->component ~project) ~project
       (with-json-file
         *bower-config-file* (project->bowerrc ~project) ~project
         ~@forms))))

(defn bower
  "Invoke the Bower component manager."
  ([project]
     (with-bower-env project
       (println (help/help-for "bower"))
       (main/abort)))
  ([project & args]
     (with-bower-files project
       (apply invoke project args))))

(defn install-deps
  [project]
  (with-bower-files project
    (invoke project "run-script" "bower")))

(defn wrap-deps
  [f & args]
  (apply f args)
  (install-deps (first args)))

(defn install-hooks []
  (robert.hooke/add-hook #'leiningen.deps/deps wrap-deps))
