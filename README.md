# voodoo
Clojure/ClojureScript library for parsing binary data as easy as it is in C

## Rationale
Parsing binary bytes of data in Clojure/ClojureScript is a pain in the butt. This library simulates C structs and pointer arithmetic to allow parsing of binary data in a C like way.

[Octet](https://github.com/funcool/octet) is a similar library for parsing binary data but it is more high level. Translating C code into octet requires more thinking. 
Voodoo is design to make it easier to translate C code into Clojure without much cognitive re-mapping.

## TODO
ClojureScript support is pending

## Sequential everywhere
Voodoo treats byte buffers as squentials. There are functions with names starting with seq-> to convert sequential of bytes into primitive types and vice versa. For example, seq->int and int->seq

## Example

Here's an exampe of parsing a git index file in C
https://github.com/sonwh98/agit/blob/master/parse_git_index.c

Here is an example parsing a git index file in Clojure using voodoo
https://github.com/sonwh98/agit/blob/3645c0fe81c330515718aeda70f4213dbd58af7b/src/cljc/stigmergy/agit.cljc#L194

Here is an example of writing a C struct to a file and reading it from Clojure using Voodoo
Compile and execute [writeStruct.c](https://github.com/sonwh98/voodoo/blob/master/src/c/writeStruct.c)

```bash
% gcc src/c/writeStruct.c
% ./a.out
```

This writes 3 structs into file person.dat. The definition of the struct is

```C
struct person { 
  int id; 
  char fname[20]; 
  char lname[20]; 
}; 
```

Parse person.dat in Clojure

```Clojure
(require '[stigmergy.tily :as util])

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
      
```
