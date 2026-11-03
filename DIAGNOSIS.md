# Diagnosis: Nima Market Footfall

Written after running `stress`, before changing any code.

## Evidence (starter, 100 runs)

```
--- 100 runs: 11 wrong, 89 correct (expected 494780)
--- log readers opened: 8000, closed: 0
```

## What goes wrong, and why

1. **Lost updates on the shared map** (`processFile`). Sixteen threads run
   `visitorsPerGate.put(gate, getOrDefault(gate) + n)` on one HashMap at once. Two threads
   can read the same count before either writes, and one addition is lost, so the total
   comes out low on some runs and right on others. That is the "never the same wrong
   answer twice" the gate staff see.
2. **Log readers are never closed** (`processFile` opens a reader and never closes it).
   The stress mode shows readers opened but none closed. On a long-running server every
   upload holds a file handle, which is the "runs out of something" after a long week.
3. **Failures are invisible** (`tally`). `execute()` gives no way to know a file failed,
   and the result of `awaitTermination` is ignored, so a partial total would be reported
   as if complete.

## What I will keep the same

The total visitors, and the output line.
