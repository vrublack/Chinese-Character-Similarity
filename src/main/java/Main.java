import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static boolean DEBUG = true;

    private static Map<String, String[]> readDecomposition(String fname) {
        Map<String, String[]> result = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(fname)));
            String line = reader.readLine();    // skip header
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (DEBUG && count % 1000 != 0)
                    continue;
                String[] comps = line.split(";");
                result.put(comps[0], comps[1].split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    /***
     *
     * @param args 0 - method, 1 - decomp filename, 2 - matrix output filename, 3 - sorted similarity list output filename, 4 - number of similar characters to include
     */
    public static void main(String[] args) {
        if (DEBUG) {
            System.out.println("Warning: DEBUG is turned on");
        }

        String decompositionFname = args[0];
        String outputFname = args[1];
        int cutoff = Integer.parseInt(args[2]);

        Map<String, String[]> decomp = readDecomposition(decompositionFname);
        List<String> allChars = new ArrayList<>(decomp.keySet());
        try {
            BufferedWriter br = new BufferedWriter(new FileWriter(new File(outputFname)));
            final long totalPairs = allChars.size() * ((long) allChars.size());

            long count = 0;
            float[] similarities = new float[allChars.size()];
            for (int i = 0; i < allChars.size(); i++) {
                // could start at j = i and then cache but cache would be very large
                for (int j = 0; j < allChars.size(); j++) {
                    // don't want the character itself
                    if (i != j) {
                        similarities[j] = calculateCharSimilarity(allChars.get(i), allChars.get(j), decomp);
                    } else {
                        similarities[j] = 0;
                    }
                    count++;
                    if (count % 1000000 == 0)
                        System.out.println(NumberFormat.getIntegerInstance().format(count) + "/" + NumberFormat.getIntegerInstance().format(totalPairs));
                }

                br.write(allChars.get(i) + ";");
                int[] similarSorted = ArrayUtils.argsort(similarities, false);
                // don't write chars to file that have similarity 0
                for (int j = 0; j < Math.min(similarSorted.length, cutoff) && similarities[similarSorted[j]] > 0; j++) {
                    if (j > 0)
                        br.write(",");
                    br.write(allChars.get(similarSorted[j]));
                }
                br.write("\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }


        System.out.println("Done");
    }


    /***
     *
     * @return Value between 0 (very dissimilar) and 1 (identical)
     */
    private static float calculateCharSimilarity(String c1, String c2, Map<String, String[]> decomp) {
        if (c1.equals(c2))
            return 1;

        float totalScore = 0;
        String[] dc1 = decomp.get(c1);
        String[] dc2 = decomp.get(c2);

        // component overlap
        for (String comp1 : dc1) {
            for (String comp2 : dc2) {
                if (comp1.equals(comp2)) {
                    totalScore += 1;
                }
            }
        }

        totalScore /= dc1.length * dc2.length;

        return totalScore;
    }
}
