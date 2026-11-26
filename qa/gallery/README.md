# Flow galleries

Generated evaluation sheets. Each run gets a folder with three device folders:

```text
<run-name>/
  phone/<flow>.png
  tablet/<flow>.png
  fold/<flow>.png
  INDEX.md
```

Build with `scripts/flow-gallery.sh <flow> --name <run-name>` or
`make gallery SCENARIO=<flow>`. These are not store assets.
