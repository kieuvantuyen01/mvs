package com.mvs.cfg.index;

import com.mvs.cfg.build.ASTFactory;
import com.mvs.cfg.build.mvsCFG;
import com.mvs.cfg.node.CFGNode;
import com.mvs.cfg.node.DecisionNode;
import com.mvs.cfg.utils.ExpressionHelper;
import org.eclipse.cdt.core.dom.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author va
 * Chưa test
 */
public class FormulaCreater {
    public static String LOGIC_AND = "and";
    public static String LOGIC_OR = "or";
    public static String NEGATIVE = "not";
    public static String BINARY_CONNECTIVE = "=>";
    public static String EQUALITY = "=";


    public static String create(CFGNode start, CFGNode exit) {
        String constraint = start.getFormula();

        String temp;
        CFGNode node = start.getNext();
        while (node != null) {
            temp = node.getFormula();
            if (temp != null) {
                if (constraint == null) {
                    constraint = temp;
                } else {
//					constraint = wrapInfix(LOGIC_AND, temp, constraint);
                    constraint = wrapPrefix(LOGIC_AND, constraint, temp);
                }
            }
            if (node == exit) break;
            if (node instanceof DecisionNode) {
                node = ((DecisionNode) node).getEndNode();
            } else {
                node = node.getNext();
            }

        }

        return constraint;
    }

    public static List<String> createAssert(CFGNode start, CFGNode exit) {
        List<String> constraint = new ArrayList<>();
        if (start.getFormula() != null) {
            constraint.add(start.getFormula());
        }

        String temp;
        CFGNode node = start.getNext();
        while (node != null) {
            temp = node.getFormula();
            if (temp != null) {
                if (constraint == null) {
                    constraint.add(temp);
                } else {
//					constraint = wrapInfix(LOGIC_AND, temp, constraint);
                    //constraint = wrapPrefix(LOGIC_AND, constraint, temp);
                    constraint.add(temp);
                }
            }
            if (node == exit) break;
            if (node instanceof DecisionNode) {
                node = ((DecisionNode) node).getEndNode();
            } else {
                node = node.getNext();
            }

        }

        return constraint;
    }

    //get list of read variables in the same thread
    public static ArrayList<Var> getReadVarListInSameThread(VariableManage vm, int threadIndex, String name) {
        ArrayList<Var> readVarList = new ArrayList<>();
        ArrayList<Var> inputList = vm.getInputList();
        for (Var var : inputList) {
            if (var.getName() == name && var.getWrIndex() == 1 && var.getThreadIndex() == threadIndex) {
                readVarList.add(var);
            }
        }
        return readVarList;
    }

    //get list of write variables in the same thread
    public static ArrayList<Var> getWriteVarListInSameThread(VariableManage vm, int threadIndex, String name) {
        ArrayList<Var> writeVarList = new ArrayList<>();
        ArrayList<Var> inputList = vm.getInputList();
        for (Var var : inputList) {
            if (var.getName() == name && var.getWrIndex() == 0 && var.getThreadIndex() == threadIndex) {
                writeVarList.add(var);
            }
        }
        return writeVarList;
    }

    static boolean isVarInFunction(int threadNum, Var var) {
        return (var.getThreadIndex() >= 1 && var.getThreadIndex() <= threadNum);
    }

