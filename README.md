# Nima Market Footfall Tally (demo submission)

A sample submission for the **demo lab**, used to show how to submit on Evalu8.

## Running it

```bash
javac -d target/classes src/main/java/FootfallTally.java
java -cp target/classes FootfallTally stress 200
```

## What I changed

See `DIAGNOSIS.md` (committed before any code change) for the evidence and mechanisms.

- **Lost updates:** the shared map is a `ConcurrentHashMap`, and each addition uses
  `merge`, which is atomic per gate. The job still runs on sixteen threads; nothing is
  serialised.
- **Leaked readers:** each reader is opened in try-with-resources, so it is closed on
  every path.
- **Invisible failures:** files are submitted with `submit()`, and each `Future` is
  checked, so a failed file stops the tally with its name instead of producing a quiet
  partial total. A tally that does not finish within a minute is reported, not read.

## Before and after

Before (starter, 100 runs):

```
--- 100 runs: 11 wrong, 89 correct (expected 494780)
--- log readers opened: 8000, closed: 0
```

After (200 runs):

```
--- 200 runs: 0 wrong, 200 correct (expected 494780)
--- log readers opened: 16000, closed: 16000
```
