#!/usr/bin/env sh
# Lists the public Scala symbols a branch adds, one per line as "file: kind name".
#
# Usage: scripts/new-symbols.sh [base-ref] [head-ref]
#   base-ref defaults to 0.2.0-alpha; with no head-ref the working tree is compared.
#
# Private and protected members are skipped. A val or var indented four spaces
# or more is treated as a local and skipped; defs, classes, objects and traits
# are listed at any depth.
set -eu
base="${1:-0.2.0-alpha}"
head="${2:-}"
git diff -U0 "$base" ${head:+"$head"} -- '*.scala' | awk '
  /^\+\+\+ b\// { file = substr($0, 7); next }
  /^\+/ && !/^\+\+\+/ {
    line = substr($0, 2)
    if (line ~ /^[[:space:]]*(\/\/|\*|\/\*)/) next
    if (line ~ /(^|[^A-Za-z])private([[:space:]]|\[)/) next
    if (line ~ /(^|[^A-Za-z])protected([[:space:]]|\[)/) next
    if (line ~ /^[[:space:]]{4,}(final |lazy |override )*(val|var)[[:space:]]/) next
    if (match(line, /(^|[[:space:]])(def|val|var|class|object|trait|enum|type|given)[[:space:]]+[A-Za-z_][A-Za-z0-9_]*/)) {
      sym = substr(line, RSTART, RLENGTH)
      sub(/^[[:space:]]*/, "", sym)
      print file ": " sym
    }
  }'
