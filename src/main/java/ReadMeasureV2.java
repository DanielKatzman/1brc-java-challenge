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

        long start = System.nanoTime();
        double mean = readOptimized(path);
        long end = System.nanoTime();

        System.out.println("Mean: " + mean);
        System.out.println("Total Time: " + (end - start) / 1_000_000 + "ms");
    }

    private static double readOptimized(Path path) throws IOException {
        try (var fileChannel = FileChannel.open(path);
             var arena = Arena.ofShared()) { // Manage memory lifetime

            long fileSize = fileChannel.size();
            // Map the entire 13GB file into memory
            MemorySegment segment = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);

            long offset = 0;
            double totalSum = 0;
            long count = 0;

            while (offset < fileSize) {
                // 1. Find the semicolon ';' (byte 59)
                long semicolonPos = findByte(segment, offset, (byte) ';');

                // 2. Parse the number directly from bytes (No String creation!)
                // We know the number is after the semicolon and ends with a newline
                long newlinePos = findByte(segment, semicolonPos + 1, (byte) '\n');

                double value = parseRawDouble(segment, semicolonPos + 1, newlinePos);

                totalSum += value;
                count++;
                offset = newlinePos + 1;

                // Progress update every 100M lines
                if (count % 100_000_000 == 0) {
                    System.out.println("Processed " + count + " lines...");
                }
            }
            return totalSum / count;
        }
    }

    // Helper to find a byte without converting to String
    private static long findByte(MemorySegment segment, long start, byte target) {
        for (long i = start; ; i++) {
            if (segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, i) == target) {
                return i;
            }
        }
    }

    // High-speed parsing: '12.3' -> (1*10 + 2 + 0.3)
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
                // Simplified: assuming one decimal place like '12.3'
                double fraction = (segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, i + 1) - '0') / 10.0;
                val += fraction;
                break;
            }
        }
        return negative ? -val : val;
    }
}
