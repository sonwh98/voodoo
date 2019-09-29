(ns stigmergy.voodoo
  (:require [stigmergy.tily :as util])
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

(defn seq->int [a-seq]
  (.. (BigInteger. (byte-array a-seq))
      intValue)
  #_(let []
      (+  (bit-shift-left (nth bytes 0) 24)
          (bit-shift-left (nth  bytes 1) 16)
          (bit-shift-left (nth  bytes 2) 8)
          (bit-shift-left (nth  bytes 3) 0))))

(defn int->seq [an-int]
  (seq (.. (BigInteger/valueOf an-int) toByteArray)))

(defn seq->oct [a-seq]
  (.. (BigInteger. (byte-array a-seq))
      (toString 8)))

(defn oct->seq [an-octal]
  (seq (.. (BigInteger. an-octal 8)
           toByteArray)))

(defn seq->char
  "convert every element in a-seq into a char"
  [a-seq]
  (map #(if (and (number? %) (neg? %))
          (char (- %))
          (char %))
       a-seq))

(defn remove-zero
  "remove zero (aka null char) from a-seq"
  [a-seq]
  (remove (fn [a]
            (if (number? a)
              (zero? a)
              false))
          a-seq))

(defn seq->str
  "converts first block of non-null characters to string. For example,
   (= (seq->str '(\a \b \c 0 \1 \2 \3))
      \"abc\""
  [a-seq]
  (let [null 0
        i (util/index-of a-seq null)
        a-seq  (if i
                 (take i a-seq)
                 a-seq)]
    (->> a-seq
         seq->char
         (clojure.string/join "")))
  #_(String. (byte-array a-seq)))

(defn str->seq [a-str]
  (map byte a-str))

(defn seq->byte-array [a-seq]
  (cond
    (bytes? a-seq) a-seq
    (string? a-seq) (-> a-seq str->seq byte-array)
    (sequential? a-seq) (byte-array a-seq)
    :else (byte-array [a-seq])))

(defn seq->hex [a-seq]
  (-> a-seq byte-array Hex/encodeHexString))

(defn hex->seq [hex-str]
  (seq (Hex/decodeHex hex-str)))

(defn sha1 [a-seq]
  (-> a-seq byte-array DigestUtils/sha1))

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

(defn pad-left
  "left pad a-seq with n number of value"
  [a-seq n value]
  (concat (padding a-seq n value) a-seq))

(defn pointer
  "returns a 'pointer' to the seq of bytes using a struct to define the structure of the seq of bytes.
  For example, suppose the following struct-person:
  (let [struct-person [:id :int32
                       :fname [:char 20]
                       :lname [:byte 20]]
       a-seq-of-bytes ...
       person-pt (pointer struct-person a-seq-of-bytes)]
  (person-pt :id) ;;bytes corresponding to the id
  (person-pt :fname) ;;bytes corresponding to fname
  (person-pt :lname) ;;bytes corresponding to lname
  )

  :id is the field that is of type :int32 which occupies 32 bit or 4 bytes. To access this field,
  (person-pt :id) . This gives the raw bytes which you can transform into integer with the function seq->int.

  A pointer keeps track of an internal offset used as a base to calculate the offset for the fields. You can do
  'pointer' arithmetic on this offset. For example,

  (person-pt + (sizeof struct-person))
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
              (util/take-between (+ @offset field-offset) size a-seq))
            (let [chunk (util/take-between (+ @offset field-offset) (+ @offset field-offset size) a-seq)]
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

(comment
  (let [buffer (util/suck "./person.dat") ;;suck in raw bytes
        struct-person [:id :int32
                       :fname [:char 20] ;;char and byte are same size so it doesn't matter which you use
                       :lname [:byte 20]]
        person-size (sizeof struct-person)
        person-pt (pointer struct-person buffer)
        person-count 3]
    (doseq [i (range person-count)
            :let [id (person-pt :id) ;;"dereferncing" id field, in C it would be like personPt->id
                  fname (person-pt :fname)
                  lname (person-pt :lname)
                  person {:id (seq->int id)
                          :fname (->> fname
                                      seq->str)
                          :lname (->> lname
                                      seq->str)}]]
      (prn person)
      (person-pt + person-size)))
  
  (pad-right [1 2 3] 6 0)
  (pad-left [1 2 3] 5 0)
  (let [two (take 2 "abcdefg")]
    (seq->str two)
    )

  (def a '(\a \b \1 \c 0 \1 \2 \3))


  (number? \a)
  (seq->str a)
  (seq->char '(100 0 -1))
  )
