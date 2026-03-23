import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReadMeasureV1 {
    public static void main(String[] args) {
        System.out.println("Reading measure v1...");
        Double mean = read(1_000_000_000,100_000_000);
        System.out.println("Mean: " + mean);
    }

    private static Double read(int lines,int update){
        try (FileReader fr = new FileReader("measurements.txt")){
            BufferedReader br = new BufferedReader(fr);
            String line;
            int count = 0;
            long start = System.nanoTime();
            double totalSum = 0.;

            for (int i = 0; i < lines; i++) {
                if(count == update){
                    System.out.println("Time taken: " + (System.nanoTime() - start) / 1_000_000 + "ms");
                    start = System.nanoTime();
                    count = 0;
                }

                line = br.readLine();
                int delimiterIndex = line.indexOf(";");
                String numberPart = line.substring(delimiterIndex + 1);
                totalSum += Double.parseDouble(numberPart);
                count++;
            }
            return totalSum / lines;

        }catch (IOException e){
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
