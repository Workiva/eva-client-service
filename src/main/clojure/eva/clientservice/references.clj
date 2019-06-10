(ns eva.clientservice.references
  (:import (com.workiva.eva.clientservice.reference SnapshotReference)))

(defmethod print-method SnapshotReference [ref ^java.io.Writer w]
  (.write w (.toString ref)))
