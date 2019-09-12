# voodoo
Clojure/ClojureScript library for binary byte buffer manipulation reminiscent of C

## Rationale
Parsing binary bytes of data in Clojure/ClojureScript is a pain in the butt. Parsing binary bytes of data in C/C++ is easier. This library simulates C structs and pointer arithmetic to allow parsing of binary data in a C like way.

[Octet](https://github.com/funcool/octet) is a library for byte buffer manipulation but it is more high level.
Translating C code for byte buffer manipulation into octet requires more thinking. The goal of voodoo is to make
it easier to translate C code into Clojure without much cognitive re-mapping.

## TODO
ClojureScript support is pending

## Example

Here's an exampe of parsing a git index file in C
https://github.com/sonwh98/clgit/blob/master/parse_git_index.c

Here is an example parsing a git index file in Clojure using voodoo
https://github.com/sonwh98/clgit/blob/e64d8efaed1285b172b64f3ef0896e04179b5090/src/cljc/stigmergy/clgit.cljc#L93

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

Parse person.dat 

```Clojure
(let [buffer (suck "./person.dat") ;;like slurp except returns raw bytes
      person-struct [:id :int32 ;; 32 bit int
                     :fname [:char 20] ;;char and byte are same size so it doesn't matter which you use
                     :lname [:byte 20]]
      person-size (sizeof person-struct) ;; size of person-struct in bytes
      person-pt (pointer person-struct buffer) ;; a "pointer" to a person-struct 
      person-count 3]
    (doseq [i (range person-count)
            :let [person {:id (seq->int (person-pt :id))
                          :fname (->> (person-pt :fname)
                                      (remove zero?)
                                      seq->char
                                      (clojure.string/join ""))
                          :lname (->> (person-pt :lname)
                                      (remove zero?)
                                      seq->char
                                      (clojure.string/join ""))}]]
      (prn person)
      ;;increment pointer by person-size bytes to point to next person-struct in the buffer
      (person-pt + person-size)))
```
