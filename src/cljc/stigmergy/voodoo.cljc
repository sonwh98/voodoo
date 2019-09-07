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
        field-type-size-offset (into {} (reduce (fn [acc [field type-size]]
                                                  (let [[last-field last-type-size] (last acc)
                                                        last-offset (:offset last-type-size)]
                                                    (conj acc (if last-offset
                                                                (let [size (:size last-type-size)
                                                                      offset (+ last-offset size)]
                                                                  [field (assoc type-size :offset offset)])
                                                                [field (assoc type-size :offset 0)]))))
                                                []
                                                field-type-size))]
    field-type-size-offset))

(defn to-bytes
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
  (.. (BigInteger. (to-bytes a-seq))
      intValue)
  #_(let []
      (+  (bit-shift-left (nth bytes 0) 24)
          (bit-shift-left (nth  bytes 1) 16)
          (bit-shift-left (nth  bytes 2) 8)
          (bit-shift-left (nth  bytes 3) 0))))

(defn int->bytes [an-int]
  (.. (BigInteger/valueOf an-int) toByteArray))

(defn bytes->oct [a-seq]
  (.. (BigInteger. (to-bytes a-seq))
      (toString 8)))

(defn oct->bytes [an-octal]
  (.. (BigInteger. an-octal 8)
      toByteArray))

(defn bytes->char [a-seq]
  (map #(char %) a-seq))

(defn bytes->str [a-seq]
  (String. (to-bytes a-seq)))

(defn bytes->hex [a-seq]
  (-> a-seq to-bytes Hex/encodeHexString))

(defn hex->bytes [hex-str]
  (Hex/decodeHex hex-str))

(defn sha1-as-bytes [a-seq]
  (-> a-seq to-bytes DigestUtils/sha1))

(defn padding [a-seq n value]
  (let [c (count a-seq)
        how-much-to-pad (- n c)]
    (if (pos? how-much-to-pad)
      (repeat how-much-to-pad value)
      '())))

(defn pad-right
  "right pad a-seq with n number of value"
  [a-seq n value]
  (concat a-seq (padding a-seq n value)))

(defn pad-left [a-seq n value]
  (concat (padding a-seq n value) a-seq))

(defn pointer
  "returns a 'pointer' to the seq of bytes using a struct to define the structure of the seq of bytes.
  For example, suppose the following person-struct:
  (let [person-struct [:id :int32
                       :fname [:char 20]
                       :lname [:byte 20]]
       a-seq-of-bytes ...
       person-pt (pointer person-struct a-seq-of-bytes)]
  (person-pt :id) ;;bytes corresponding to the id
  (person-pt :fname) ;;bytes corresponding to fname
  (person-pt :lname) ;;bytes corresponding to lname
  )

  :id is the field that is of type :int32 which occupies 32 bit or 4 bytes. To access this field,
  (person-pt :id) . This gives the raw bytes which you can transform into integer with the function bytes->int.

  A pointer keeps track of an internal offset used as a base to calculate the offset of for the fields. You can do
  'pointer' arithmetic on this offset. For example,

  (person-pt + (sizeof person-struct))
  This advances the offset to the next chunk of data containing a person. Therefore, calling
  (person-pt :id) again will give the id of the next person.

  You can also use field names when doing arithmetic on the 'pointer'
  (person-pt + :id) which is the same as doing (person-pt + (sizeof :int32)) because :id is of type :int32
  "
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
            (let [chunk (take-between (+ @offset field-offset) (+ @offset field-offset size) a-seq)]
              (if (= (count chunk) 1)
                (first chunk)
                chunk))))
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
  (let [buffer (suck "./person.dat")
        person-struct [:id :int32
                       :fname [:char 20] ;;char and byte are same size so it doesn't matter which you use
                       :lname [:byte 20]]
        person-size (sizeof person-struct)
        person-pt (pointer person-struct buffer)
        person-count 3]
    (doseq [i (range person-count)
            :let [person {:id (bytes->int (person-pt :id))
                          :fname (->> (person-pt :fname)
                                      (remove zero?)
                                      bytes->char
                                      (clojure.string/join ""))
                          :lname (->> (person-pt :lname)
                                      (remove zero?)
                                      bytes->char
                                      (clojure.string/join ""))}]]
      (prn person)
      (person-pt + person-size)))

  (pad-right [1 2 3] 6 0)
  (pad-left [1 2 3] 5 0)

  )