    public static List<String> createAdvancedAssert(VariableManage vm) {
        List<String> constraint = new ArrayList<>();
        //get list of Input list tu vm
        ArrayList<Var> inputList = vm.getInputList();

        String operand = "=";

        //possible value assignments for read variables
        int threadNum = vm.getCurrentThreadIndex() - 2;
        //iterative traversal to get list of variables by thread
        for (Var readVar : inputList) {
            if (isVarInFunction(threadNum, readVar) && readVar.getWrIndex() == 1) {
                String varName = readVar.getName();
                ArrayList<Var> writeListInSameThread = getWriteVarListInSameThread(vm, readVar.getThreadIndex(), varName);
                ArrayList<Var> readListInSameThread = getReadVarListInSameThread(vm, readVar.getThreadIndex(), varName);
                //iterative list of write variables
                for (Var writeVar : inputList) {
                    //check if 2 variables are in the same name
                    if (isVarInFunction(threadNum, writeVar) && writeVar.getName() == readVar.getName() && writeVar.getThreadIndex() != readVar.getThreadIndex()) {
                        if (writeVar.getWrIndex() == 0) {
                            String left = readVar.getVariableWithIndex();
                            String right = writeVar.getVariableWithIndex();
                            String constraintTemp = wrapPrefix(operand, left, right);
                            String constraintTemp4 = "";

                            //iterative list of read variables in readListInSameThread
                            for (Var readVar2 : readListInSameThread) {
                                //check if readVar2 has greater ssaIndex than readVar
                                if (readVar2.getSsaIndex() > readVar.getSsaIndex()) {
                                    String constraintTemp2 = "";
                                    //iterative list of write variables in writeListInSameThread
                                    for (Var writeVar2 : writeListInSameThread) {
                                        //check if writeVar2 has less ssaIndex than writeVar
                                        if (writeVar2.getSsaIndex() < writeVar.getSsaIndex()) {
                                            String left2 = readVar2.getVariableWithIndex();
                                            String right2 = writeVar2.getVariableWithIndex();
                                            String constraintTemp3 = wrapInfix(operand, left2, right2);
                                            if (constraintTemp2.isEmpty()) {
                                                constraintTemp2 = constraintTemp3;
                                            } else {
                                                constraintTemp2 = wrapPrefix(LOGIC_OR, constraintTemp2, constraintTemp3);
                                            }
                                        }
                                    }
                                    //iterative list of write variables in other threads
                                    for (Var writeVar2 : inputList) {
                                        //check if writeVar2 is in other threads with readVar2 and check if writeVar2 has same name with readVar2
                                        if (isVarInFunction(threadNum, writeVar2) && writeVar2.getThreadIndex() != readVar2.getThreadIndex() && writeVar2.getName() == readVar2.getName()) {
                                            //check if writeVar2 is write variable
                                            if (writeVar2.getWrIndex() == 0) {
                                                //check if writeVar2 has same thread index with writeVar
                                                if (writeVar2.getThreadIndex() == writeVar.getThreadIndex()) {
                                                    //check if writeVar2 has ssaIndex greater than or equal to writeVar
                                                    if (writeVar2.getSsaIndex() >= writeVar.getSsaIndex()) {
                                                        String left2 = readVar2.getVariableWithIndex();
                                                        String right2 = writeVar2.getVariableWithIndex();
                                                        String constraintTemp3 = wrapPrefix(operand, left2, right2);
                                                        if (constraintTemp2.isEmpty()) {
                                                            constraintTemp2 = constraintTemp3;
                                                        } else {
                                                            constraintTemp2 = wrapPrefix(LOGIC_OR, constraintTemp2, constraintTemp3);
                                                        }
                                                    }
                                                } else {
                                                    String left2 = readVar2.getVariableWithIndex();
                                                    String right2 = writeVar2.getVariableWithIndex();
                                                    String constraintTemp3 = wrapPrefix(operand, left2, right2);
                                                    if (constraintTemp2.isEmpty()) {
                                                        constraintTemp2 = constraintTemp3;
                                                    } else {
                                                        constraintTemp2 = wrapPrefix(LOGIC_OR, constraintTemp2, constraintTemp3);
                                                    }
                                                }

                                            }
                                        }
                                    }
                                    if (constraintTemp4.isEmpty()) {
                                        constraintTemp4 = constraintTemp2;
                                    } else {
                                        constraintTemp4 = wrapPrefix(LOGIC_AND, constraintTemp4, constraintTemp2);
                                    }
                                }
                            }
                            if (!constraintTemp4.isEmpty()) {
                                String constraintTemp5 = wrapPrefix(BINARY_CONNECTIVE, constraintTemp, constraintTemp4);
                                constraint.add(constraintTemp5);
                            }
                        }
                    }

                }
            }
        }
        return constraint;
    }

