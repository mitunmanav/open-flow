# Run the model benchmark and publish the results

Type: task
Status: open
Blocked by: none

## Question

Ticket 06 picked the V1 default model (Silero VAD + streaming Zipformer-en-20M int8, ~45 MB) from a **method**, not from measurements. The method lives in `docs/providers/model-selection.md`; the numbers do not exist anywhere. That document already says where results go: "Results, when run, go in `docs/providers/model-benchmark-results.md` and revise the 'Recommended V1 defaults' section above."

The V1 destination is a three-device acceptance gate across ten text-entry scenarios, and this model choice sits under all of it. A model chosen on plausibility rather than a measured RTF on the weakest supported device is an assumption the acceptance gate will test by failing.

**This needs hardware the agent cannot provide**, so it is a human task. What is needed:

- The three device classes already named in the destination (Pixel-like, Samsung-class, Xiaomi-class), since "weakest tier" in the pass thresholds means the slowest of them.
- The matrix in `docs/providers/model-selection.md`: both model families, int8 variants, fixed 16 kHz mono eval set, 5 warmup decodes discarded, median of ≥20 iterations, `num_threads=4`, thermal state noted.
- The thresholds already proposed in that document: RTF ≤ 0.3 on the weakest tier, first partial ≤ 500 ms, Final within ~1 s of endpoint/VAD close, WER regression ≤ +2 absolute within family, peak RSS ≤ 1 GB on 4 GB devices.

Write the results to `docs/providers/model-benchmark-results.md`, then **revise the "Recommended V1 defaults" section of `model-selection.md`** to agree with what was measured. If the 20M int8 model fails RTF on the weakest tier, that is a finding, and the ADR or map entry recording the default needs amending rather than quietly editing a table.

Note the pattern here: the 20M int8 default is currently an assumption wearing a decision's clothes. That is the same defect as ticket 21's false resolution, and this file is the only place it is written down.

Also add the new document to `docs/README.md` in the same commit — `docs-check` fails if a document under `docs/` is not reachable from the index.