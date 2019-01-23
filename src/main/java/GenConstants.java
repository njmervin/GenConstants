import org.apache.commons.cli.*;

import java.io.*;
import java.util.*;

class ConstantInfo {
    public String group;
    public String key;
    public String value;
    public String comment;

    public String getValueType(String lang) {
        if(value.startsWith("\"")) {
            if (lang.equalsIgnoreCase("Java"))
                return "String";
            else if(lang.equalsIgnoreCase("C++"))
                return "constexpr char*";
            else
                return null;
        }
        else
            return "int";
    }

    public String getIdentifier() {
        return (group + "_" + key).replace('.', '_').toUpperCase();
    }

    public String getPrefix(String lang) {
        if(lang.equalsIgnoreCase("Java"))
            return "public static final";
        else if(lang.equalsIgnoreCase("C++"))
            return "static const";
        else
            return null;
    }

    public String getValueCode(String lang) {
        if(value.startsWith("\"")) {
            if (lang.equalsIgnoreCase("Java"))
                return value;
            else if(lang.equalsIgnoreCase("C++")) {
                try {
                    byte[] bytes = value.substring(1, value.length() - 1).getBytes("utf-8");
                    StringBuilder sb = new StringBuilder(bytes.length * 3);
                    sb.append('"');
                    for(int b : bytes) {
                        sb.append("\\x");
                        b = (b + 256 ) % 256;
                        if(b < 0x10) {
                            sb.append('0');
                            sb.append(Integer.toHexString(b));
                        }
                        else
                            sb.append(Integer.toHexString(b));
                    }
                    sb.append('"');
                    return sb.toString();
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            }
            else
                return null;
        }

        return value;
    }

    public String getComment(String lang) {
        if(value.startsWith("\"")) {
            if(lang.equalsIgnoreCase("C++")) {
                if (comment == null || comment.isEmpty())
                    return value;
            }
        }

        return comment;
    }
}

public class GenConstants {
    private static String output_package;
    private static String output_name;
    private static ArrayList<String> group_order_list = new ArrayList<String>();
    private static Map<String, List<ConstantInfo>>  output_constants = new HashMap<String, List<ConstantInfo>>();

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addOption("h", "help", false, "print help");
        options.addOption(null, "lang", true, "program language");
        options.addOption(null, "in", true, "configuration file path");
        options.addOption(null, "out", true, "contain constant definition file directory");
        CommandLine cmd = new DefaultParser().parse(options, args);

        if(cmd.getOptions().length == 0 || cmd.hasOption('h')) {
            new HelpFormatter().printHelp("java -jar GenConstants [option]", options);
            return;
        }

        if(!cmd.hasOption("lang") || !cmd.hasOption("in") || !cmd.hasOption("out")) {
            new HelpFormatter().printHelp("java -jar GenConstants [option]", options);
            return;
        }

        parseInput(cmd.getOptionValue("in"));

        if(output_package == null) {
            System.err.println("$package not found!");
            return;
        }

        if(output_name == null) {
            System.err.println("$name not found!");
            return;
        }

        //sort group
        for(String group : output_constants.keySet()) {
            group_order_list.add(group);
        }
        Collections.sort(group_order_list);


        String lang = cmd.getOptionValue("lang");
        if(lang.equalsIgnoreCase("Java"))
            outputJava(lang, cmd.getOptionValue("out"));
        else if(lang.equalsIgnoreCase("C++"))
            outputCpp(lang, cmd.getOptionValue("out"));
        else {
            System.err.println("language " + lang + " is not supported.");
            return;
        }
    }

    private static ConstantInfo parseLine(String line) {
        int pos;
        String group = "", key = "", value = "", comment = "";

        pos = line.indexOf('#');
        if(pos != -1) {
            comment = line.substring(pos + 1).trim();
            line = line.substring(0, pos);
        }

        pos = line.indexOf('=');
        if(pos != -1) {
            value = line.substring(pos + 1).trim();
            line = line.substring(0, pos).trim();
        }

        pos = line.indexOf('.');
        if(pos != -1) {
            key = line.substring(pos + 1).trim();
            group = line.substring(0, pos).trim();
        }
        else {
            key = line.trim();
        }

        if(key.isEmpty())
            return null;

        ConstantInfo info = new ConstantInfo();
        info.group = group;
        info.key = key;
        info.value = value;
        info.comment = comment;
        return info;
    }

