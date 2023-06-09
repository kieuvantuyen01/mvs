package com.mvs.cfg.build;

import com.mvs.cfg.node.*;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;

import java.util.ArrayList;


public class ControlFlowGraph {
    protected IASTFunctionDefinition func;
    protected CFGNode start;
    protected CFGNode exit;

    public ControlFlowGraph() {
    }

    public ControlFlowGraph(IASTFunctionDefinition def) {
        ControlFlowGraph cfg = build(def);
        start = cfg.getStart();
        exit = cfg.getExit();
        func = def;
    }

    public ControlFlowGraph(CFGNode start, CFGNode exit) {
        this.start = start;
        this.exit = exit;
    }

    public ControlFlowGraph(IASTFunctionDefinition def, ASTFactory ast) {
        ControlFlowGraph cfg = build(def, ast);
        if (cfg == null) {
            System.out.println("CFG is empty");
            System.exit(1);
        }
        start = cfg.getStart();
        exit = cfg.getExit();
        func = def;
    }

    public static void printGraph(CFGNode start) {
        print(start, 0);
    }

    private static void printSpace(int level) {
        for (int i = 0; i < level; i++) {
            System.out.print(" ");
        }
    }

    private static void print(CFGNode start, int level) {
        CFGNode iter = start;
        printSpace(level);
        if (iter == null) {
            //System.out.println(iter);
            return;
        } else if (iter instanceof DecisionNode) {
            iter.printNode();
            //System.out.println(iter.getFormula());
            printSpace(level);
            System.out.println("Then Clause: ");
            if (((DecisionNode) iter).getThenNode() != null) {
                print(((DecisionNode) iter).getThenNode(), level + 7);
            }
            //printSpace(level);
            System.out.println("Else Clause: ");
            if (((DecisionNode) iter).getElseNode() != null)
                print(((DecisionNode) iter).getElseNode(), level + 7);
        } else if (iter instanceof GotoNode) {
            iter.printNode();
            //printSpace(level);
            ((GotoNode) iter).getNext().printNode();
            //print(((GotoNode) iter).getNext(), level);
        } else if (iter instanceof IterationNode) {
            iter.printNode();
            if (iter.getNext() != null) print(iter.getNext(), level);
            else return;
        } else if (iter instanceof EmptyNode) {
            iter.printNode();
            print(iter.getNext(), level);
        } else if (iter instanceof EndConditionNode) {
            level -= 7;
        } else if (iter instanceof BeginNode) {
            iter.printNode();
            print(iter.getNext(), level);
            ((BeginNode) iter).getEndNode().printNode();
            print(((BeginNode) iter).getEndNode().getNext(), level);
        } else if (iter instanceof EndNode) {
            iter.printNode();
            print(iter.getNext(), level);
        } else { //BeginFunctionNode...
            iter.printNode();
            print(iter.getNext(), level);
        }
    }

    public String getNameFunction() {
        if (func == null) return null;
        return func.getDeclarator().getName().toString();
    }

    public void concat(ControlFlowGraph other) {
        if (start == null || exit == null) {
            start = other.start;
            exit = other.exit;
        } else {
            exit.setNext(other.start);
            exit = other.exit;
        }
    }

    public ControlFlowGraph build(IASTFunctionDefinition def) {
        return (new ControlFlowGraphBuilder()).build(def);
    }

    public ControlFlowGraph build(IASTFunctionDefinition def, ASTFactory ast) {
        MultiFunctionCFGBuilder multicfg = new MultiFunctionCFGBuilder(ast);
        return multicfg.build(def);
    }

    public CFGNode getStart() {
        return start;
    }

    public void setStart(CFGNode start) {
        this.start = start;
    }

    public CFGNode getExit() {
        return exit;
    }

    public void setExit(CFGNode node) {
        exit = node;
    }

    public void DFS() {
        DFSHelper(start);
    }

    private void DFSHelper(CFGNode node) {
        node.setVistited(true);
        //node.printNode();
        ArrayList<CFGNode> adj = node.adjacent();
        for (CFGNode iter : adj) {
            if (iter == null) return;
            if (!iter.isVistited()) {
                DFSHelper(iter);
            }
        }
    }

    /**
     * Dung de unfold graph
     *
     * @return
     */
    public void unfold() {
        UnfoldCFG unfoldCfg = new UnfoldCFG();
        unfoldCfg.generate(this);
    }

    public void unfold(int loopCount) {
        UnfoldCFG unfoldCfg = new UnfoldCFG();
        unfoldCfg.setLoop(loopCount);
        unfoldCfg.generate(this);
    }

    public void ungoto() {
        GotoCFG gotoCFG = new GotoCFG();
        gotoCFG.generate(this);
    }
    /*
    create java.invariant graph
     */
    public void invariant() {
        InvariantCFG invariantCFG = new InvariantCFG();
        invariantCFG.generate(this);
    }

    /**
     * Ham printGraph de in java.cfg
     */
    public void printGraph() {
        if (this != null)
            print(start, 0);
    }

    public void printFuncGraph() {
        printFunc(start, 0);
    }
    public void printBoundary() {
        System.out.print("StartNode: ");
        start.printNode();
        System.out.print("ExitNode: ");
        if (exit != null) {
            exit.printNode();
        } else System.out.println(exit);
    }

    private void printFunc(CFGNode start, int level) {
        CFGNode iter = start;
        printSpace(level);
        if (iter == null) {
            //System.out.println(iter);
            return;
        } else if (iter instanceof DecisionNode) {
            //System.out.println(iter.getFormula());
            printSpace(level);
            if (((DecisionNode) iter).getThenNode() != null) {
                printFunc(((DecisionNode) iter).getThenNode(), level);
            }
            //printSpace(level);
            if (((DecisionNode) iter).getElseNode() != null)
                printFunc(((DecisionNode) iter).getElseNode(), level);
        } else if (iter instanceof GotoNode) {
            printFunc(((GotoNode) iter).getNext(), level);
        } else if (iter instanceof IterationNode) {
            if (iter.getNext() != null) printFunc(iter.getNext(), level);
            else return;
        } else if (iter instanceof EmptyNode) {
            printFunc(iter.getNext(), level);
        } else if (iter instanceof EndConditionNode) {
            //level -= 1;
        } else if (iter instanceof BeginNode) {
            printFunc(iter.getNext(), level);
            printFunc(((BeginNode) iter).getEndNode().getNext(), level);
        } else if (iter instanceof BeginFunctionNode) {
            iter.printNode();
            //level += 1;
            printFunc(iter.getNext(), level);
        } else if (iter instanceof EndFunctionNode) {
            iter.printNode();
            printFunc(iter.getNext(), level);
        }  else {
            printFunc(iter.getNext(), level);
        }
    }

    public void printDebug() {
        printDebug(start);
    }

    private void printDebug(CFGNode start) {
        CFGNode iter = start;
        if (iter == null) return;
        else if (iter instanceof DecisionNode) {
            iter.printNode();
            System.out.print("      ");
            printDebug(((DecisionNode) iter).getThenNode());
            System.out.print("      ");
            printDebug(((DecisionNode) iter).getElseNode());
        } else if (iter instanceof EndConditionNode) {
            System.out.print("EndConditionNode");
            return;
        } else {
            if (iter != null) {
                iter.printNode();
                System.out.println(iter.getFormula());
                printDebug(iter.getNext());
            }
        }

    }

}
