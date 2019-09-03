# voodoo
Clojure/ClojureScript library for binary byte buffer manipulation reminiscent of C

## Rationale
Parsing binary bytes of data in Clojure/ClojureScript is a pain in the butt. Parsing binary bytes of data in C/C++ is easier. This library simulates C structs and pointer arithmetic to allow use parsing of binary data in a C like way.

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

