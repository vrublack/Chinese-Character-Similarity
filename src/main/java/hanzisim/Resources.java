package hanzisim;

import org.apache.commons.cli.CommandLine;

import java.io.*;
import java.text.ParseException;
import java.util.*;


public class Resources  {
    /**
     * Take the cjk decomposition and recursively (until it hits a character from the stop radicals)
     * creates a list of all radicals the character consists of
     *
     * @param args
     */
    public static Map<String, FlatDecomp[]> flattenDecomposition(CommandLine args) {
        String cjkDecompPath = args.getOptionValue("decomp");
        String stopRadicalsPath = args.getOptionValue("radicals");

        Set<String> radicals = readRadicals(stopRadicalsPath);
        Map<String, CjkDecomp> decomp = readCjkDecomp(cjkDecompPath);

        Map<String, FlatDecomp[]> flattened = new HashMap<>();
        for (String character : decomp.keySet()) {
            try {
                List<FlatDecomp> all = decomposeComponent(character, radicals, decomp, flattened, 0, 1.0f, 1.0f, 0);
                flattened.put(character, all.toArray(new FlatDecomp[all.size()]));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // sort by component character
        for (FlatDecomp[] charDecomp : flattened.values()) {
            Arrays.sort(charDecomp, new Comparator<FlatDecomp>() {
                @Override
                public int compare(FlatDecomp a, FlatDecomp b) {
                    return a.comp.compareTo(b.comp);
                }
            });
        }

        return flattened;
    }

    public static List<FlatDecomp> decomposeComponent(String comp, Set<String> radicals, Map<String, CjkDecomp> decomp,
                                                       Map<String, FlatDecomp[]> flattened, float left, float right, float top, float bottom) throws ParseException {
        float width = right - left;
        float height = top - bottom;
        assert (width >= 0 && height >= 0);

        CjkDecomp d = decomp.get(comp);
        List<FlatDecomp> all = new ArrayList<>();
        if (d.comps.length == 0 || radicals.contains(comp)) {
            all.add(new FlatDecomp(comp, left, right, top, bottom));
        } else {
            if (d.modifier.equals("c")) {
                // no recursion since no constituents
                throw new ParseException("Malformatted decomp for " + comp, 0);
            } else if (d.modifier.startsWith("m")) {
                // modified, may include punishment factor later
                all.addAll(decomposeComponent(d.comps[0], radicals, decomp, flattened, left, right, top, bottom));
            } else if (d.modifier.startsWith("w")) {
                // second constituent somehow contained within the first one. Assume it has the same dimensions
                for (String subcomp : d.comps)
                    all.addAll(decomposeComponent(subcomp, radicals, decomp, flattened, left, right, top, bottom));
            } else if (d.modifier.startsWith("b")) {
                // second between first moving across or downwards. Not important, applies to few characters
                for (String subcomp : d.comps)
                    all.addAll(decomposeComponent(subcomp, radicals, decomp, flattened, left, right, top, bottom));
            } else if (d.modifier.startsWith("lock")) {
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
                } else if (d.modifier.startsWith("sbl")) {
                    left2 = left + width / 2;
                    right2 = right;
                    top2 = top;
                    bottom2 = bottom + height / 2;
                } else if (d.modifier.startsWith("str")) {
                    left2 = left;
                    right2 = left + width / 2;
                    top2 = bottom + height / 2;
                    bottom2 = bottom;
                } else if (d.modifier.startsWith("sbr")) {
                    left2 = left;
                    right2 = left + width / 2;
                    top2 = top;
                    bottom2 = bottom + height / 2;
                } else if (d.modifier.startsWith("sl")) {
                    left2 = left + width / 2;
                    right2 = right;
                    top2 = top - height / 4;
                    bottom2 = bottom + height / 4;
                } else if (d.modifier.startsWith("sr")) {
                    left2 = left;
                    right2 = left + width / 2;
                    ;
                    top2 = top - height / 4;
                    bottom2 = bottom + height / 4;
                } else if (d.modifier.startsWith("st")) {
                    left2 = left + width / 4;
                    right2 = right - width / 4;
                    top2 = bottom + height / 2;
                    bottom2 = bottom;
                } else if (d.modifier.startsWith("sb")) {
                    left2 = left + width / 4;
                    right2 = right - width / 4;
                    top2 = top;
                    bottom2 = bottom + height / 2;
                } else {
                    left2 = left + width / 4;
                    right2 = right - width / 4;
                    top2 = top - height / 4;
                    bottom2 = bottom + height / 4;
                }

                all.addAll(decomposeComponent(d.comps[0], radicals, decomp, flattened, left, right, top, bottom));
                all.addAll(decomposeComponent(d.comps[1], radicals, decomp, flattened, left2, right2, top2, bottom2));
            } else if (d.modifier.startsWith("a")) {
                // side to side
                float hSplit = left + width / 2;
                all.addAll(decomposeComponent(d.comps[0], radicals, decomp, flattened, left, hSplit, top, bottom));
                all.addAll(decomposeComponent(d.comps[1], radicals, decomp, flattened, hSplit, right, top, bottom));
            } else if (d.modifier.startsWith("d")) {
                // top to bottom
                float vSplit = bottom + height / 2;
                all.addAll(decomposeComponent(d.comps[0], radicals, decomp, flattened, left, right, top, vSplit));
                all.addAll(decomposeComponent(d.comps[1], radicals, decomp, flattened, left, right, vSplit, bottom));
            } else if (d.modifier.startsWith("r")) {
                assert(d.comps.length == 1);
                int repititions;
                if (d.modifier.startsWith("rot")) {
                    // don't include punishing term as of right now
                    repititions = 1;
                } else if (Character.isDigit(d.modifier.charAt(1))) {
                    repititions = Integer.parseInt(String.valueOf(d.modifier.charAt(1)));
                } else if (d.modifier.startsWith("rr") || d.modifier.startsWith("ra") || d.modifier.startsWith("rd") || d.modifier.startsWith("rst")) {
                    repititions = 2;
                }
                else {
                    repititions = 1;
                }
                List<FlatDecomp> children = decomposeComponent(d.comps[0], radicals, decomp, flattened, left, right, top, bottom);
                for (int i = 0; i < repititions; i++)
                    all.addAll(children);
            } else {
                // unknow, assume they both cover the width of the component
                for (String subcomp : d.comps)
                    all.addAll(decomposeComponent(subcomp, radicals, decomp, flattened, left, right, top, bottom));
            }
        }
        return all;
    }

    public static Map<String, CjkDecomp> readCjkDecomp(String path) {
        Map<String, CjkDecomp> result = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(path)));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (Definitions.DEBUG && count % 20 != 0)
                    continue;
                String[] ss = line.split(":");
                assert (ss.length == 2);
                int modifierEnd = ss[1].indexOf('(');
                String modifier = ss[1].substring(0, modifierEnd);
                String allList = ss[1].substring(modifierEnd + 1, ss[1].indexOf(')'));
                String[] comps = allList.length() > 0 ? allList.split(",") : new String[]{};
                result.put(ss[0], new CjkDecomp(modifier, comps));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    public static Set<String> readRadicals(String path) {
        Set<String> result = new HashSet<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(path)));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (Definitions.DEBUG && count % 20 != 0)
                    continue;
                result.add(line.split(",")[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    public static Map<String, String[]> readDecomposition(String fname) {
        Map<String, String[]> result = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(fname)));
            String line = reader.readLine();    // skip header
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (Definitions.DEBUG && count % 20 != 0)
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

    public static List<String[]> readTestcases(String fname) {
        List<String[]> result = new ArrayList<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(fname)));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (Definitions.DEBUG && count % 20 != 0)
                    continue;
                result.add(line.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    public static Map<String, String[]> readJapaneseToSimplChinese(String fname) {
        Map<String, String[]> result = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(fname)));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (count >= 17) {  // header
                    String[] comps = line.split("\t");
                    String jpn = comps[0];
                    String chn = comps[2];
                    result.put(jpn, chn.split(","));
                }
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    public static void write(final BufferedWriter br, final String s) throws IOException {
        synchronized (br) {
            br.write(s);
        }
    }

}
