# Nima Market Footfall Tally (demo lab)

Starter repository for the **demo lab** used to show how to submit on Evalu8.
Read the brief alongside it.

## Running it

You need JDK 21. Sample data is in `data/`.

```bash
javac -d target/classes src/main/java/FootfallTally.java
java -cp target/classes FootfallTally run
java -cp target/classes FootfallTally stress        # 100 runs against the known total
```

Run `stress` before you change anything: it is your evidence.
