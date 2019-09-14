# voodoo
Clojure/ClojureScript library for binary data parsing reminiscent of C

## Rationale
Parsing binary bytes of data in Clojure/ClojureScript is a pain in the butt. This library simulates C structs and pointer arithmetic to allow parsing of binary data in a C like way.

[Octet](https://github.com/funcool/octet) is also a library for binary data manipulation 
but it is more high level. Translating C code for into octet requires more thinking. Voodoo is design
to make it easier to translate C code into Clojure without much cognitive re-mapping.

## TODO
ClojureScript support is pending

## seq everywhere
Voodoo treats byte buffers as seq. There are functions with names starting with seq-> to convert seq of bytes into 
primitive types and vice versa. For example, seq->int and int->seq

## Example

Here's an exampe of parsing a git index file in C
https://github.com/sonwh98/clgit/blob/master/parse_git_index.c

Here is an example parsing a git index file in Clojure using voodoo
https://github.com/sonwh98/clgit/blob/6546c3295f82ab25ab7f4a2a78447ff91b5ea3f1/src/cljc/stigmergy/clgit.cljc#L126

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
