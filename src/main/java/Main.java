import java.io.*;
import java.text.NumberFormat;
import java.util.*;

public class Main {
    public static boolean DEBUG = false;

    private static Map<String, String[]> readDecomposition(String fname) {
        Map<String, String[]> result = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(fname)));
            String line = reader.readLine();    // skip header
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (DEBUG && count % 20 != 0)
                    continue;
                String[] comps = line.split(";");
                String[] decompComps = comps[1].split(",");
                Arrays.sort(decompComps);
                result.put(comps[0], decompComps);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    private static void write(final BufferedWriter br, final String s) throws IOException {
        synchronized(br) {
            br.write(s);
        }
    }

    private static void computeSimilarityRanking(List<String> allChars, float[] similarities, Map<String, String[]> decomp, int cutoff, int i, final BufferedWriter br) {
        // could start at j = i and then cache but cache would be very large
        for (int j = 0; j < allChars.size(); j++) {
            // don't want the character itself
            if (i != j) {
                similarities[j] = calculateCharSimilarity(allChars.get(i), allChars.get(j), decomp);
            } else {
                similarities[j] = 0;
            }
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append(allChars.get(i)).append(";");
            int[] similarSorted = ArrayUtils.argsort(similarities, false);
            // don't write chars to file that have similarity 0
            for (int j = 0; j < Math.min(similarSorted.length, cutoff) && similarities[similarSorted[j]] > 0; j++) {
                if (j > 0)
                    sb.append(",");
                sb.append(allChars.get(similarSorted[j]));
            }
            sb.append("\n");
            write(br, sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /***
     *
     * @param args 0 - method, 1 - decomp filename, 2 - matrix output filename, 3 - sorted similarity list output filename, 4 - number of similar characters to include
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        if (DEBUG) {
            System.out.println("Warning: DEBUG is turned on");
        }

        final String decompositionFname = args[0];
        final String outputFname = args[1];
        final int cutoff = Integer.parseInt(args[2]);
        final int nThreads = Integer.parseInt(args[3]);

        final Map<String, String[]> decomp = readDecomposition(decompositionFname);
        final List<String> allChars = new ArrayList<>(decomp.keySet());
        try {
            final BufferedWriter br = new BufferedWriter(new FileWriter(new File(outputFname)));
            Thread[] threads = new Thread[nThreads];
            final int itemsPerThread = allChars.size() / nThreads;
            for (int iThread = 0; iThread < threads.length; iThread++) {
                final int startIndex = iThread * itemsPerThread;
                final int endIndex = iThread == threads.length - 1 ? allChars.size() : startIndex + itemsPerThread;
                threads[iThread] = new Thread() {
                    @Override
                    public void run() {
                        final float[] similarities = new float[allChars.size()];
                        for (int i = startIndex; i < endIndex; i++) {
                            computeSimilarityRanking(allChars, similarities, decomp, cutoff, i, br);
                            if ((i - startIndex) % 100 == 0)
                                System.out.println(NumberFormat.getIntegerInstance().format(i - startIndex) + "/" + NumberFormat.getIntegerInstance().format(endIndex - startIndex));
                        }

                    }
                };
            }

            for (Thread thread : threads)
                thread.start();

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Done after " + (System.currentTimeMillis() - start) + " ms");
    }


    /***
     *
     * @return Value between 0 (very dissimilar) and 1 (identical)
     */
    private static float calculateCharSimilarity(String c1, String c2, Map<String, String[]> decomp) {
        if (c1.equals(c2))
            return 1;

        // component overlap
        float totalScore = 0;
        String[] dc1 = decomp.get(c1);
        String[] dc2 = decomp.get(c2);

        int i = 0;
        int j = 0;

        while (i < dc1.length && j < dc2.length) {
            if (dc1[i].equals(dc2[j])) {
                String sameComp = dc1[i];
                while (i < dc1.length && dc1[i].equals(sameComp)) {
                    i++;
                    totalScore++;
                }
                while (j < dc2.length && dc2[j].equals(sameComp)) {
                    j++;
                    totalScore++;
                }
            } else if (dc1[i].compareTo(dc2[j]) < 0) {  // advance pointer to smaller component
                i++;
            } else {
                j++;
            }
        }

        totalScore /= dc1.length + dc2.length;

        return totalScore;
    }
}