    public static String createInvariantFormula(CFGNode start, CFGNode exit) {
        CFGNode node = start;
        String constraint = node.getFormula();
        String temp;

        while (node != null) {
            if (!(node instanceof DecisionNode)) {
                temp = node.getFormula();
                if (temp != null) {
                    if (constraint == null) {
                        constraint = temp;
                    } else {
                        constraint = wrapPrefix(LOGIC_AND, constraint, temp);
                    }
                }
            }
            if (node == exit) break;
            if (node instanceof DecisionNode) {
                node = ((DecisionNode) node).getThenNode();
            } else {
                node = node.getNext();
            }
        }

        return constraint;
    }

    public static ArrayList<String> createListConstraint(CFGNode start, CFGNode exit) {
        //String constraint = start.getFormula();
        ArrayList<String> constraintList = new ArrayList<>();
        String temp = null;
        ArrayList<String> dectemps;
        CFGNode node = start.getNext();
        while (node != null) {
            if (node instanceof DecisionNode) {
                dectemps = ((DecisionNode) node).va_getFormula();
                for (String dectemp : dectemps) {
                    constraintList.add(dectemp);
                }
            } else temp = node.getFormula();
            if (temp != null) {
                constraintList.add(temp);
//					constraint = wrapInfix(LOGIC_AND, temp, constraint);
//					constraint = wrapPrefix(LOGIC_AND, constraint, temp);

            }
            if (node == exit) break;
            if (node instanceof DecisionNode) {
                node = ((DecisionNode) node).getEndNode();
            } else {
                node = node.getNext();
            }

        }

        return constraintList;

    }

    public static String createInfix(CFGNode start, CFGNode exit) {
        String constraint = start.getInfixFormula();

        String temp;
        CFGNode node = start.getNext();
        while (node != null) {

            temp = node.getInfixFormula();
            if (temp != null) {
                if (constraint == null) {
                    constraint = temp;
                } else {
                    constraint = wrapInfix(LOGIC_AND, temp, constraint);
//					constraint = wrapPrefix(LOGIC_AND, constraint, temp);
                }
            }
            if (node == exit) break;
            if (node instanceof DecisionNode) {
                node = ((DecisionNode) node).getEndNode();

            } else {
                node = node.getNext();
            }

        }

        return constraint;
    }

    public static String createFormula(IASTNode node) {
        if (node instanceof IASTDeclarationStatement) {
            return prefixDeclarationStatement((IASTDeclarationStatement) node);
        } else if (node instanceof IASTExpressionStatement) { //cau lenh gan va so sanh
            return prefixExpressionStatement((IASTExpressionStatement) node);
        } else if (node instanceof IASTBinaryExpression) { //phep gan va so sanh
            return prefixBinaryExpression((IASTBinaryExpression) node);
        } else if (node instanceof IASTIdExpression) { //bien (khong tinh trong phep khoi tao)
            return ExpressionHelper.toString(node);
        } else if (node instanceof IASTUnaryExpression) {
            return prefixBinaryExpression((IASTBinaryExpression) ((IASTUnaryExpression) node).getOperand());
        } else if (node instanceof IASTLiteralExpression) {
            return ExpressionHelper.toString(node);
//		} else if (node instanceof IASTReturnStatement){
//			return prefixReturnStatement((IASTReturnStatement) node); //da xu ly return o ReturnNode
        }
        return node.getClass().getSimpleName();
    }

    public static String createInfixFormula(IASTNode node) {
        if (node instanceof IASTDeclarationStatement) {
            return infixDeclarationStatement((IASTDeclarationStatement) node);
        } else if (node instanceof IASTExpressionStatement) { //cau lenh gan va so sanh
            return infixExpressionStatement((IASTExpressionStatement) node);
        } else if (node instanceof IASTBinaryExpression) { //phep gan va so sanh
            return infixBinaryExpression((IASTBinaryExpression) node);
        } else if (node instanceof IASTIdExpression) { //bien (khong tinh trong phep khoi tao)
            return ExpressionHelper.toString(node);
        } else if (node instanceof IASTLiteralExpression) {
            return ExpressionHelper.toString(node);
        }
        return null;
    }

