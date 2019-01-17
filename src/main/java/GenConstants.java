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
                return "const char *";
            else
                return null;
        }
        else
            return "int";
    }

    public String getIdentifier() {
        return (group + "_" + key).replace('.', '_').toUpperCase();
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
            outputJava(cmd.getOptionValue("out"));
        else if(lang.equalsIgnoreCase("C++"))
            outputCpp(cmd.getOptionValue("out"));
        else {
            System.err.println("language " + lang + " is not supported.");
            return;
        }
    }

    private static void parseInput(String inputfile) throws IOException {
        String line;
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile),"UTF-8"));

        while((line = in.readLine()) != null) {
            line = line.trim();
            if(line.isEmpty()) {
                continue;
            }

            int pos;
            String group = "", key = "", value = "", comment = "";

            pos = line.indexOf('#');
            if(pos != -1) {
                comment = line.substring(pos + 1).trim();
                line = line.substring(0, pos);
            }

            pos = line.indexOf('=');
            if(pos == -1)
                continue;
            value = line.substring(pos + 1).trim();
            line = line.substring(0, pos).trim();
            if(line.equals("$package")) {
                output_package = value;
                continue;
            }
            else if(line.equals("$name")) {
                output_name = value;
                continue;
            }

            pos = line.indexOf('.');
            if(pos == -1)
                continue;
            key = line.substring(pos + 1);
            group = line.substring(0, pos);

            ConstantInfo info = new ConstantInfo();
            info.group = group;
            info.key = key;
            info.value = value;
            info.comment = comment;

            List<ConstantInfo> list = output_constants.get(group.toLowerCase());
            if(list == null) {
                list = new ArrayList<ConstantInfo>();
                output_constants.put(group.toLowerCase(), list);
            }
            list.add(info);
        }

        in.close();
    }

    private static int[] getKeyValueLength(List<ConstantInfo> list) {
        int[] lens = new int[]{0, 0};

        for(ConstantInfo info : list) {
            lens[0] = Math.max(lens[0], info.getIdentifier().length());
            lens[1] = Math.max(lens[1], info.value.length());
        }

        return lens;
    }

    private static void outputJava(String outdir) throws IOException {
        String path = outdir + "\\" + output_package.replace('.', '\\');
        new File(path).mkdirs();

        File file = new File(path + "\\" + output_name + ".java");
        PrintStream out = new PrintStream(file, "utf-8");

        out.println("package " + output_package + ";");
        out.println();
        out.println("public class " + output_name + " {");

        for(String key : group_order_list) {
            List<ConstantInfo> list = output_constants.get(key);
            int[] lens = getKeyValueLength(list);

            for(ConstantInfo info : list) {
                out.print("    public static final ");
                out.print(info.getValueType("Java"));
                out.print(" ");
                out.print(info.getIdentifier());

                for(int i=0; i<lens[0] - info.getIdentifier().length(); i++)
                    out.print(" ");

                out.print(" = ");
                out.print(info.value);
                out.print(";");
                if(!info.comment.isEmpty()) {
                    for(int i=0; i<lens[1] - info.value.length(); i++)
                        out.print(" ");

                    out.print(" //");
                    out.print(info.comment);
                }
                out.println();
            }


            out.println();
        }

        out.println("}");

        out.close();
    }

    private static void outputCpp(String outdir) throws IOException {
        new File(outdir).mkdirs();

        File file = new File(outdir + "\\" + output_name.toLowerCase() + ".h");
        PrintStream out = new PrintStream(file, "utf-8");

        out.println("#pragma once");
        out.println();
        out.println("class " + output_name);
        out.println("{");
        out.println("public:");

        for(String key : group_order_list) {
            List<ConstantInfo> list = output_constants.get(key);
            int[] lens = getKeyValueLength(list);

            for(ConstantInfo info : list) {
                out.print("    static const ");
                out.print(info.getValueType("C++"));
                out.print(" ");
                out.print(info.getIdentifier());

                for(int i=0; i<lens[0] - info.getIdentifier().length(); i++)
                    out.print(" ");

                out.print(" = ");
                out.print(info.value);
                out.print(";");
                if(!info.comment.isEmpty()) {
                    for(int i=0; i<lens[1] - info.value.length(); i++)
                        out.print(" ");

                    out.print(" //");
                    out.print(info.comment);
                }
                out.println();
            }

            out.println();
        }

        out.println("};");

        out.close();
    }
}
