import java.io.*;
import java.util.*;
import java.util.regex.*;

public class SweepHarness {
    // Run external Java program and capture its stdout
    private static String runProgram(String className, String[] args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add(className);
        command.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        process.waitFor();
        return output.toString();
    }

    // Parse stdout into a CSV row
    private static String parseOutput(String raw, int gridSize, double searchFraction, int numSearches, int implFlag) {
        // implFlag: 0 = serial, 1 = parallel
        int timeMs = -1;
        int maxMana = -1;
        double maxX = Double.NaN, maxY = Double.NaN;
        int evaluatedPoints = -1;
        double evaluatedPercent = Double.NaN;

        // Regex patterns to extract needed numbers
        Matcher m;

        // Extract time (ms)
        m = Pattern.compile("time:\\s*(\\d+)\\s*ms").matcher(raw);
        if (m.find()) {
            timeMs = Integer.parseInt(m.group(1));
        }

        // Extract max mana and coordinates (maxX, maxY)
        m = Pattern.compile("mana\\s*(\\d+)\\)\\s*found at:\\s*x=([\\d\\.,-]+)\\s*y=([\\d\\.,-]+)").matcher(raw);
        if (m.find()) {
            maxMana = Integer.parseInt(m.group(1));
            maxX = Double.parseDouble(m.group(2).replace(",", "."));
            maxY = Double.parseDouble(m.group(3).replace(",", "."));
        }

        // Extract evaluated points and percentage
        m = Pattern.compile("evaluated:\\s*(\\d+)\\s*.*\\((\\d+)%\\)").matcher(raw);
        if (m.find()) {
            evaluatedPoints = Integer.parseInt(m.group(1));
            evaluatedPercent = Double.parseDouble(m.group(2));
        }

        // Correct the format specifiers for integers and floats
        return String.format(Locale.US, "%d,%.2f,%d,%d,%d,%d,%.1f,%.1f,%d,%.2f",
                gridSize, searchFraction, numSearches, implFlag, timeMs, maxMana, maxX, maxY, evaluatedPoints, evaluatedPercent);
    }

    public static void main(String[] args) throws Exception {
        int[] gridSizes = {5,10, 20, 50,100}; // Grid sizes for testing
        double[] searchFractions = {0.05, 0.10, 0.20,0.5}; // Search fractions for testing
        int randomSeed = 123; // Random seed value
        System.out.println("gridSize,searchFraction,numSearches,impl,timeMs,maxMana,maxX,maxY,evaluatedPoints,evaluatedPercent");

        // Loop through all grid sizes and search fractions
        for (int gridSize : gridSizes) {
            for (double searchFraction : searchFractions) {
                int numSearches = (int) (gridSize * gridSize * searchFraction); // Calculate number of searches
                String[] runArgs = { Integer.toString(gridSize), Double.toString(searchFraction), Integer.toString(randomSeed) };

                // Serial run
                String serialOutput = runProgram("DungeonHunter", runArgs);
                String serialCsv = parseOutput(serialOutput, gridSize, searchFraction, numSearches, 0);
                System.out.println(serialCsv);

                // Parallel run
                String parallelOutput = runProgram("DungeonHunterParallel", runArgs);
                String parallelCsv = parseOutput(parallelOutput, gridSize, searchFraction, numSearches, 1);
                System.out.println(parallelCsv);
            }
        }
    }
}