    private static String prefixDeclarationStatement(IASTDeclarationStatement node) {
        boolean isAssign = false;
        IASTNode[] nodes2 = null;
        IASTDeclaration declaration = node.getDeclaration();
        IASTNode[] nodes1 = declaration.getChildren();
        String left = null;
        String right = null;
        //String type = null;
        String formula = null;
        for (IASTNode iter1 : nodes1) {
//			if (iter1 instanceof IASTSimpleDeclSpecifier) {
//				type = iter1.toString();
//			}
            if (iter1 instanceof IASTDeclarator) {
                nodes2 = iter1.getChildren();
                //kiem tra co la phep gan?
                for (IASTNode iter2 : nodes2) {
                    if (iter2 instanceof IASTEqualsInitializer) {
                        isAssign = true;
                    }
                }
                if (isAssign) {
                    for (IASTNode iter : nodes2) {
                        if (iter instanceof IASTName) {
                            left = ((IASTName) iter).toString();
                        }
                        if (iter instanceof IASTEqualsInitializer) {
                            right = createFormula(iter.getChildren()[0]);
                        }
                    }
                    if (formula == null) {
                        formula = wrapPrefix("=", left, right);
                    } else {
                        formula = wrapPrefix(LOGIC_AND, formula, wrapPrefix("=", left, right));
                    }
                }
            }
        }

        return formula;
    }

    private static String infixDeclarationStatement(IASTDeclarationStatement node) {
//		node.getDeclaration()
        boolean isAssign = false;
        IASTNode[] nodes2 = null;
        IASTDeclaration declaration = node.getDeclaration();
        IASTNode[] nodes1 = declaration.getChildren();

        for (IASTNode iter1 : nodes1) {
            if (iter1 instanceof IASTDeclarator) {
                nodes2 = iter1.getChildren();
                //kiem tra co la phep gan?
                for (IASTNode iter2 : nodes2) {
                    if (iter2 instanceof IASTEqualsInitializer) {
                        isAssign = true;
                    }
                }

            }
        }
        if (isAssign) {
            String left = null;
            String right = null;
            for (IASTNode iter : nodes2) {
                if (iter instanceof IASTName) {
                    left = ((IASTName) iter).toString();
                }
                if (iter instanceof IASTEqualsInitializer) {
                    right = createInfixFormula(iter.getChildren()[0]);
                }
            }
            return wrapInfix("=", left, right);
        }
        return null;

    }

    private static String prefixExpressionStatement(IASTExpressionStatement node) {
        return createFormula(node.getExpression());
    }

    private static String prefixBinaryExpression(IASTBinaryExpression node) {
        IASTExpression op1 = node.getOperand1();
        IASTExpression op2 = node.getOperand2();
        String operand = ExpressionHelper.getCorrespondBinaryOperator(node.getOperator());
        String opStr1 = createFormula(op1);
        String opStr2 = createFormula(op2);
        return wrapPrefix(operand, opStr1, opStr2);
    }

    private static String infixBinaryExpression(IASTBinaryExpression node) {
        IASTExpression op1 = node.getOperand1();
        IASTExpression op2 = node.getOperand2();
        String operand = ExpressionHelper.getCorrespondBinaryOperator(node.getOperator());
        String opStr1 = createInfixFormula(op1);
        String opStr2 = createInfixFormula(op2);
        return wrapInfix(operand, opStr1, opStr2);
    }

    private static String infixExpressionStatement(IASTExpressionStatement node) {
        return createInfixFormula(node.getExpression());
    }

    public static String createFormula(String operator, String operand) {
        return "(" + operator + " " + operand + ")";
    }

    public static String createInfixFormula(String operator, String operand) {
        return "(" + operator + " " + operand + ")";
    }

    public static String wrapInfix(String operand, String left, String right) {
        return "(" + left + " " + operand + " " + right + ")";
    }

    public static String wrapPrefix(String operand, String left, String right) {
        return "(" + operand + " " + left + " " + right + ")";
    }

    public static void main(String[] args) {
        ASTFactory ast = new ASTFactory("./TestInput.c");
        IASTFunctionDefinition func = ast.getFunction(0);
        mvsCFG cfg = new mvsCFG(func);
        cfg.index();
        cfg.unfold();
        System.out.println(create(cfg.getStart(), cfg.getExit()));
    }

    public static String createFuncCallFormula(IASTFunctionCallExpression funcCall, ASTFactory ast) {

        return null;
    }
}