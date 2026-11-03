import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Nima Market footfall tally.
// Reads the day's gate logs (one file per gate counter upload) and totals how many
// visitors came through each gate. Run "stress" to see the trouble.
public class FootfallTally {

    static final int FILES = 80;
    static final int RECORDS_PER_FILE = 400;
    static final Path DATA = Paths.get("data");
    static final AtomicInteger READERS_OPENED = new AtomicInteger();
    static final AtomicInteger READERS_CLOSED = new AtomicInteger();

    final Map<String, Long> visitorsPerGate = new ConcurrentHashMap<>();

    void processFile(Path f) {
        try {
            BufferedReader r = openLog(f);
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split(",");
                visitorsPerGate.merge(p[0], Long.parseLong(p[1]), Long::sum);
            }
        } catch (IOException e) {
            System.out.println("could not read " + f);
        }
    }

    long tally() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        for (Path f : listData()) pool.execute(() -> processFile(f));
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        long total = 0;
        for (long v : visitorsPerGate.values()) total += v;
        return total;
    }

    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "run";
        switch (cmd) {
            case "generate" -> generate();
            case "stress" -> stress(args.length > 1 ? Integer.parseInt(args[1]) : 100);
            default -> {
                long total = new FootfallTally().tally();
                System.out.println("Total visitors: " + total);
            }
        }
    }

    static BufferedReader openLog(Path f) throws IOException {
        READERS_OPENED.incrementAndGet();
        return new BufferedReader(new FileReader(f.toFile())) {
            @Override public void close() throws IOException {
                READERS_CLOSED.incrementAndGet();
                super.close();
            }
        };
    }

    static List<Path> listData() throws IOException {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(DATA, "*.log")) {
            for (Path p : ds) out.add(p);
        }
        return out;
    }

    static void generate() throws IOException {
        Files.createDirectories(DATA);
        Random rnd = new Random(7);
        long expected = 0;
        for (int i = 0; i < FILES; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < RECORDS_PER_FILE; j++) {
                String gate = "GATE_" + (char) ('A' + rnd.nextInt(4));
                int visitors = 1 + rnd.nextInt(30);
                sb.append(gate).append(',').append(visitors).append('\n');
                expected += visitors;
            }
            Files.writeString(DATA.resolve(String.format("upload_%02d.log", i + 1)), sb.toString());
        }
        Files.writeString(DATA.resolve("EXPECTED.txt"), expected + "\n");
        System.out.println("Generated " + FILES + " files. Expected total: " + expected);
    }

    static void stress(int runs) throws Exception {
        long expected = Long.parseLong(Files.readString(DATA.resolve("EXPECTED.txt")).trim());
        READERS_OPENED.set(0);
        READERS_CLOSED.set(0);
        int wrong = 0;
        for (int i = 0; i < runs; i++) {
            if (new FootfallTally().tally() != expected) wrong++;
        }
        System.out.println("--- " + runs + " runs: " + wrong + " wrong, " + (runs - wrong) + " correct (expected " + expected + ")");
        System.out.println("--- log readers opened: " + READERS_OPENED.get() + ", closed: " + READERS_CLOSED.get());
    }
}
