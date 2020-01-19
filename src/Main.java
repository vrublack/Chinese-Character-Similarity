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

    private static void writeSimilarityMatrix(String decompositionFname, String outFname) {
        Map<String, String[]> decomp = readDecomposition(decompositionFname);
        List<String> allChars = new ArrayList<>(decomp.keySet());
        try {
            BufferedWriter br = new BufferedWriter(new FileWriter(new File(outFname)));
            final long totalPairs = allChars.size() * ((long) allChars.size());

            // write index to first line
            for (int i = 0; i < allChars.size(); i++) {
                br.write(allChars.get(i));
                if (i < allChars.size() - 1)
                    br.write(",");
            }
            br.write("\n");

            long count = 0;
            for (int i = 0; i < allChars.size(); i++) {
                // could start at j = i and then cache but cache would be very large
                for (int j = 0; j < allChars.size(); j++) {
                    float sim = calculateCharSimilarity(allChars.get(i), allChars.get(j), decomp);
                    String formatted;
                    if (sim == 0) {
                        // save space
                        formatted = "0";
                    } else {
                        formatted = String.format("%.4f", sim);
                    }
                    br.write(formatted);
                    if (j < allChars.size() - 1)
                        br.write(" ");
                    count++;
                    if (count % 1000000 == 0)
                        System.out.println(NumberFormat.getIntegerInstance().format(count) + "/" + NumberFormat.getIntegerInstance().format(totalPairs));
                }
                br.write("\n");
            }
            br.close();
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
        if (DEBUG) {
            System.out.println("Warning: DEBUG is turned on");
        }

        switch (args[0]) {
            case "matrix":
                writeSimilarityMatrix(args[1], args[2]);
                break;
            case "sort":
                sortSimilarities(args[2], args[3], Integer.parseInt(args[4]));
                break;
            default:
                System.out.println("Error: unknown method");
                System.exit(1);
        }

        System.out.println("Done");
    }

    private static void sortSimilarities(String matrixFname, String outFname, int cutoff) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(matrixFname)));
            String line = reader.readLine();    // character index
            String[] allChars = line.split(",");
            while ((line = reader.readLine()) != null) {
                String[] formattedSims = line.split(" ");
                float[] sims = new float[formattedSims.length];
                for (int i = 0; i < sims.length; i++)
                    sims[i] = Float.parseFloat(formattedSims[i]);
                // TODO argsort
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
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