    private static void parseInput(String inputfile) throws IOException {
        boolean use_autoid = false;
        long autoid = 0;
        String line;
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile),"UTF-8"));

        while((line = in.readLine()) != null) {
            ConstantInfo info = parseLine(line);
            if(info == null)
                continue;

            if(info.key.equals("$package")) {
                output_package = info.value;
                continue;
            }
            else if(info.key.equals("$name")) {
                output_name = info.value;
                continue;
            }
            else if(info.key.equals("$autoid")) {
                use_autoid = true;
                if(info.value.isEmpty())
                    autoid = 1;
                else {
                    autoid = Long.parseLong(info.value);
                }
                continue;
            }

            if(info.value.isEmpty()) {
                if(use_autoid)
                    info.value = Long.toString(autoid++);
                else
                    continue;
            }
            else {
                use_autoid = false;
            }

            List<ConstantInfo> list = output_constants.get(info.group.toLowerCase());
            if(list == null) {
                list = new ArrayList<ConstantInfo>();
                output_constants.put(info.group.toLowerCase(), list);
            }
            list.add(info);
        }

        in.close();
    }

    private static int[] getTypeKeyValueLength(String lang, List<ConstantInfo> list) {
        int[] lens = new int[]{0, 0, 0};

        for(ConstantInfo info : list) {
            lens[0] = Math.max(lens[0], info.getPrefix(lang).length() + 1 + info.getValueType(lang).length());
            lens[1] = Math.max(lens[1], info.getIdentifier().length());
            lens[2] = Math.max(lens[2], info.value.length());
        }

        return lens;
    }

    private static void outputItems(String lang, PrintStream out) {
        String text;

        for(String key : group_order_list) {
            List<ConstantInfo> list = output_constants.get(key);
            int[] lens = getTypeKeyValueLength(lang, list);

            for(ConstantInfo info : list) {
                out.print("    ");
                out.print(info.getPrefix(lang));
                out.print(" ");
                out.print(info.getValueType(lang));
                for(int i=0; i<lens[0] - info.getPrefix(lang).length() - info.getValueType(lang).length() - 1; i++)
                    out.print(" ");
                out.print(" ");
                out.print(info.getIdentifier());
                for(int i=0; i<lens[1] - info.getIdentifier().length(); i++)
                    out.print(" ");

                out.print(" = ");
                out.print(info.getValueCode(lang));
                out.print(";");

                text = info.getComment(lang);
                if(text != null && !text.isEmpty()) {
                    for(int i=0; i<lens[2] - info.value.length(); i++)
                        out.print(" ");

                    out.print(" //");
                    out.print(info.getComment(lang));
                }
                out.println();
            }


            out.println();
        }
    }

    private static void outputJava(String lang, String outdir) throws IOException {
        String path = outdir + "\\" + output_package.replace('.', '\\');
        new File(path).mkdirs();

        File file = new File(path + "\\" + output_name + ".java");
        PrintStream out = new PrintStream(file, "utf-8");

        out.println("package " + output_package + ";");
        out.println();
        out.println("public class " + output_name + " {");

        outputItems(lang, out);

        out.println("}");

        out.close();
    }

    private static void outputCpp(String lang, String outdir) throws IOException {
        new File(outdir).mkdirs();

        File file = new File(outdir + "\\" + output_name.toLowerCase() + ".h");
        PrintStream out = new PrintStream(file, "utf-8");

        out.println("#pragma once");
        out.println();
        out.println("class " + output_name);
        out.println("{");
        out.println("public:");

        outputItems(lang, out);

        out.println("};");

        out.close();
    }
}
