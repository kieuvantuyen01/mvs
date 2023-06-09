package com.mvs.app.solver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Z3Runner {
//	public static List<String> runZ3(String filename) 
//			throws IOException {

//		return result;
//	}

    public static List<String> runZ3(String filename)
            throws IOException {
        List<String> result = new ArrayList<String>();
        String s;

        //System.err.println(filename);
        if (System.getProperty("os.name").equalsIgnoreCase("Mac OS X")) {
            try {
                Process p = Runtime.getRuntime().exec("/usr/local/bin/z3 -smt2 -st -T:1500 " + filename);

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((s = br.readLine()) != null) {
//                        System.out.println("line: " + s);
                    result.add(s);
                    //System.err.println(s);
                }
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // System.out.println ("exit: " + p.exitValue());

                //System.out.println("size: " + result.size());

            } catch (Exception e) {
            }
        } else if (System.getProperty("os.name").equalsIgnoreCase("Linux")) {
            try {
                Process p = Runtime.getRuntime().exec("z3 -smt2 -st -T:1500 " + filename);

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((s = br.readLine()) != null) {
                    //System.out.println("line: " + s);
                    result.add(s);
                    //System.err.println(s);
                }
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // System.out.println ("exit: " + p.exitValue());

                //System.out.println("size: " + result.size());

            } catch (Exception e) {
            }
        } else {
//    		List<String> result = new ArrayList<String>();
            String pathToZ3 = "solvers\\z3\\bin\\z3.exe";
            // System.out.println("z3 file name: " + filename);
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", pathToZ3 + " -smt2 -st -T:1500 " + filename);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            //System.err.println("p: " + p);
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) {
                    break;
                }
                result.add(line);
            }
        }


        //p.destroy();
        return result;
    }

    public static List<String> runZ3(String filename, int time)
            throws IOException {
        List<String> result = new ArrayList<String>();
        String pathToZ3 = "z3\\bin\\z3.exe";
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", pathToZ3 + " -smt2 -st -T:1500" + time + filename);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }
            result.add(line);
        }
        return result;
    }

    public static void main(String[] args) {
        List<String> result = new ArrayList<String>();
        try {
            result = runZ3("./src/main/resources/smt/transmitter_2.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String s : result) {
            System.out.println(s);
        }
    }
}
