# Optional RWKV-Instant model package

Bihua does not ship RWKV weights. The published weights are marked
research/personal-use only, and their notice asks for the dataset owners'
permission before they are distributed in an app. The importer exists for a
learner who obtained a compatible model themselves and has the right to use it.

This page is deliberately not linked from onboarding or the landing page. The
package is imported from **Settings → Schedule → Research model package**.

## What the package is

A zip file with exactly two entries:

```text
manifest.json
model.onnx
```

`manifest.json`:

```json
{
  "format": "bihua-rwkv-package/2",
  "name": "My RWKV step model",
  "architecture": "rwkv-instant-step",
  "features": 7,
  "stateSize": 32,
  "sha256": "<sha256 of model.onnx>",
  "license": "Research use only; state the exact terms",
  "source": "https://example.com/where-you-got-it",
  "inputs": { "features": "features", "state": "state" },
  "outputs": { "recall": "recall", "stateOut": "state_out" },
  "notes": "Optional"
}
```

Validation rejects: a missing entry, an unsafe path, a model over 250 MB, an
unknown format or architecture, a different feature count, a state size outside
1–4096, a missing licence or HTTPS source, a bad SHA-256, and tensor names or
shapes that ONNX Runtime cannot satisfy.

## Tensor contract

The model is a **recurrent step**, not a whole review history in one graph.

| Tensor | Direction | Shape | Meaning |
|---|---|---|---|
| `features` | input | `[1, 7]` | the versioned feature frame below |
| `state` | input | `[1, stateSize]` | the card's recurrent state |
| `recall` | output | one value | predicted probability of recall |
| `state_out` | output | `[1, stateSize]` | the state after this review |

Feature frame version 1:

| Index | Value |
|---|---|
| 0 | `ln(elapsed days + 1/1440)` |
| 1 | grade / 4 |
| 2 | `ln(reps + 1)` |
| 3 | `ln(lapses + 1)` |
| 4 | same-day reviews / 10 |
| 5 | local hour / 23 |
| 6 | day of week / 6 |

The frame is fixed in this build. A package that needs the full 92-feature
upstream RWKV frame is **not** compatible yet; its manifest will be rejected
with a feature-count error rather than silently receiving the wrong inputs.

## Scheduling

When the model is selected, Bihua:

1. builds the feature frame for the review;
2. probes the model at log-spaced intervals up to the 365-day cap, holding the
   state fixed, and then bisects the bracket to find the largest interval whose
   predicted recall is still at or above the target;
3. runs the model once at the real elapsed time to update the state;
4. stores the state as a BLOB on the card and in the review log.

If the package is removed while selected, Settings falls back to FSRS-7 and the
stored FSRS/SBD states are untouched.

## Privacy and licensing

- The import is local. Nothing is downloaded, uploaded, or sent to a server.
- The app never bundles, fetches, or ships the weights.
- The importer is not an endorsement of any model's accuracy or licence.
- Do not import a package you do not have the right to use.
