import java.io.*;
import java.util.*;

public class ParallelBenchmark {
    public static void main(String[] args) throws Exception {
        // Experiment parameters
        int[] gridSizes = {10, 20, 40,60,100};       // gateSize values
        double[] searchFractions = {0.05, 0.1, 0.2,0.4,0.6,0.8,1};
        int randomSeed = 42;

        // CSV header (same as serial)
        System.out.println("gridSize,searchFraction,numSearches,timeMs,maxMana,maxX,maxY,evaluatedPoints,evaluatedPercent");

        for (int gridSize : gridSizes) {
            for (double searchFraction : searchFractions) {
                // Prepare arguments for the parallel program
                String[] progArgs = {
                        String.valueOf(gridSize),
                        String.valueOf(searchFraction),
                        String.valueOf(randomSeed)
                };

                // Capture console output of DungeonHunter.main()
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                PrintStream oldOut = System.out;
                System.setOut(ps);

                try {
                    DungeonHunter.main(progArgs);  // run parallel program
                } finally {
                    System.out.flush();
                    System.setOut(oldOut);
                }

                // Parse output
                String out = baos.toString();
                long timeMs = parseLongAfter(out, "time: ", " ms");
                int maxMana = parseIntAfter(out, "mana ");
                double maxX = parseDoubleAfter(out, "x=");
                double maxY = parseDoubleAfter(out, "y=");
                int evaluatedPoints = parseIntAfter(out, "number dungeon grid points evaluated: ");
                double evalPercent = parseDoubleAfter(out, "(");

                // Number of searches (calculated same way as parallel code)
                int numSearches = (int) (searchFraction * (gridSize * 2) * (gridSize * 2) * DungeonMap.RESOLUTION);

                // Print CSV row
                System.out.printf(Locale.US, "%d,%.2f,%d,%d,%d,%.1f,%.1f,%d,%.2f%n",
                        gridSize, searchFraction, numSearches, timeMs,
                        maxMana, maxX, maxY, evaluatedPoints, evalPercent);
            }
        }
    }

    // --- Helper parsing functions (same as serial benchmark) ---
    private static long parseLongAfter(String text, String prefix, String suffix) {
        int start = text.indexOf(prefix);
        if (start == -1) return -1;
        start += prefix.length();
        int end = text.indexOf(suffix, start);
        return Long.parseLong(text.substring(start, end).trim());
    }

    private static int parseIntAfter(String text, String prefix) {
        int start = text.indexOf(prefix);
        if (start == -1) return -1;
        start += prefix.length();
        StringBuilder num = new StringBuilder();
        for (char c : text.substring(start).toCharArray()) {
            if (Character.isDigit(c) || c == '-') num.append(c);
            else break;
        }
        return Integer.parseInt(num.toString());
    }

    private static double parseDoubleAfter(String text, String prefix) {
        int start = text.indexOf(prefix);
        if (start == -1) return Double.NaN;
        start += prefix.length();
        StringBuilder num = new StringBuilder();
        for (char c : text.substring(start).toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == '-') num.append(c);
            else break;
        }
        return Double.parseDouble(num.toString());
    }
}
