import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReadMeasureV1 {
    public static void main(String[] args) {
        System.out.println("Reading measure v1...");
        long programStart = System.currentTimeMillis();

        Double mean = read(1_000_000_000, 100_000_000);

        long programEnd = System.currentTimeMillis();
        long totalDuration = programEnd - programStart;

        System.out.println("Mean: " + mean);
        printStatsForR("v1.0 (Baseline)", totalDuration, 1_000_000_000);
    }

    private static Double read(int lines, int update) {
        try (FileReader fr = new FileReader("measurements.txt")) {
            BufferedReader br = new BufferedReader(fr);
            String line;
            int count = 0;
            long start = System.nanoTime();
            double totalSum = 0.;

            for (int i = 0; i < lines; i++) {
                line = br.readLine();
                if (line == null) break;

                if (count == update) {
                    System.out.println("Chunk processed. Interval time: " + (System.nanoTime() - start) / 1_000_000 + "ms");
                    start = System.nanoTime();
                    count = 0;
                }

                int delimiterIndex = line.indexOf(";");
                String numberPart = line.substring(delimiterIndex + 1);
                totalSum += Double.parseDouble(numberPart);
                count++;
            }
            return totalSum / lines;

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static void printStatsForR(String version, long durationMs, long totalRows) {
        double seconds = durationMs / 1000.0;
        double rowsPerSec = totalRows / seconds;
        double mbPerSec = (13800.0) / seconds; // Based on ~13.8GB file size

        // Capture memory usage
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        System.out.println("\n===============================");
        System.out.println("   📊 DETAILED STATS FOR R     ");
        System.out.println("===============================");
        System.out.printf("Version:      %s\n", version);
        System.out.printf("Time_ms:      %d\n", durationMs);
        System.out.printf("Time_sec:     %.2f\n", seconds);
        System.out.printf("Rows_per_sec: %.2f\n", rowsPerSec);
        System.out.printf("MB_per_sec:   %.2f\n", mbPerSec);
        System.out.printf("Memory_MB:    %d\n", memoryUsed);
        System.out.println("===============================");
    }
}