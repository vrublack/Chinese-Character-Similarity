import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
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

    private static List<String[]> readTestcases(String fname) {
        List<String[]> result = new ArrayList<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(fname)));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (DEBUG && count % 20 != 0)
                    continue;
                result.add(line.split(","));
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

    private static void computeSimilarityRanking(List<String> allChars, float[] similarities, Map<String, FlatDecomp[]> decomp, int cutoff, int i, final BufferedWriter br) {
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
        if (DEBUG) {
            System.out.println("Warning: DEBUG is turned on");
        }

        if (args.length > 0 && args[0].equals("create"))
        {
            createSimilarityRanking(args);
        } else if (args.length > 0 && args[0].equals("evaluate"))
        {
            evaluateSimilarityRanking(args);
        } else {
            System.err.println("Unknown method");
            System.exit(1);
        }
    }

    private static class CjkDecomp {
        String modifier;
        String[] comps;

        public CjkDecomp(String modifier, String[] comps)
        {
            this.modifier = modifier;
            this.comps = comps;
        }
    }

    private static class FlatDecomp {
        String comp;
        float centerVertical, centerHorizontal;

        public FlatDecomp(String comp, float left, float right, float top, float bottom)
        {
            this.comp = comp;
            this.centerVertical = bottom + (top + bottom) / 2;
            this.centerHorizontal = left + (left + right) / 2;
        }
    }

    /**
     * Take the cjk decomposition and recursively (until it hits a character from the stop radicals)
     * creates a list of all radicals the character consists of
     * @param args
     */
    private static Map<String, FlatDecomp[]> flattenDecomposition(String[] args)
    {
        String cjkDecompPath = args[1];
        String stopRadicalsPath = args[2];

        Set<String> radicals = readRadicals(stopRadicalsPath);
        Map<String, CjkDecomp> decomp = readCjkDecomp(cjkDecompPath);

        Map<String, FlatDecomp[]> flattened = new HashMap<>();
        for (String character : decomp.keySet()) {
            try {
                decomposeComponent(character, radicals, decomp, flattened, 0, 1.0f, 1.0f, 0);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // sort by component character
        for (FlatDecomp[] charDecomp : flattened.values()) {
            Arrays.sort(charDecomp, new Comparator<FlatDecomp>()
            {
                @Override
                public int compare(FlatDecomp a, FlatDecomp b)
                {
                    return a.comp.compareTo(b.comp);
                }
            });
        }

        return flattened;
    }

    private static List<FlatDecomp> decomposeComponent(String comp, Set<String> radicals, Map<String, CjkDecomp> decomp,
                                                   Map<String, FlatDecomp[]> flattened, float left, float right, float top, float bottom) throws ParseException
    {
        float width = right - left;
        float height = top - bottom;
        assert(width >= 0 && height >= 0);

        CjkDecomp d = decomp.get(comp);
        List<FlatDecomp> all = new ArrayList<>();
        if (d.comps.length == 0 || radicals.contains(comp)) {
            all.add(new FlatDecomp(comp, left, right, top, bottom));
        }
        else
        {
            if (d.modifier.equals("c")) {
                // no recursion since no constituents
                throw new ParseException("Malformatted decomp for " + comp, 0);
            }
            else if (d.modifier.startsWith("m")) {
                // modified, may include punishment factor later
                all.addAll(decomposeComponent(d.comps[0], radicals, decomp, flattened, left, right, top, bottom));
            }
            else if (d.modifier.startsWith("w")) {
                // second constituent somehow contained within the first one. Assume it has the same dimensions
                for (String subcomp : d.comps)
                    all.addAll(decomposeComponent(subcomp, radicals, decomp, flattened, left, right, top, bottom));
            }
            else if (d.modifier.startsWith("b")) {
                // second between first moving across or downwards. Not important, applies to few characters
                for (String subcomp : d.comps)
                    all.addAll(decomposeComponent(subcomp, radicals, decomp, flattened, left, right, top, bottom));
            }
            else if (d.modifier.startsWith("lock")) {
                // components locked together. Assume it has the same dimensions
                for (String subcomp : d.comps)
                    all.addAll(decomposeComponent(subcomp, radicals, decomp, flattened, left, right, top, bottom));
            } else if (d.modifier.startsWith("s")) {
                // first component surrounds second
                float left2, right2, top2, bottom2;
                if (d.modifier.startsWith("stl")) {
                    left2 = left + width / 2;
                    right2 = right;
                    top2 = bottom + height / 2;
                    bottom2 = bottom;
                }
                else if (d.modifier.startsWith("sbl")) {
                    left2 = left + width / 2;
                    right2 = right;
                    top2 = top;
                    bottom2 = bottom + height / 2;
                }
                else if (d.modifier.startsWith("str")) {
                    left2 = left;
                    right2 = left + width / 2;
                    top2 = bottom + height / 2;
                    bottom2 = bottom;
                }
                else if (d.modifier.startsWith("sbr")) {
                    left2 = left;
                    right2 = left + width / 2;
                    top2 = top;
                    bottom2 = bottom + height / 2;
                }
                else if (d.modifier.startsWith("sl")) {
                    left2 = left + width / 2;
                    right2 = right;
                    top2 = top - height / 4;
                    bottom2 = bottom + height / 4;
                }
                else if (d.modifier.startsWith("sr")) {
                    left2 = left;
                    right2 = left + width / 2;;
                    top2 = top - height / 4;
                    bottom2 = bottom + height / 4;
                }
                else if (d.modifier.startsWith("st")) {
                    left2 = left + width / 4;
                    right2 = right - width / 4;
                    top2 = bottom + height / 2;
                    bottom2 = bottom;
                }
                else if (d.modifier.startsWith("sb")) {
                    left2 = left + width / 4;
                    right2 = right - width / 4;
                    top2 = top;
                    bottom2 = bottom + height / 2;
                }
                else {
                    left2 = left + width / 4;
                    right2 = right - width / 4;
                    top2 = top - height / 4;
                    bottom2 = bottom + height / 4;
                }

                all.addAll(decomposeComponent(d.comps[0], radicals, decomp, flattened, left, right, top, bottom));
                all.addAll(decomposeComponent(d.comps[1], radicals, decomp, flattened, left2, right2, top2, bottom2));
            }
            else if (d.modifier.startsWith("a")) {
                // side to side
                float hSplit = left + width / 2;
                all.addAll(decomposeComponent(d.comps[0], radicals, decomp, flattened, left, hSplit, top, bottom));
                all.addAll(decomposeComponent(d.comps[1], radicals, decomp, flattened, hSplit, right, top, bottom));
            }
            else if (d.modifier.startsWith("d")) {
                // top to bottom
                float vSplit = bottom + height / 2;
                all.addAll(decomposeComponent(d.comps[0], radicals, decomp, flattened, left, right, top, vSplit));
                all.addAll(decomposeComponent(d.comps[1], radicals, decomp, flattened, left, right, vSplit, bottom));
            }
            else if (d.modifier.startsWith("r")) {
                // TODO
                all.addAll(decomposeComponent(d.comps[0], radicals, decomp, flattened, left, right, top, bottom));
            } else {
                // unknow, assume they both cover the width of the component
                for (String subcomp : d.comps)
                    all.addAll(decomposeComponent(subcomp, radicals, decomp, flattened, left, right, top, bottom));
            }
        }
        flattened.put(comp, all.toArray(new FlatDecomp[all.size()]));
        return all;
    }

    private static Map<String, CjkDecomp> readCjkDecomp(String path)
    {
        Map<String, CjkDecomp> result = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(path)));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (DEBUG && count % 20 != 0)
                    continue;
                String[] ss = line.split(":");
                assert(ss.length == 2);
                int modifierEnd = ss[1].indexOf('(');
                String modifier = ss[1].substring(0, modifierEnd);
                String allList = ss[1].substring(modifierEnd+1, ss[1].indexOf(')'));
                String[] comps = allList.length() > 0 ? allList.split(",") : new String[] {};
                result.put(ss[0], new CjkDecomp(modifier, comps));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    private static Set<String> readRadicals(String path)
    {
        Set<String> result = new HashSet<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(path)));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (DEBUG && count % 20 != 0)
                    continue;
                result.add(line.split(",")[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    private static void createSimilarityRanking(String[] args) {
        long start = System.currentTimeMillis();

        final Map<String, FlatDecomp[]> decomp = flattenDecomposition(args);
        final String outputFname = args[3];
        final int cutoff = Integer.parseInt(args[4]);
        final int nThreads = Integer.parseInt(args[5]);

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

    private static void evaluateSimilarityRanking(String[] args) {
        long start = System.currentTimeMillis();

        final String testcasesFname = args[3];

        final Map<String, FlatDecomp[]> decomp = flattenDecomposition(args);
        final List<String[]> testcases = readTestcases(testcasesFname);
        final List<String> allChars = new ArrayList<>(decomp.keySet());

        float totalScore = 0;
        int scoreCount = 0;
        int posSum = 0;
        int posCount = 0;

        final float[] similarities = new float[allChars.size()];
        for (int i = 0; i < testcases.size(); i++) {
            String character = testcases.get(i)[0];
            if (!decomp.containsKey(character)) {
                System.out.println("Skipping " + character + " because it's not included in the decomp");
                continue;
            }
            for (int j = 0; j < allChars.size(); j++) {
                // don't want the character itself
                if (!character.equals(allChars.get(j))) {
                    similarities[j] = calculateCharSimilarity(character, allChars.get(j), decomp);
                } else {
                    similarities[j] = 0;
                }
            }

            int[] similarSorted = ArrayUtils.argsort(similarities, false);

            StringBuilder sb = new StringBuilder();
            for (int pos = 0; pos < Math.min(20, similarSorted.length); pos++) {
                sb.append(allChars.get(similarSorted[pos]) + " ");
            }

            // check how reference characters were ranked
            List<Integer> positions = new ArrayList<>();
            float score = 0;
            for (int k = 1; k < testcases.get(i).length; k++) {
                int rankedPos = -1;
                for (int pos = 0; pos < similarSorted.length; pos++) {     // TODO could use map
                    if (allChars.get(similarSorted[pos]).equals(testcases.get(i)[k])) {
                        rankedPos = pos;
                        posSum += pos;
                        posCount++;
                        break;
                    }
                }
                if (rankedPos == -1)
                    rankedPos = 10000000;

                positions.add(rankedPos);

                score += 1.0f / k * 1.0f / (rankedPos + 1);
                scoreCount++;
            }
            totalScore += score;

            System.out.print("Character " + character + ": " + sb.toString() + ", Reference: ");
            for (int k = 1; k < testcases.get(i).length; k++) {
                System.out.print(testcases.get(i)[k] + " (" + positions.get(k - 1) + "), ");
            }
            System.out.println(" -> Score " + score);
        }

        System.out.println("Avg score: " + totalScore / scoreCount);
        System.out.println("Avg position: " + (float) posSum / posCount);
        System.out.println("Done after " + (System.currentTimeMillis() - start) + " ms");
    }


    /***
     *
     * @return Value between 0 (very dissimilar) and 1 (identical)
     */
    private static float calculateCharSimilarity(String c1, String c2, Map<String, FlatDecomp[]> decomp) {
        if (c1.equals(c2))
            return 1;

        // TODO incorporate positions within the character

        // component overlap
        float totalScore = 0;
        FlatDecomp[] dc1 = decomp.get(c1);
        FlatDecomp[] dc2 = decomp.get(c2);

        int i = 0;
        int j = 0;

        while (i < dc1.length && j < dc2.length) {
            if (dc1[i].comp.equals(dc2[j].comp)) {
                String sameComp = dc1[i].comp;
                while (i < dc1.length && dc1[i].comp.equals(sameComp) && j < dc2.length && dc2[j].comp.equals(sameComp)) {
                    i++;
                    j++;
                    totalScore += 2;
                }
                // additional occurrences of the same character in only one decomposition don't increase the score
                while (i < dc1.length && dc1[i].comp.equals(sameComp)) {
                    i++;
                }
                while (j < dc2.length && dc2[j].comp.equals(sameComp)) {
                    j++;
                }
            } else if (dc1[i].comp.compareTo(dc2[j].comp) < 0) {  // advance pointer to smaller component
                i++;
            } else {
                j++;
            }
        }

        totalScore /= dc1.length + dc2.length;

        return totalScore;
    }
}
