package com.mvs.app.verification.report;

import com.mvs.app.utils.PrefixToInfix;
import com.mvs.cfg.index.Var;
import com.mvs.cfg.utils.FunctionHelper;

import java.util.ArrayList;
import java.util.List;


public class Report {

    private List<Var> listParameter;
    private VerificationReport report;
    private List<String> result; // result of running solver
    private String functionName;
    private int mode;

    public void setMode(int mode) {
        this.mode = mode;
    }

    public static String parseError(String error) {
        int begin = error.indexOf("\"") + 1;
        int end = error.lastIndexOf("\"");

        return error.substring(begin, end);
    }

    public static VerificationReport getToTest() {
        List<String> list = new ArrayList<>();
        list.add("(error \"line 11 column 21: unknown constant IN\")");
        list.add("(error \"line 11 column 22: unknown constant IN\")");
        list.add("sat");
        list.add("(model");
        list.add("  (define-fun IN_0 () Real");
        list.add("    (- (/ 3.0 2.0)))");
        list.add("  (define-fun return () Real");
        list.add("    (- (/ 71979.0 71680.0)))");
        list.add("  (define-fun result_0 () Real");
        list.add("    (- (/ 71979.0 71680.0)))");
        list.add("  (define-fun x_0 () Real");
        list.add("    (- (/ 3.0 2.0)))");
        list.add(")");
        list.add("(:eliminated-vars    3");
        list.add(" :memory             2.70");
        list.add(" :nlsat-decisions    1");
        list.add(" :nlsat-propagations 3");
        list.add(" :nlsat-stages       2");
        list.add(" :time               0.01");
        list.add(" :total-time         0.01)");

        Report report = new Report();
        VerificationReport verificationReport = report.generateReport(list);
        verificationReport.print();

        return verificationReport;
    }

    public static void main(String[] args) {

        List<String> list = new ArrayList<>();
        list.add("(error \"line 11 column 21: unknown constant IN\")");
        list.add("(error \"line 11 column 22: unknown constant IN\")");
        list.add("sat");
        list.add("(model");
        list.add("  (define-fun IN_0 () Real");
        list.add("    (- (/ 3.0 2.0)))");
        list.add("  (define-fun return () Real");
        list.add("    (- (/ 71979.0 71680.0)))");
        list.add("  (define-fun result_0 () Real");
        list.add("    (- (/ 71979.0 71680.0)))");
        list.add("  (define-fun x_0 () Real");
        list.add("    (- (/ 3.0 2.0)))");
        list.add(")");
        list.add("(:eliminated-vars    3");
        list.add(" :memory             2.70");
        list.add(" :nlsat-decisions    1");
        list.add(" :nlsat-propagations 3");
        list.add(" :nlsat-stages       2");
        list.add(" :time               0.01");
        list.add(" :total-time         0.01)");

        List<Var> listParameter = new ArrayList<>();
        listParameter.add(new Var("real", "IN"));

        Report report = new Report();
        report.setListParameter(listParameter);
        VerificationReport verificationReport = report.generateReport(list);
        verificationReport.print();
    }

    /**
     * @return the listParameter
     */
    public List<Var> getListParameter() {
        return listParameter;
    }

    /**
     * @param listParameter the listParameter to set
     */
    public void setListParameter(List<Var> listParameter) {
        this.listParameter = listParameter;
    }

    public VerificationReport generateReport(List<String> result) {

        this.result = result;
        report = new VerificationReport();
        report.setResult(result);

        List<String> listError = new ArrayList<>();

        String str;
        String status = null;
        int time = 0;
        @SuppressWarnings("unused")
        String model = "";

        int i = 0;
        int n = result.size();
        while (i < n) {
            str = result.get(i);

            if (!str.contains("(") && !str.contains(")") && status == null) {
                status = str;
            } else if (str.contains("(error")) {
                listError.add(parseError(str));
            } else if (str.contains("(model")) {
                i++;
                int begin = i;
                int end = 0;
                while (i < n) {
                    str = result.get(i);

                    if (str.equals(")")) {
                        end = i - 1;
                        break;
                    } else {
                        i++;
                    }
                }

                parseModel(begin, end);
            } else if (str.contains(":total-time")) {
                String[] temp = str.split("[ ]+|[)]");
                time = (int) (Float.parseFloat(temp[temp.length - 1]) * 1000);
            }

            i++;
        }

        report.setErrors(listError);
        report.setStatus(status, mode);
        report.setSolverTime(time);

        return report;
    }

    private void parseModel(int begin, int end) {
        if (listParameter == null)
            return;

        List<DefineFun> paramtersDefineFun = new ArrayList<>();
        int i;
        for (Var v : listParameter) {
            if (v.getName().equals("return"))
                continue;
            String varName = v.getName() + "_0";
            i = begin;
            while (i <= end) {
                if (result.get(i).contains(" " + varName + " ")) {
                    String valueStr = "";
                    i++;
                    while (i <= end && !result.get(i).contains("define-fun")) {
                        valueStr += result.get(i);
                        i++;
                    }

                    String value = getValue(valueStr);
                    paramtersDefineFun.add(new DefineFun(removePostFix(v.getName(), FunctionHelper.getShortenName(functionName)), v.getType(), value));
                    break;
                }

                i++;
            }
        }
        report.setParameters(paramtersDefineFun);

        i = begin;
        while (i <= end) {
            if (result.get(i).contains("return_" + FunctionHelper.getShortenName(functionName))) {
                String valueStr = "";
                i++;
                while (i <= end && !result.get(i).contains("define-fun")) {
                    valueStr += result.get(i);
                    i++;
                }
                String value = getValue(valueStr);

                report.setReturn(new DefineFun("return", value));
                break;
            }
            i++;
        }
    }

    private String removePostFix(String varName, String funcName) {
        int index = varName.lastIndexOf(funcName);
        return varName.substring(0, index - 1);

    }

    private String getValue(String valueStr) {
        valueStr = valueStr.replace('(', ' ')
                .replace(')', ' ')
                .trim();

        String value = PrefixToInfix.prefixToInfix(valueStr);

        return value;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
}
