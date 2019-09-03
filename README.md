# voodoo
Clojure/ClojureScript library for binary byte buffer manipulation reminiscent of C

## Rationale
Parsing binary bytes of data in Clojure/ClojureScript is a pain in the butt. Parsing binary bytes of data in C/C++ is simply.  This library simulates C structs and pointer arthimatic to allow use parsing of binary data.

[Octet](https://github.com/funcool/octet) is a library for byte buffer manipulation but it is more high level.

## Example

https://github.com/sonwh98/clgit/blob/07121ed5fda130a487ea77d11995ab0ee45ca833/src/cljc/stigmergy/clgit.cljc#L93
