package com.mvs.cfg.build;

import com.mvs.cfg.utils.FunctionHelper;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Get IASTFunctionDefinition
 * IASTFunctionDefiniton func = (new ASTFactory(filelocation)).getFunction(index);
 * filelocation: String
 * index: int
 *
 * @author va
 */
public class ASTFactory {

    private static IASTTranslationUnit translationUnit;
    private String filelocation = "./test.c";

    public ASTFactory() {
        FileContent fileContent = FileContent.createForExternalFileLocation(filelocation);
        IncludeFileContentProvider includeFile = IncludeFileContentProvider.getEmptyFilesProvider();
        IParserLogService log = new DefaultLogService();
        String[] includePaths = new String[0];
        IScannerInfo info = new ScannerInfo(new HashMap<String, String>(), includePaths);
        try {
            translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, includeFile, null, 0, log);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public ASTFactory(IASTTranslationUnit ast) {
        translationUnit = ast;
    }

    public ASTFactory(String filelocation) {
        FileContent fileContent = FileContent.createForExternalFileLocation(filelocation);
        IncludeFileContentProvider includeFile = IncludeFileContentProvider.getEmptyFilesProvider();
        IParserLogService log = new DefaultLogService();
        String[] includePaths = new String[0];
        IScannerInfo info = new ScannerInfo(new HashMap<String, String>(), includePaths);
        try {
            translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, includeFile, null, 0, log);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void printTree(IASTNode node, int index) {
        IASTNode[] children = node.getChildren();

        for (int i = 0; i < index; i++) {
            System.out.print(" ");
        }

        System.out.println("-" + node.getClass().getSimpleName() + " -> " + node.getRawSignature());
        for (IASTNode iastNode : children)
            printTree(iastNode, index + 2);
    }

    /**
     * getters & setters
     */
    public IASTTranslationUnit getTranslationUnit() {
        return translationUnit;
    }

    public void setTranslationUnit(IASTTranslationUnit tranUnit) {
        translationUnit = tranUnit;
    }

    public void setFileLocation(String fileName) {
        filelocation = fileName;
    }

    public List<String> getFunctionSignatures() {
        if (translationUnit == null) return null;
        List<String> funcList = new ArrayList<>();
        for (IASTNode run : translationUnit.getDeclarations()) {
            if (run instanceof IASTFunctionDefinition) {
                String name = ((IASTFunctionDefinition) run).getDeclarator().getName().toString();
                funcList.add(name);
            }
        }
        return funcList;
    }
    public ArrayList<IASTFunctionDefinition> getListFunction() {
        if (translationUnit == null) return null;
        ArrayList<IASTFunctionDefinition> funcList = new ArrayList<>();
        for (IASTNode run : translationUnit.getDeclarations()) {
            if (run instanceof IASTFunctionDefinition) {
                funcList.add((IASTFunctionDefinition) run);
            }
        }
        return funcList;
    }

    public ArrayList<String> getGlobarVarStrList() {
        ArrayList<String> result = new ArrayList<>();
        for (IASTDeclaration decl : this.getGlobarVarList()) {
            IASTDeclarator[] declarators = ((IASTSimpleDeclaration) decl).getDeclarators();
            for (IASTDeclarator declarator : declarators) {
                String tmp = declarator.getName().toString();
                result.add(tmp);
            }
        }
        return result;
    }

    public ArrayList<IASTDeclaration> getGlobarVarList() {
        if (translationUnit == null) return null;
        ArrayList<IASTDeclaration> varList = new ArrayList<>();
        for (IASTDeclaration run : translationUnit.getDeclarations()) {
            if (run instanceof IASTSimpleDeclaration) {
                boolean isVar = true;
                IASTDeclarator[] declors = ((IASTSimpleDeclaration) run).getDeclarators();
                for (IASTDeclarator decl : declors) {
                    if (decl instanceof IASTStandardFunctionDeclarator) {
                        isVar = false;
                    }
                    if (isVar) varList.add(run);
                }
            }
        }
        return varList;
    }

    /**
     * functionDef
     */
//    public IASTFunctionDefinition getFunction(int index) {
////		int count = 0;
//        IASTFunctionDefinition funcDef = null;
//        IASTDeclaration[] declarations = translationUnit.getDeclarations();
//        for (IASTDeclaration d : declarations) {
//            if (d instanceof IASTFunctionDefinition) {
//                funcDef = (IASTFunctionDefinition) d;
//                break;
//            }
//        }
//        return funcDef;
//    }
    public IASTFunctionDefinition getFunction(int index) {
        ArrayList<IASTFunctionDefinition> funcList = getListFunction();
        if(index < funcList.size()){
            return funcList.get(index);
        }
        //Bao loi neu khong tim duoc function

        System.err.println("Index " + index + " out of bound of size " + funcList.size());
        System.exit(1);
        return null;
    }

    public IASTFunctionDefinition getFunction(String name) {
        String funcName = null;
        ArrayList<String> funcNameList = new ArrayList<>();
        ArrayList<IASTFunctionDefinition> funcList = getListFunction();
        for (IASTFunctionDefinition func : funcList) {
            funcName = func.getDeclarator().getName().toString();
            funcNameList.add(funcName);
            if (name.equals(funcName)) {
                return func;
            }
        }
        //Bao loi neu khong tim duoc function
        System.out.println("- Function list: ");
        for (String str : funcNameList) {
            System.out.println("   ." + str);
        }

        System.err.println("Cannot find function: " + name);
        System.exit(1);
        return null;
    }
    public IASTFunctionDefinition getMain() {
        return FunctionHelper.getFunction(this.getListFunction(), "main");
    }

    public void print() {
        IASTDeclaration[] iter = translationUnit.getDeclarations();
        for (IASTDeclaration i : iter) {
            printTree(i, 0);
        }
    }


}
