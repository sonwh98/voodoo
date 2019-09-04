(ns stigmergy.voodoo
  (:import [org.apache.commons.codec.binary Hex]
           [org.apache.commons.codec.digest DigestUtils]))

(defonce type->size {:byte 1
                     :byte* 0

                     :boolean 1
                     :boolean* 0
                     
                     :char 1
                     :char* 0
                     
                     :int16 2
                     :int16* 0
                     
                     :int32 4
                     :int32* 0})

(defn struct?
  "A struct simulates a C struct with a vector. A struct describes the binary layout of fields.
  Keywords are used for field names and field types. For example,
  (struct? [:name [:char 10]
            :age :int32])"
  [struct]
  (and (vector? struct) (> (count struct) 2 )))

(defn sizeof
  "return number of bytes of a struct or a field"
  [t] {:pre [(or (vector? t) (keyword? t))]}
  (cond
    (keyword? t) (type->size t)
    (struct? t) (let [field-type-pairs (partition 2 t)]
                  (reduce + (map (fn [[field type]]
                                   (sizeof type))
                                 field-type-pairs)))
    :else (let [[seq-type count] t]
            (* (sizeof seq-type) count))))

(defn struct-metadata
  "calculates size and offet of fields in the struct"
  [struct]
  (let [field-type-pairs (partition 2 struct)
        field-type-size (map (fn [[field type]]
                               [field {:type type
                                       :size (sizeof type)}])
                             field-type-pairs)
        field-type-size-offset (into {} (reduce (fn [acc [field md]]
                                                  (let [[last-field last-md] (last acc)
                                                        last-offset (:offset last-md)]
                                                    (conj acc (if last-offset
                                                                (let [size (:size last-md)
                                                                      offset (+ last-offset size)]
                                                                  [field (assoc md :offset offset)])
                                                                [field (assoc md :offset 0)]))))
                                                []
                                                field-type-size))]
    field-type-size-offset))

(defn toBytes
  "convert a seq into a byte-array"
  [a-seq]
  (cond
    (sequential? a-seq) (byte-array a-seq)
    (bytes? a-seq) a-seq
    :else [a-seq]))

(defn take-between [i j coll]
  (let [chunk (drop i coll)
        num (- j i)]
    (take num chunk)))

(defn bytes->int [a-seq]
  (.. (BigInteger. (toBytes a-seq))
      intValue)
  #_(let []
      (+  (bit-shift-left (nth bytes 0) 24)
          (bit-shift-left (nth  bytes 1) 16)
          (bit-shift-left (nth  bytes 2) 8)
          (bit-shift-left (nth  bytes 3) 0))))

(defn bytes->oct [a-seq]
  (.. (BigInteger. (toBytes a-seq))
      (toString 8)))

(defn bytes->char [a-seq]
  (map #(char %) a-seq))

(defn bytes->str [a-seq]
  (String. (toBytes a-seq)))

(defn bytes->hex [a-seq]
  (-> a-seq toBytes Hex/encodeHexString))

(defn hex-str->bytes [hex-str]
  (Hex/decodeHex hex-str))

(defn sha1-as-bytes [a-seq]
  (-> a-seq toBytes DigestUtils/sha1))

(defn pointer
  "returns a closure that simulates a C pointer given a struct and a seq of bytes"
  [struct a-seq]
  (let [metadata (struct-metadata struct)
        offset (atom 0)]
    (fn [arg0 & args]
      (if (-> args count zero?)
        (let [field arg0
              field-type (-> metadata field :type)
              field-offset (-> metadata field :offset)
              size (-> metadata field :size)]
          (if (= size 0)
            (let [size (count a-seq)]
              (take-between (+ @offset field-offset) size a-seq))
            (take-between (+ @offset field-offset) (+ @offset field-offset size) a-seq)))
        (let [+or- arg0
              next-offset (reduce + (map (fn [field-or-offset]
                                           (if (keyword? field-or-offset)
                                             (let [field field-or-offset]
                                               (-> field metadata :offset))
                                             (let [offset field-or-offset]
                                               field-or-offset)))
                                         args))]
          (swap! offset (fn [offset]
                          (+or- offset next-offset))))))))

(defn read-bytes
  "read num-of-bytes from input-stream and return as a byte-array"
  [input-stream num-of-bytes]
  (let [bytes (byte-array num-of-bytes)]
    (.. input-stream (read bytes))
    bytes))

(defn suck
  "like slurp but returns raw bytes"
  [file-name]
  (let [paths (clojure.string/split file-name #"/")
        root-dir (let [fp (first paths)]
                   (if (= "" fp)
                     "/"
                     fp))
        path (java.nio.file.Paths/get root-dir (into-array (rest paths)))]
    (java.nio.file.Files/readAllBytes path)))

(comment
  (sizeof [:name :int32
           :age :int32])
  )
