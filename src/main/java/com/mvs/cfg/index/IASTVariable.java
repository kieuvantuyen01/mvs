package com.mvs.cfg.index;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;

public class IASTVariable {
    private IASTIdExpression var;
    private IASTDeclSpecifier type;

    public IASTVariable() {
    }

    public IASTVariable(IASTDeclSpecifier typeVar, IASTIdExpression varId) {
        type = typeVar;
        var = varId;
    }

    public IASTIdExpression getVar() {
        return var;
    }

    public IASTDeclSpecifier getType() {
        return type;
    }

    public void setType(IASTDeclSpecifier type) {
        this.type = type;
    }

    public IASTName getName() {
        return var.getName();
    }

    public void setName(IASTIdExpression name) {
        this.var = name;
    }

}
