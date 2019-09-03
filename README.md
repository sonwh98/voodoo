# voodoo
Clojure/ClojureScript library for binary byte buffer manipulation reminiscent of C

## Rationale
Parsing binary bytes of data in Clojure/ClojureScript is a pain in the butt. Parsing binary bytes of data in C/C++ is simply.  This library simulates C structs and pointer arthimatic to allow use parsing of binary data.

[Octet](https://github.com/funcool/octet) is a library for byte buffer manipulation but it is more high level.

## Example

Here is an example parsing a git index file using voodoo
https://github.com/sonwh98/clgit/blob/e64d8efaed1285b172b64f3ef0896e04179b5090/src/cljc/stigmergy/clgit.cljc#L93


