import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReadMeasureV2 {
    public static void main(String[] args) throws IOException {
        Path path = Paths.get("measurements.txt");
        System.out.println("Reading with Memory Segments (V2)...");

        // Track total execution for the whole program
        long programStart = System.currentTimeMillis();

        double mean = readOptimized(path);

        long programEnd = System.currentTimeMillis();
        long totalDuration = programEnd - programStart;

        System.out.println("Mean: " + mean);
        // Using 1 Billion rows for the stats calculation
        printStatsForR("v2.0 (FFM API)", totalDuration, 1_000_000_000L);
    }

    private static double readOptimized(Path path) throws IOException {
        try (var fileChannel = FileChannel.open(path);
             var arena = Arena.ofShared()) {

            long fileSize = fileChannel.size();
            MemorySegment segment = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);

            long offset = 0;
            double totalSum = 0;
            long count = 0;

            while (offset < fileSize) {
                long semicolonPos = findByte(segment, offset, (byte) ';');
                long newlinePos = findByte(segment, semicolonPos + 1, (byte) '\n');

                double value = parseRawDouble(segment, semicolonPos + 1, newlinePos);

                totalSum += value;
                count++;
                offset = newlinePos + 1;

                if (count % 100_000_000 == 0) {
                    System.out.println("Processed " + count + " lines...");
                }
            }
            return totalSum / count;
        }
    }

    private static long findByte(MemorySegment segment, long start, byte target) {
        for (long i = start; ; i++) {
            if (segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, i) == target) {
                return i;
            }
        }
    }

    private static double parseRawDouble(MemorySegment segment, long start, long end) {
        boolean negative = false;
        if (segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, start) == '-') {
            negative = true;
            start++;
        }

        double val = 0;
        for (long i = start; i < end; i++) {
            byte b = segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, i);
            if (b >= '0' && b <= '9') {
                val = val * 10 + (b - '0');
            } else if (b == '.') {
                // Assuming one decimal place as per 1BRC spec
                double fraction = (segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, i + 1) - '0') / 10.0;
                val += fraction;
                break;
            }
        }
        return negative ? -val : val;
    }

    // Identical print function for consistency in RStudio
    private static void printStatsForR(String version, long durationMs, long totalRows) {
        double seconds = durationMs / 1000.0;
        double rowsPerSec = totalRows / seconds;
        double mbPerSec = (13800.0) / seconds; // Based on ~13.8GB file size

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