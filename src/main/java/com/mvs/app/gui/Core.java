package com.mvs.app.gui;

import com.mvs.app.solver.SMTInput;
import com.mvs.app.solver.Z3Runner;
import com.mvs.app.verification.userassertion.UserInput;
import com.mvs.cfg.build.ASTFactory;
import com.mvs.cfg.build.ControlFlowGraph;
import com.mvs.cfg.build.mvsCFG;
import com.mvs.cfg.index.Var;
import com.mvs.cfg.index.Variable;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Core {
    private String[] methodSignatures;
    private String filepath;
    private int nLoops = 4;
    int[] lineNumberOfMethods;
    static String SMTINPUT_DIR = "smt/";
    List<ControlFlowGraph> CFGList;
    ASTFactory ast;

    public String[] getMethodSignatures() {
        this.methodSignatures = ast.getFunctionSignatures().toArray(new String[0]);
        return methodSignatures;
    }
    public Core(){}

    public Core(String filepath) {
        this.filepath = filepath;
        ast = new ASTFactory(filepath);
    }


    public List<String> runSolver(int indexOfMethod,
                                  String userAssertion, String preCondition) throws Exception {

        //	int index = find(methodSignatures, methodSignature);

        mvsCFG cfg = new mvsCFG(ast.getFunction(indexOfMethod), ast);

        SMTInput smtInput;
        smtInput = new SMTInput(cfg.getVm().getVariableList(), cfg.createFormula());
        if (smtInput == null) {
            System.out.println("smtInput is null");
            System.exit(-1);
        }

        String constraintTemp;

        List<String> constraints = new ArrayList<>();
        UserInput userInput = new UserInput();
        ArrayList<Var> params = cfg.getInitVariables();
        params.add(new Var(cfg.getTypeFunction(), "return"));
        userInput.setParameter(params);

        // add pre-condition
        if (preCondition != null && !preCondition.equals("")) {
            constraintTemp = userInput.createUserAssertion(preCondition, cfg.getNameFunction(), cfg.getVm());
            constraints.add(constraintTemp);
        }

        // add user's assertion
        constraintTemp = userInput.createUserAssertion(userAssertion,cfg.getNameFunction(), cfg.getVm());
        constraintTemp = "(not " + constraintTemp + ")";
        constraints.add(constraintTemp);

        smtInput.setConstrainst(constraints);

        long end = System.currentTimeMillis();

        String functionName = cfg.getNameFunction();
        String path = SMTINPUT_DIR + functionName + ".smt";

        FileOutputStream fo = new FileOutputStream(new File(path));
        smtInput.printInputToOutputStream(fo);

        List<String> result = Z3Runner.runZ3(path);
        return result;
    }

    public List<String> analyseSolverResult(mvsCFG mf) {

        return null;
    }
    public List<String> getSolverLog() {
        List<String> res = new ArrayList<>();

        return res;
    }
    public int[] getLineNumberOfMethods() {
        if (lineNumberOfMethods == null) {
            System.err.println("null");
            System.exit(1);
        }
        return lineNumberOfMethods;
    }
    public Core setLoop(int nLoops) {
        //cfgBuilder.setNumberOfLoop(nLoops);
        return this;
    }
    public void create()
            throws FileNotFoundException {

        List<IASTFunctionDefinition> functionList = ast.getListFunction();
        if (functionList == null) {
            System.out.println("null");
        }

        int nMethods = functionList.size();
        methodSignatures = new String[nMethods];
        lineNumberOfMethods = new int[nMethods];
        CFGList = new ArrayList<>();

        SMTInput smtInput = new SMTInput();
        mvsCFG cfg = new mvsCFG();
        for(int i = 0; i < nMethods; i++) {
            methodSignatures[i] = functionList.get(i).getDeclarator().getName().toString();
            //lineNumberOfMethods[i] = functionList.get(i).getPosition().getLine();
            CFGList.add( cfg.build(functionList.get(i)) );
        }
    }

}
