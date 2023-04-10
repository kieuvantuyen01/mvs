package com.mvs.app.verification;

import com.mvs.app.verification.report.VerificationReport;
import com.mvs.app.verification.userassertion.AssertionMethod;
import com.mvs.cfg.build.ASTFactory;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunction;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPVariable;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileVerification {

    private List<String> functionList = new ArrayList<String>();;
    private List<String> variableList = new ArrayList<String>();;
    public static final String C_TAG = ".c";
    public static final String CPP_TAG = ".cpp";
    public static final String PP_FILE_TAG = ".xml";
    private int loopCount = -1;

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }

    public FileVerification()
           throws RowsExceededException, WriteException, IOException
            {

    }
    public List<VerificationReport> verifyDirectory(File directory) throws WriteException, IOException {
        List<VerificationReport> reportList = new ArrayList<>();
        List<VerificationReport> reports;

        if (directory == null) {
            return reportList;
        } else if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            for (File f : files) {
                reports = verifyDirectory(f);
                reportList.addAll(reports);
            }
        } else {
            reports = verify(directory);
            reportList.addAll(reports);
        }

        return reportList;
    }
    public List<VerificationReport> verifyDirectory(File directory, int mode) throws WriteException, IOException {
        List<VerificationReport> reportList = new ArrayList<>();
        List<VerificationReport> reports;

        if (directory == null) {
            return reportList;
        } else if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            for (File f : files) {
                reports = verifyDirectory(f, mode);
                reportList.addAll(reports);
            }
        } else {
            reports = verify(directory, mode);
            reportList.addAll(reports);
        }

        return reportList;
    }

    public List<VerificationReport> verify(File file, int mode) {
        List<VerificationReport> reportList = new ArrayList<>();

        if (file == null) {
            System.out.println("file is null");
            return reportList;
        }

        String filePath = file.getAbsolutePath();
        String CPPFilename = getCFilename(file);
        String PPPathFile;

        if (CPPFilename == null) {
            System.out.println("not cpp file");
            return reportList;
        } else {
            PPPathFile = CPPFilename + PP_FILE_TAG;
        }

        ASTFactory ast = new ASTFactory(filePath);
        ArrayList<IASTFunctionDefinition> listFunction = ast.getListFunction();
        FunctionVerification mv = new FunctionVerification();

        File PPFile = new File(PPPathFile);
        if (!PPFile.exists()) {
            System.err.println("file is not exist: " + PPPathFile);
            return null;
        }

        List<AssertionMethod> listAssertion = AssertionMethod.getUserAssertions(PPFile);

        VerificationReport report;

        for (AssertionMethod am : listAssertion) {
            //System.err.println("***Verification report:");
            System.out.println("- Method name: " + am.getMethodName());
            int nLoops;
            if (loopCount == -1) {
                if (am.getLoopCount() != null && am.getLoopCount().matches("^\\d+$")) {
                    nLoops = Integer.parseInt(am.getLoopCount());
                } else {
                    nLoops = 100;
                }
            } else {
                nLoops = loopCount;
            }

            for (IASTFunctionDefinition function : listFunction) {
                String functionName = getFunctionName(function);
                if (functionName.equals(am.getMethodName())) {
                    try {
                        long start = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                        System.err.println("function: " + functionName);
                        if (mode == FunctionVerification.UNFOLD_MODE) {
                            int incr = (nLoops < 1025) ? nLoops : 1025;
                            report = mv.verify(ast, function, am.getPreCondition(), am.getPostCondition(), incr, mode);
                        }
                        else { //Invariant_Mode
                            report = mv.verify(ast, function, am.getPreCondition(), am.getPostCondition(), nLoops, mode);
                        }

                        if (report != null) {
                            reportList.add(report);
                        }
                        long end = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return reportList;
    }

    private void processNode(IASTDeclaration node) {
        if (node instanceof CPPASTFunctionDefinition) {
            CPPASTFunctionDefinition functionDefinition = (CPPASTFunctionDefinition) node;
            IASTFunctionDeclarator declarator = functionDefinition.getDeclarator();
            IASTName name = declarator.getName();
            functionList.add(name.toString());

            IASTStatement body = functionDefinition.getBody();
            if (body != null) {
                IASTNode[] children = body.getChildren();
                for (IASTNode child : children) {
                    if (child instanceof IASTDeclaration) {
                        processNode((IASTDeclaration) child);
                    } else if (child instanceof IASTExpression) {
                        processNode((IASTDeclaration) child);
                    }
                }
            }
        } else if (node instanceof CPPASTSimpleDeclaration) {
            CPPASTSimpleDeclaration simpleDeclaration = (CPPASTSimpleDeclaration) node;
            IASTDeclSpecifier declSpecifier = simpleDeclaration.getDeclSpecifier();
            if (declSpecifier instanceof CPPASTCompositeTypeSpecifier) {
                CPPASTCompositeTypeSpecifier compositeTypeSpecifier = (CPPASTCompositeTypeSpecifier) declSpecifier;
                IASTDeclaration[] members = compositeTypeSpecifier.getMembers();
                for (IASTDeclaration member : members) {
                    if (member instanceof CPPASTSimpleDeclaration) {
                        CPPASTSimpleDeclaration memberDeclaration = (CPPASTSimpleDeclaration) member;
                        IASTDeclarator[] declarators = memberDeclaration.getDeclarators();
                        for (IASTDeclarator declarator : declarators) {
                            IASTName name = declarator.getName();
                            IBinding binding = name.resolveBinding();
                            if (binding instanceof ICPPVariable) {
                                ICPPVariable variable = (ICPPVariable) binding;
                                if (variable instanceof ICPPFunction) {
                                    functionList.add(binding.getName());
                                } else {
                                    variableList.add(binding.getName());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Phương thức đệ quy để lấy danh sách các biến được sử dụng trong một biểu thức
    private static void collectUsedVariables(IASTExpression expression, List<String> usedVariables) {
        if (expression instanceof IASTName) {
            IASTName name = (IASTName) expression;
            if (name instanceof CPPASTIdExpression) {
                IASTName variableName = ((CPPASTIdExpression) name).getName();
                if (variableName != null) {
                    usedVariables.add(variableName.toString());
                }
            }
        } else if (expression instanceof IASTUnaryExpression) {
            IASTUnaryExpression unaryExpr = (IASTUnaryExpression) expression;
            collectUsedVariables(unaryExpr.getOperand(), usedVariables);
        } else if (expression instanceof IASTBinaryExpression) {
            IASTBinaryExpression binaryExpr = (IASTBinaryExpression) expression;
            collectUsedVariables(binaryExpr.getOperand1(), usedVariables);
            collectUsedVariables(binaryExpr.getOperand2(), usedVariables);
        }
    }

    public List<VerificationReport> verify(File file) {
        List<VerificationReport> reportList = new ArrayList<>();

        if (file == null) {
            System.out.println("file is null");
            return reportList;
        }

        String filePath = file.getAbsolutePath();
        String CPPFilename = getCFilename(file);
        String PPPathFile;

        if (CPPFilename == null) {
            System.out.println("not cpp file");
            return reportList;
        } else {
            PPPathFile = CPPFilename + PP_FILE_TAG;
        }

        //IASTTranslationUnit ast = new ASTFactory(filePath).getTranslationUnit();

        /*List<String> functionList = new ArrayList<String>();
        // Get the list of functions
        // Duyệt từng For statement
        for (IASTDeclaration declExpr : ast.getDeclarations()) {
            CPPASTFunctionDefinition functionDefinition = (CPPASTFunctionDefinition) declExpr;
            IASTFunctionDeclarator declarator = functionDefinition.getDeclarator();
            IASTName name = declarator.getName();
            functionList.add(name.toString());

            IASTStatement body = functionDefinition.getBody();
            if (body != null) {
                IASTNode[] children = body.getChildren();
                if (declExpr instanceof CPPASTForStatement) {

                    for (IASTNode child : children) {
                        if (child instanceof IASTDeclarationStatement) {
                            IASTDeclaration decl = ((IASTDeclarationStatement) child).getDeclaration();
                            if (decl instanceof IASTSimpleDeclaration) {
                                IASTDeclarator[] declarators = ((IASTSimpleDeclaration) decl).getDeclarators();
                                *//*for (IASTDeclarator declarator : declarators) {
                                    // Add the variable name to the list of declarations
                                    declarator.getName();
                                }*//*
                            }
                        }
                    }



                    //System.out.println("For statement uses variables " + variables + " and variables " + usedVariables);
                }
            }
        }*/

        ASTFactory ast = new ASTFactory(filePath);
        ArrayList<IASTFunctionDefinition> listFunction = ast.getListFunction();
        FunctionVerification mv = new FunctionVerification();

        File PPFile = new File(PPPathFile);
        if (!PPFile.exists()) {
            System.err.println("file is not exist: " + PPPathFile);
            return null;
        }

        List<AssertionMethod> listAssertion = AssertionMethod.getUserAssertions(PPFile);

        VerificationReport report;

        for (AssertionMethod am : listAssertion) {
            //System.err.println("***Verification report:");
            System.out.println("- Method name: " + am.getMethodName());
            int nLoops;
            if (loopCount == -1) {
                if (am.getLoopCount() != null && am.getLoopCount().matches("^\\d+$")) {
                    nLoops = Integer.parseInt(am.getLoopCount());
                } else {
                    nLoops = 100;
                }
            } else {
                nLoops = loopCount;
            }

            for (IASTFunctionDefinition function : listFunction) {
                IASTFileLocation fileLocation = function.getFileLocation();
                int loc = fileLocation.getEndingLineNumber() - fileLocation.getStartingLineNumber();
                String functionName = getFunctionName(function);
                if (!functionName.equals(am.getMethodName())) {
                    try {
                        long start = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                        System.err.println("function: " + functionName);
                        //Verification steps
                        if (nLoops < 1000 && loc < 100) {
                            report = mv.verify(ast, function, am.getPreCondition(), am.getPostCondition(), nLoops, FunctionVerification.UNFOLD_MODE);
                            System.err.println("UNFOLD STRATEGY");
                            if (report != null) {
                                reportList.add(report);
                            }
                        } else  {
                            report = mv.verify(ast, function, am.getPreCondition(), am.getPostCondition(), nLoops, FunctionVerification.INVARIANT_MODE);
                            if (report.getStatus().equals(VerificationReport.ALWAYS_TRUE)) {
                                System.out.println("INVARIANT STRATEGY");
                                reportList.add(report);
                            } else {
                                if (nLoops > 1000) {
                                    nLoops = 100;
                                }
                                report = mv.verify(ast, function, am.getPreCondition(), am.getPostCondition(), nLoops, FunctionVerification.UNFOLD_MODE);
                                System.err.println("UNFOLD STRATEGY");
                                if (report != null) {
                                    reportList.add(report);
                                }
                            }
                        }

                        long end = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (functionName.equals(am.getMethodName())) {
                    try {
                        long start = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                        System.err.println("function: " + functionName);
                        //Verification steps
                        if (nLoops < 1000 && loc < 100) {
                            report = mv.verify(ast, function, am.getPreCondition(), am.getPostCondition(), nLoops, FunctionVerification.UNFOLD_MODE);
                            System.err.println("UNFOLD STRATEGY");
                            if (report != null) {
                                reportList.add(report);
                            }
                        } else  {
                            report = mv.verify(ast, function, am.getPreCondition(), am.getPostCondition(), nLoops, FunctionVerification.INVARIANT_MODE);
                            if (report.getStatus().equals(VerificationReport.ALWAYS_TRUE)) {
                                System.out.println("INVARIANT STRATEGY");
                                reportList.add(report);
                            } else {
                                if (nLoops > 1000) {
                                    nLoops = 100;
                                }
                                report = mv.verify(ast, function, am.getPreCondition(), am.getPostCondition(), nLoops, FunctionVerification.UNFOLD_MODE);
                                System.err.println("UNFOLD STRATEGY");
                                if (report != null) {
                                    reportList.add(report);
                                }
                            }
                        }

                        long end = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return reportList;
    }

    private VerificationReport getFunctionReport(AssertionMethod am, List<IASTFunctionDefinition> listFunction) {
        VerificationReport report = null;
        System.out.println("- Method name: " + am.getMethodName());
        for (IASTFunctionDefinition function : listFunction) {
            String functionName = getFunctionName(function);
            if (functionName.equals(am.getMethodName())) {

                    long start = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    System.err.println("function: " + functionName);

                    long end = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            }
        }
        return report;
    }
    private String getCFilename(File file) {
        String filename = file.getAbsolutePath();
        int index = filename.lastIndexOf(CPP_TAG);
        if (index >= 0) {
            filename = filename.substring(0, index);
        } else {
            index = filename.lastIndexOf(C_TAG);
            if (index >= 0) {
                filename = filename.substring(0, index);
            } else {
                filename = null;
            }
        }

        return filename;
    }

    private String getFunctionName(IASTFunctionDefinition function) {
        if (function == null)
            return null;
        return
                function.getDeclarator().getName().toString();
    }

}
