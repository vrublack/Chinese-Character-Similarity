import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static Map<String, String[]> readDecomposition(String fname) {
        Map<String, String[]> result = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(fname)));
            String line = reader.readLine();    // skip header
            while ((line = reader.readLine()) != null) {
                String[] comps = line.split(";");
                result.put(comps[0], comps[1].split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    public static void main(String[] args) {
        Map<String, String[]> decomp = readDecomposition("C:\\Users\\valen\\PycharmProjects\\hanzi-decomposition\\char_decomp.txt");
        List<String> allChars = new ArrayList<>(decomp.keySet());
        try {
            BufferedWriter br = new BufferedWriter(new FileWriter(new File("C:\\Users\\valen\\PycharmProjects\\hanzi-decomposition\\hanzi_similarity_matrix.txt")));
            final long totalPairs = allChars.size() * ((long) allChars.size() + 1) / 2;
            long count = 0;
            for (int i = 0; i < allChars.size(); i++) {
                br.write(allChars.get(i) + " ");
                for (int j = i; j < allChars.size(); j++) {
                    float sim = calculateCharSimilarity(allChars.get(i), allChars.get(j), decomp);
                    br.write(sim + " ");
                    count++;
                    if (count % 10000 == 0)
                        System.out.println(NumberFormat.getIntegerInstance().format(count) + "/" + NumberFormat.getIntegerInstance().format(totalPairs));
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
