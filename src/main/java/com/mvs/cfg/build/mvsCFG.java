package com.mvs.cfg.build;

import com.mvs.app.solver.SMTTypeConvertion;
import com.mvs.cfg.index.FormulaCreater;
import com.mvs.cfg.index.Var;
import com.mvs.cfg.index.VariableManage;
import com.mvs.cfg.node.*;
import com.mvs.cfg.utils.FunctionHelper;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class mvsCFG extends ControlFlowGraph {
    private VariableManage vm;
    List<Var> parameters;
    @SuppressWarnings("unused")
    private String returnType;

    public mvsCFG() {
        vm = new VariableManage();
    }

    public mvsCFG(IASTFunctionDefinition func) {
        super(func);
        this.func = func;
        vm = new VariableManage(func);
        returnType = getReturnType();
    }

    public mvsCFG(IASTFunctionDefinition func, ASTFactory ast) {
        super(func, ast);
        vm = FunctionHelper.getVM(ast.getListFunction());
    }

    public void printMetaSMT(PrintStream printStream) {
        printMeta(printStream, start, exit, " ");
    }

    private static void printMeta(PrintStream printStream, CFGNode node, CFGNode end, String nSpaces) {
        if (node == null) {
            return;
        } else if (node == end) {
            if (node.toString() != "") {
                printStream.println(nSpaces + node.toString());
            }
        } else if (node instanceof PlainNode) {
            if (node.toString() != null) {
                printStream.println(nSpaces + node.toString());
            }
            printMeta(printStream, node.getNext(), end, nSpaces);    // 4 spaces
        } else if (node instanceof SyncNode) {
            printStream.println(nSpaces + node.toString());
            printMeta(printStream, node.getNext(), end, nSpaces);
        } else if (node instanceof BeginNode) {
            BeginNode begin = (BeginNode) node;
            printMeta(printStream, begin.getNext(), end, nSpaces);
            printMeta(printStream, begin.getEndNode().getNext(), end, nSpaces);
        } else if (node instanceof DecisionNode) {
            DecisionNode cn = (DecisionNode) node;
            printStream.println(nSpaces + "if ( " + cn.toString() + " ) {");
            printMeta(printStream, cn.getThenNode(), cn.getEndNode(), nSpaces + "    ");    // 4 spaces
            printStream.println(nSpaces + "}");
            printStream.println(nSpaces + "else {");
            printMeta(printStream, cn.getElseNode(), cn.getEndNode(), nSpaces + "    ");    // 4 spaces
            printStream.println(nSpaces + "}");
            printMeta(printStream, cn.getEndNode(), end, nSpaces + "");    // 4 spaces
        } else if (node instanceof EndConditionNode) {
            return;
        } else {
            String constraint = node.toString();
            if (constraint != null && constraint != "") {
                printStream.println(nSpaces + constraint);
            }
            printMeta(printStream, node.getNext(), end, nSpaces);
        }
    }

    /**
     * @return kieu tra ve cua ham
     */
    private String getReturnType() {
        if (func != null) {
            IASTNode type = func.getDeclSpecifier();
            return type.toString();
        }
        return "unIdentify";
    }

    public VariableManage getVm() {
        return vm;
    }

    public void setVm(VariableManage vm) {
        this.vm = vm;
    }

    public IASTFunctionDefinition getFunc() {
        return func;
    }

    public void setFunc(IASTFunctionDefinition func) {
        this.func = func;
    }

    public void DFS() {
        DFSHelper(super.start);
    }

    public String createFormula() {
        return FormulaCreater.create(start, exit);
    }

    public List<String> createFormulas() {
        List<String> formulas = FormulaCreater.createAssert(start, exit);
        List<String> additionalFormulas = FormulaCreater.createAdditionalAssert(vm);
        List<String> advancedFormulas = FormulaCreater.createAdvancedAssert(vm);
        formulas.addAll(additionalFormulas);
        formulas.addAll(advancedFormulas);
        return formulas;
    }
    public String createInvariantFormula () {
        return FormulaCreater.createInvariantFormula(start, exit);
    }
    public String createInfixFormula() {
        return FormulaCreater.createInfix(start, exit);
    }

    public void printFormular(PrintStream ps) {
        ps.print(createFormula());
    }

    public String getNameFunction() {
        if (func == null) return null;
        return func.getDeclarator().getName().toString();
    }

    /**
     * @return tham so dau vao cua ham
     */
    public ArrayList<Var> getInitVariables() {
        if (this.func == null) return null;
        ArrayList<Var> varList = new ArrayList<>();
        //TODO this.func.get
        for (IASTNode iter : this.func.getDeclarator().getChildren()) {
            if (iter instanceof IASTParameterDeclaration) {
                String type = ((IASTParameterDeclaration) iter).getDeclSpecifier().toString();
                String name = ((IASTParameterDeclaration) iter).getDeclarator().getName().toString();
                Var var = new Var(type, name + "_" + FunctionHelper.getShortenName(this.getNameFunction()), -1, -1, -1);
                varList.add(var);
            }
        }
        return varList;
    }

    public Var getReturn() {
        return this.getVm().getVariable("return_" + FunctionHelper.getShortenName(getNameFunction()));
    }

    public String getTypeFunction() {
        if (func == null) return null;
        return this.func.getDeclSpecifier().toString();
    }

    public void index() {
        iteration(start);
    }
/*
    Note: Index java.invariant + 1, tuy nhien k index notConditionNode
 */
    private void iteration(CFGNode start) {
        CFGNode iter = start;
        if (iter == null) {
            return;
        } else if (iter instanceof DecisionNode) {
            iter.index(vm);
            iteration(((DecisionNode) iter).getEndNode());
        } else if (iter instanceof BeginNode) {
            iter.index(vm);
            iteration(iter.getNext());
            ((BeginNode) iter).getEndNode().index(vm);
            iteration(((BeginNode) iter).getEndNode().getNext());
        } else if (iter instanceof EndConditionNode) {
            iter.index(vm);
        } else if (iter instanceof InvariantNode) {
            iter.index(vm);
            iteration(iter.getNext());
        } else if (iter instanceof PlainNode) {
            iter.index(vm);
            iteration(iter.getNext());
        } else {
            if (iter instanceof BeginFunctionNode) {
                vm.increaseCurrentThreadIndex();
                vm.resetSsaIndex();
            } else if (iter instanceof EndFunctionNode) {
                if (!(iter.getNext() instanceof BeginFunctionNode)) {
                    vm.increaseCurrentThreadIndex();
                    vm.resetSsaIndex();
                }
            }
            iter.index(vm);
            iteration(iter.getNext());
        }

    }

    public List<Var> getParameters() {
        return parameters;
    }

    public void setParameters(List<Var> parameters) {
        this.parameters = parameters;
    }

    private void DFSHelper(CFGNode node) {
        node.setVistited(true);
        node.printNode();
        ArrayList<CFGNode> adj = node.adjacent();
        for (CFGNode iter : adj) {
            if (iter == null) return;
            if (!iter.isVistited()) {
                DFSHelper(iter);
            }
        }
    }

    public void printMeta() { //Da unfold
        printMeta(System.out, start, exit, " ");
    }

    public void printSMTFormula(PrintStream printStream) {
        int lastIndex;
        // (declare-fun a_0 () Int)
        for (Var var : vm.getVariableList()) {
            lastIndex = var.getSsaIndex();
            if (lastIndex == -3) {
                printStream.println("(declare-fun " + var.getName() +
                        " () " + SMTTypeConvertion.getSMTType(var.getType()) + ")");
            }
            for (int i = 0; i <= lastIndex; i++) {
                printStream.println("(declare-fun " + var.getName() + "_" + i +
                        " () " + SMTTypeConvertion.getSMTType(var.getType()) + ")");
            }

        }

//		if (!returnType.equals("void")) {
//			printStream.println("(declare-fun return () " +
//							SMTTypeConvertion.getSMTType(returnType)+ ")");
//		}

//		CFGNode node = start.getNext();
//		String f = null;
//
//		while (node != exit && node != null) {
//			f = node.getFormula();
//			if (f != null) {
//				printStream.println("(assert " + f + ")");
//			}
//			if (node instanceof DecisionNode) {
//				node = ((DecisionNode) node).getEndNode();
//			} else {
//				node = node.getNext();
//			}
//
//		}
        String f = FormulaCreater.create(start, exit);
        if (f != null) {
            printStream.println("(assert " + f + ")");
        }
    }

    public void va_printFormular(PrintStream printStream) {
        int lastIndex;
//		for (Var var: vm.getVariableList()) {
//			lastIndex = var.getIndex();
//			if (lastIndex == -3) {
//				printStream.println("(declare-fun " + var.getName() +
//						" () "+ SMTTypeConvertion.getSMTType(var.getType()) +")");
//			}
//			for (int i = 0; i <= lastIndex; i++) {
//				printStream.println("(declare-fun " + var.getName() + "_" + i +
//										" () "+ SMTTypeConvertion.getSMTType(var.getType()) +")");
//			}
//
//		}
        ArrayList<String> f = FormulaCreater.createListConstraint(start, exit);
        if (f != null) {
            //System.out.print(f.get(1));
            for (String f_child : f) {
                if (f_child.length() < 30)
                    printStream.println("(assert " + f_child + ")");
            }

        }
    }
}
