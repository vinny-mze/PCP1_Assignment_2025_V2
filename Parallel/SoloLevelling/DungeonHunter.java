
import java.util.Random;
import java.util.concurrent.*;

/**
 * Parallel version of DungeonHunter using Fork-Join Framework.
 * Usage:
 *   java DungeonHunter <gridSize> <numSearches> <randomSeed>
 */
class DungeonHunter {
    static final boolean DEBUG = false;
    static long startTime = 0;
    static long endTime = 0;
    
    private static void tick() { startTime = System.currentTimeMillis(); }
    private static void tock() { endTime = System.currentTimeMillis(); }

    // Inner class for parallel task
    static class HuntTask extends RecursiveTask<Integer> {
        private static final int SEQUENTIAL_THRESHOLD = 1000; // Tune for performance
        private final DungeonMap dungeon;
        private final int startSearch, endSearch;
        private final Random rand;
        private final Hunt[] searches;
        private final int[] peakValues;
        private int finder = -1;

        HuntTask(DungeonMap dungeon, int start, int end, Random rand, Hunt[] searches, int[] peakValues) {
            this.dungeon = dungeon;
            this.startSearch = start;
            this.endSearch = end;
            this.rand = rand;
            this.searches = searches;
            this.peakValues = peakValues;
        }

        @Override
        protected Integer compute() {
            if (endSearch - startSearch <= SEQUENTIAL_THRESHOLD) {
                int localMax = Integer.MIN_VALUE;
                for (int i = startSearch; i < endSearch; i++) {
                    searches[i] = new Hunt(i + 1,
                            rand.nextInt(dungeon.getRows()),
                            rand.nextInt(dungeon.getColumns()),
                            dungeon);
                    peakValues[i] = searches[i].findManaPeak(); // store computed peak
                    if (peakValues[i] > localMax) {
                        localMax = peakValues[i];
                        finder = i;
                    }
                    if (DEBUG) System.out.println("Shadow " + searches[i].getID() + " finished at " + peakValues[i]);
                }
                return localMax;
            } else {
                int mid = (startSearch + endSearch) >>> 1;
                HuntTask left = new HuntTask(dungeon, startSearch, mid, rand, searches, peakValues);
                HuntTask right = new HuntTask(dungeon, mid, endSearch, rand, searches, peakValues);
                left.fork();
                int rightMax = right.compute();
                int leftMax = left.join();
                return Math.max(leftMax, rightMax);
            }
        }
    }

    public static void main(String[] args) {
        double xmin, xmax, ymin, ymax;
        DungeonMap dungeon;
        int numSearches = 10, gateSize = 10;
        Hunt[] searches;
        int[] peakValues;
        Random rand = new Random();
        int randomSeed = 0;

        if (args.length != 3) {
            System.out.println("Usage: java DungeonHunter <gridSize> <numSearches> <randomSeed>");
            System.exit(0);
        }

        try {
            gateSize = Integer.parseInt(args[0]);
            if (gateSize <= 0) throw new IllegalArgumentException("Grid size must be > 0");
            
            numSearches = (int)(Double.parseDouble(args[1]) * (gateSize * 2) * (gateSize * 2) * DungeonMap.RESOLUTION);
            randomSeed = Integer.parseInt(args[2]);
            if (randomSeed < 0) throw new IllegalArgumentException("Random seed must be >= 0");
        } catch (NumberFormatException e) {
            System.err.println("Error: All arguments must be numeric");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        xmin = -gateSize;
        xmax = gateSize;
        ymin = -gateSize;
        ymax = gateSize;
        dungeon = new DungeonMap(xmin, xmax, ymin, ymax, randomSeed);
        searches = new Hunt[numSearches];
        peakValues = new int[numSearches];

        // Parallel execution
        tick();
        ForkJoinPool pool = new ForkJoinPool();
        int max = pool.invoke(new HuntTask(dungeon, 0, numSearches, rand, searches, peakValues));
        tock();

        // Find which search found the global maximum
        int globalFinder = -1;
        for (int i = 0; i < searches.length; i++) {
            if (searches[i] != null && peakValues[i] == max) {
                globalFinder = i;
                break;
            }
        }

        // Output results (same format as serial version)
        System.out.printf("\t dungeon size: %d,\n", gateSize);
        System.out.printf("\t rows: %d, columns: %d\n", dungeon.getRows(), dungeon.getColumns());
        System.out.printf("\t x: [%f, %f], y: [%f, %f]\n", xmin, xmax, ymin, ymax);
        System.out.printf("\t Number searches: %d\n", numSearches);
        System.out.printf("\n\t time: %d ms\n", endTime - startTime);
        
        int evaluated = dungeon.getGridPointsEvaluated();
        System.out.printf("\tnumber dungeon grid points evaluated: %d (%2.0f%%)\n",
                evaluated, (evaluated * 100.0) / (dungeon.getRows() * dungeon.getColumns()));

        System.out.printf("Dungeon Master (mana %d) found at: ", max);
        System.out.printf("x=%.1f y=%.1f\n\n",
                dungeon.getXcoord(searches[globalFinder].getPosRow()),
                dungeon.getYcoord(searches[globalFinder].getPosCol()));

        // Generate visualization files
        dungeon.visualisePowerMap("visualiseSearch.png", false);
        dungeon.visualisePowerMap("visualiseSearchPath.png", true);
    }
}
