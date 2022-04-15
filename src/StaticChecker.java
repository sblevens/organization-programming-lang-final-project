/*
 * File: StaticChecker.java
 * Date: Spring 2022
 * Auth: Sami Blevens
 * Desc: Static Checker for HW5
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


// NOTE: Some of the following are filled in, some partly filled in,
// and most left for you to fill in. The helper functions are provided
// for you to use as needed. 


public class StaticChecker implements Visitor {

  // the symbol table
  private SymbolTable symbolTable = new SymbolTable();
  // the current expression type
  private String currType = null;
  // the program's user-defined (record) types and function signatures
  private TypeInfo typeInfo = null;

  //--------------------------------------------------------------------
  // helper functions:
  //--------------------------------------------------------------------
  
  // generate an error
  private void error(String msg, Token token) throws MyPLException {
    String s = msg;
    if (token != null)
      s += " near line " + token.line() + ", column " + token.column();
    throw MyPLException.StaticError(s);
  }

  // return all valid types
  // assumes user-defined types already added to symbol table
  private List<String> getValidTypes() {
    List<String> types = new ArrayList<>();
    types.addAll(Arrays.asList("int", "double", "bool", "char", "string",
                               "void"));
    for (String type : typeInfo.types())
      if (symbolTable.get(type).equals("type"))
        types.add(type);
    return types;
  }

  // return the build in function names
  private List<String> getBuiltinFunctions() {
    return Arrays.asList("print", "read", "length", "get", "stoi",
                         "stod", "itos", "itod", "dtos", "dtoi");
  }
  
  // check if given token is a valid function signature return type
  private void checkReturnType(Token typeToken) throws MyPLException {
    if (!getValidTypes().contains(typeToken.lexeme())) {
      String msg = "'" + typeToken.lexeme() + "' is an invalid return type";
      error(msg, typeToken);
    }
  }

  // helper to check if the given token is a valid parameter type
  private void checkParamType(Token typeToken) throws MyPLException {
    if (typeToken.equals("void"))
      error("'void' is an invalid parameter type", typeToken);
    else if (!getValidTypes().contains(typeToken.lexeme())) {
      String msg = "'" + typeToken.lexeme() + "' is an invalid return type";
      error(msg, typeToken);
    }
  }

  
  // helpers to get first token from an expression for calls to error
  
  private Token getFirstToken(Expr expr) {
    return getFirstToken(expr.first);
  }

  private Token getFirstToken(ExprTerm term) {
    if (term instanceof SimpleTerm)
      return getFirstToken(((SimpleTerm)term).rvalue);
    else
      return getFirstToken(((ComplexTerm)term).expr);
  }

  private Token getFirstToken(RValue rvalue) {
    if (rvalue instanceof SimpleRValue)
      return ((SimpleRValue)rvalue).value;
    else if (rvalue instanceof NewRValue)
      return ((NewRValue)rvalue).typeName;
    else if (rvalue instanceof IDRValue)
      return ((IDRValue)rvalue).path.get(0);
    else if (rvalue instanceof CallExpr)
      return ((CallExpr)rvalue).funName;
    else 
      return getFirstToken(((NegatedRValue)rvalue).expr);
  }

  
  //---------------------------------------------------------------------
  // constructor
  //--------------------------------------------------------------------
  
  public StaticChecker(TypeInfo typeInfo) {
    this.typeInfo = typeInfo;
  }
  

  //--------------------------------------------------------------------
  // top-level nodes
  //--------------------------------------------------------------------
  
  public void visit(Program node) throws MyPLException {
    // push the "global" environment
    symbolTable.pushEnvironment();

    // (1) add each user-defined type name to the symbol table and to
    // the list of rec types, check for duplicate names
    for (TypeDecl tdecl : node.tdecls) {
      String t = tdecl.typeName.lexeme();
      if (symbolTable.nameExists(t))
        error("type '" + t + "' already defined", tdecl.typeName);
      // add as a record type to the symbol table
      symbolTable.add(t, "type");
      // add initial type info (rest added by TypeDecl visit function)
      typeInfo.add(t);
    }
    
    // TODO: (2) add each function name and signature to the symbol
    // table check for duplicate names
    for (FunDecl fdecl : node.fdecls) {
      String funName = fdecl.funName.lexeme();
      // make sure not redefining built-in functions
      if (getBuiltinFunctions().contains(funName)) {
        String m = "cannot redefine built in function " + funName;
        error(m, fdecl.funName);
      }
      // check if function already exists
      if (symbolTable.nameExists(funName))
        error("function '" + funName + "' already defined", fdecl.funName);

      // make sure the return type is a valid type
      checkReturnType(fdecl.returnType);
      // add to the symbol table as a function
      symbolTable.add(funName, "fun");
      // add to typeInfo
      typeInfo.add(funName);

      // TODO: add each formal parameter as a component type
      // ...
      for (FunParam fparam : fdecl.params){
        //check if duplicate
        if(typeInfo.get(funName,fparam.paramName.lexeme()) != null){
          error("cannot have duplicate parameter names in function " + funName, fdecl.funName);
        }
        //check if param type is valid
        checkParamType(fparam.paramType);

        typeInfo.add(funName, fparam.paramName.lexeme(), fparam.paramType.lexeme());
      }

      // add the return type
      typeInfo.add(funName, "return", fdecl.returnType.lexeme());
    }

    // TODO: (3) ensure "void main()" defined and it has correct
    // signature
    // ...
    // not sure what token to send into the error messages
    Token mainError = new Token(null,currType,0,0);
    if(symbolTable.nameExists("main")){
      //check if void
      String s = typeInfo.get("main","return");
      if(!s.equals("void")){
        error("must define void main()",mainError);
      }

      List<String> params = new ArrayList<>(typeInfo.components("main"));
      if(params.size() > 1){
        error("main must not have params",mainError);
      }

    } else {
      error("must define main() function",mainError);
    }
    
    // check each type and function
    for (TypeDecl tdecl : node.tdecls) 
      tdecl.accept(this);
    for (FunDecl fdecl : node.fdecls) 
      fdecl.accept(this);

    // all done, pop the global table
    symbolTable.popEnvironment();
  }
  

  public void visit(TypeDecl node) throws MyPLException {
    symbolTable.pushEnvironment();
    for(VarDeclStmt vdecl: node.vdecls){
      vdecl.accept(this);
      typeInfo.add(node.typeName.lexeme(), vdecl.varName.lexeme(), currType);
    }
    symbolTable.popEnvironment();
  }

  
  public void visit(FunDecl node) throws MyPLException {
    symbolTable.pushEnvironment();

    for(FunParam p : node.params){
      symbolTable.add(p.paramName.lexeme(),p.paramType.lexeme());
    }

    boolean returned = false;
    for(Stmt s: node.stmts){
      if(s instanceof ReturnStmt){
        returned = true;
      }
      s.accept(this);
    }

    String returnType = typeInfo.get(node.funName.lexeme(), "return");

    if(returned){
      if(!returnType.equals(currType) && !currType.equals("void")){
        error("expecting " + returnType + " return type", node.funName);
      }
    }

    symbolTable.popEnvironment();
  }


  //--------------------------------------------------------------------
  // statement nodes
  //--------------------------------------------------------------------
  
  public void visit(VarDeclStmt node) throws MyPLException {
    node.expr.accept(this);
    String expType = currType;
    String varName = node.varName.lexeme();

    if(!expType.equals("int") && !expType.equals("double") && !expType.equals("char") && !expType.equals("string") && !expType.equals("bool") && !expType.equals("void") && (symbolTable.get(expType) == null || !symbolTable.get(expType).equals("type"))){
      if(node.expr.first instanceof SimpleTerm){
        SimpleTerm s = (SimpleTerm)node.expr.first;
        if(s.rvalue instanceof IDRValue){
          IDRValue i = (IDRValue)s.rvalue;
          if(i.path.size() == 1){
            error("expecting new with UDT",getFirstToken(node.expr));
          }
        } else if(!(s.rvalue instanceof NewRValue)){
          error("expecting variable or new",getFirstToken(node.expr));
        }
      }
      else {
        String m = "expecting variable";
        error(m,getFirstToken(node.expr));
      }
    }

    if(symbolTable.nameExistsInCurrEnv(varName)){
      error("no variable shadowing", node.varName);
    }

    if(node.typeName != null){
      //explicit
      checkParamType(node.typeName);
      if(!node.typeName.lexeme().equals(expType) && !expType.equals("void")){
        error("expected "+ node.typeName.lexeme() + ", found "+ expType,node.typeName);
      }
    } else {
      //implicit
      if(expType.equals("void")){
        error("bad implicit var declaration", node.varName);
      }
    }
    
    if(node.typeName != null){
      currType = node.typeName.lexeme();
      symbolTable.add(node.varName.lexeme(),currType);
    } else
      symbolTable.add(node.varName.lexeme(),expType);

  }
  

  public void visit(AssignStmt node) throws MyPLException {

    // TODO
    node.expr.accept(this);
    String rhsType = currType;

    String varName = node.lvalue.get(0).lexeme();

    if(!symbolTable.nameExists(varName)){
      String m = varName + " is not defined ";
      error(m,node.lvalue.get(0));
    }

    String lhsType = symbolTable.get(varName);
    
    //check for more complex paths
    if(node.lvalue.size() > 1){
      String prevType = lhsType;
      for(Token t: node.lvalue){
        String type = symbolTable.get(t.lexeme());
        
        if(type == null){
          //not init type - part of the path
          lhsType = typeInfo.get(prevType,t.lexeme());
          if(lhsType == null){
            String m = prevType + " does not have field "+ t.lexeme();
            error(m,t);
          }
          prevType = lhsType;
        } else {
          //init type
          lhsType = type;
          prevType = type;
        }
      }
    }

    if(!rhsType.equals("void") && !lhsType.equals(rhsType)){
      String m = "expecting " + lhsType + ", found " + rhsType;
      error(m, getFirstToken(node.expr));
    }
  }
  
  
  public void visit(CondStmt node) throws MyPLException {
    //if
    symbolTable.pushEnvironment();
    node.ifPart.cond.accept(this);
    if(!currType.equals("bool")){
      String m = "expression must be bool";
      error(m, getFirstToken(node.ifPart.cond));
    }
    for(Stmt s: node.ifPart.stmts){
      s.accept(this);
    }
    symbolTable.popEnvironment();
    //elifs
    if(node.elifs.size() > 0){
      for(BasicIf b: node.elifs){
        symbolTable.pushEnvironment();
        b.cond.accept(this);
        if(!currType.equals("bool")){
          String m = "expression must be bool";
          error(m, getFirstToken(b.cond));
        }
        for(Stmt s: b.stmts){
          s.accept(this);
        }
        symbolTable.popEnvironment();
      }
    }
    //else
    if(node.elseStmts != null && node.elseStmts.size() > 0){
      symbolTable.pushEnvironment();
      for(Stmt s: node.elseStmts){
        s.accept(this);
      }
      symbolTable.popEnvironment();
    }
  }
  

  public void visit(WhileStmt node) throws MyPLException {
    symbolTable.pushEnvironment();
    node.cond.accept(this);
    if(!currType.equals("bool")){
      String m = "expression must be bool";
      error(m, getFirstToken(node.cond));
    }
    for(Stmt s: node.stmts){
      s.accept(this);
    }
    symbolTable.popEnvironment();
  }
  

  public void visit(ForStmt node) throws MyPLException {
    symbolTable.pushEnvironment();               
    symbolTable.add(node.varName.lexeme(),"int");

    node.start.accept(this);
    String start = currType;
    node.end.accept(this);
    String end = currType;
    if(!start.equals("int")){
      String m = "expecting int for start, found " +start;
      error(m,node.varName);
    }
    if(!end.equals("int")){
      String m = "expecting int for end, found " +end;
      error(m,node.varName);
    }

    for(Stmt s: node.stmts){
      s.accept(this);
    }
    
    symbolTable.popEnvironment();
  }
  
  
  public void visit(ReturnStmt node) throws MyPLException {
    if(node.expr != null){
      node.expr.accept(this);
    } else {
      currType = "void";
    }
  }
  
  public void visit(DeleteStmt node) throws MyPLException {
    if(!symbolTable.nameExists(node.varName.lexeme())){
      String m = "varible must exist before deletion";
      error(m,node.varName);
    }
    //check if primitive - no deletion of primitive chars
    String type = symbolTable.get(node.varName.lexeme());
    //"int", "double", "bool", "char", "string"
    if(type.equals("int") || type.equals("double") || type.equals("char") || type.equals("string")){
      String m = "cannot delete a primitive type";
      error(m,node.varName);
    }
    //check if function
    if(type.equals("fun")){
      String m = "cannot delete a function type";
      error(m, node.varName);
    }
  }
  

  //----------------------------------------------------------------------
  // statement and rvalue node
  //----------------------------------------------------------------------

  private void checkBuiltIn(CallExpr node) throws MyPLException {
    String funName = node.funName.lexeme();
    if (funName.equals("print")) {
      node.args.get(0).accept(this);
      // has to have one argument, any type is allowed
      if (node.args.size() != 1)
        error("print expects one argument", node.funName);
      currType = "void";
    }
    else if (funName.equals("read")) {
      // no arguments allowed
      if (node.args.size() != 0)
        error("read takes no arguments", node.funName);
      currType = "string";
    }
    else if (funName.equals("length")) {
      // one string argument
      if (node.args.size() != 1)
        error("length expects one argument", node.funName);
      Expr e = node.args.get(0);
      e.accept(this);
      if (!currType.equals("string"))
        error("expecting string in length", getFirstToken(e));
      currType = "int";
    }
    else if (funName.equals("get")) {
      if (node.args.size() != 2)
        error("get expects two arguments", node.funName);
      Expr e1 = node.args.get(0);
      Expr e2 = node.args.get(1);
      e1.accept(this);
      if (!currType.equals("int"))
        error("expecting int in get", getFirstToken(e1));
      e2.accept(this);
      if(!currType.equals("string"))
        error("expecting string in get", getFirstToken(e2));
      currType = "char";
    }
    else if (funName.equals("stoi")) {
      if (node.args.size() != 1)
        error("stoi expects one argument", node.funName);
      Expr e = node.args.get(0);
      e.accept(this);
      if (!currType.equals("string"))
        error("expecting string in stoi", getFirstToken(e));
      currType = "int";
    }
    else if (funName.equals("stod")) {
      if (node.args.size() != 1)
        error("stod expects one argument", node.funName);
      Expr e = node.args.get(0);
      e.accept(this);
      if (!currType.equals("string"))
        error("expecting string in stod", getFirstToken(e));
      currType = "double";
    }
    else if (funName.equals("itos")) {
      if (node.args.size() != 1)
        error("itos expects one argument", node.funName);
      Expr e = node.args.get(0);
      e.accept(this);
      if (!currType.equals("int"))
        error("expecting int in itos", getFirstToken(e));
      currType = "string";
    }
    else if (funName.equals("itod")) {
      if (node.args.size() != 1)
        error("itod expects one argument", node.funName);
      Expr e = node.args.get(0);
      e.accept(this);
      if (!currType.equals("int"))
        error("expecting int in itod", getFirstToken(e));
      currType = "double";
    }
    else if (funName.equals("dtos")) {
      if (node.args.size() != 1)
        error("dtos expects one argument", node.funName);
      Expr e = node.args.get(0);
      e.accept(this);
      if (!currType.equals("double"))
        error("expecting double in dtos", getFirstToken(e));
      currType = "string";
    }
    else if (funName.equals("dtoi")) {
      if (node.args.size() != 1)
        error("dtoi expects one argument", node.funName);
      Expr e = node.args.get(0);
      e.accept(this);
      if (!currType.equals("double"))
        error("expecting double in dtoi", getFirstToken(e));
      currType = "int";
    }
  }

  
  public void visit(CallExpr node) throws MyPLException {
    if (getBuiltinFunctions().contains(node.funName.lexeme())) {
      checkBuiltIn(node);
    } else {
      String funName = node.funName.lexeme();
      if(!symbolTable.nameExists(funName)){
        error(funName + "does not exist",node.funName);
      }
      if(!symbolTable.get(funName).equals("fun")){
        error(funName + "is not defined as a function", node.funName);
      }

      List<String> params = new ArrayList<>(typeInfo.components(funName));
      if(params.size()-1 != node.args.size()){
        error("incorrect number of params",getFirstToken(node.args.get(0)));
      }

      for(int i=0; i<params.size() -1; ++i){
        String paramName = params.get(i);
        String paramType = typeInfo.get(funName, paramName);
        
        node.args.get(i).accept(this);
        String argsType = currType;

        if(!paramType.equals(argsType) && !argsType.equals("void")){
          String m = "expecting "+ paramType + " as arg, found " + argsType;
          error(m,getFirstToken(node.args.get(i)));
        }
      } 

      currType = typeInfo.get(funName,"return");
    }
    
  }
  

  //----------------------------------------------------------------------
  // rvalue nodes
  //----------------------------------------------------------------------
  
  public void visit(SimpleRValue node) throws MyPLException {
    TokenType tokenType = node.value.type();
    if (tokenType == TokenType.INT_VAL)
      currType = "int";
    else if (tokenType == TokenType.DOUBLE_VAL)
      currType = "double";
    else if (tokenType == TokenType.BOOL_VAL)
      currType = "bool";
    else if (tokenType == TokenType.CHAR_VAL)    
      currType = "char";
    else if (tokenType == TokenType.STRING_VAL)
      currType = "string";
    else if (tokenType == TokenType.NIL)
      currType = "void";
  }
  
    
  public void visit(NewRValue node) throws MyPLException {
    String typeName = node.typeName.lexeme();

    if(symbolTable.get(typeName) == null){
      error(typeName + "has not been defined",node.typeName);
    }

    if(!symbolTable.get(typeName).equals("type")){
      error(typeName + " is not a user defined variable",node.typeName);
    }

    currType = typeName;
  }
  
      
  public void visit(IDRValue node) throws MyPLException {
    String prevType = "";
    for(Token t: node.path){
      String type = symbolTable.get(t.lexeme());
      if(type == null){
        //not init type - part of the path
        currType = typeInfo.get(prevType,t.lexeme());
        if(currType == null){
          String m = prevType + "does not have field "+ t.lexeme();
          error(m,t);
        }
        prevType = currType;
      } else {
        //init type
        currType = type;
        prevType = type;
      }
    }

  }
  
      
  public void visit(NegatedRValue node) throws MyPLException {
    node.expr.accept(this);
  }
  

  //----------------------------------------------------------------------
  // expression node
  //----------------------------------------------------------------------
  
  public void visit(Expr node) throws MyPLException {
    node.first.accept(this);
    String lhsType = currType;
    if(node.rest != null){
      node.rest.accept(this);
      String rhsType = currType;
      //check lhs and rhs are compatible
      // +
      if(node.op.lexeme().equals("+")){
        if((lhsType.equals("char") && rhsType.equals("string")) || (lhsType.equals("string") && rhsType.equals("char"))){
          currType = "string";
        } else if(lhsType.equals("char") && rhsType.equals("char")){
          error("incompatible types for + operator ", getFirstToken(node));
        } else if(lhsType.equals("bool") && rhsType.equals("bool")){
          error("incompatible types for + operator ", getFirstToken(node));
        }
        else if(!lhsType.equals(rhsType)) {
          error("incompatible types for + operator ", getFirstToken(node));
        }
      } // -, /, *
      else if(node.op.lexeme().equals("-") || node.op.lexeme().equals("/") || node.op.lexeme().equals("*")){
        if(lhsType.equals("int") || lhsType.equals("double")){
          if(!lhsType.equals(rhsType)){
            error("incompatible types for " + node.op.lexeme() + "operator ", getFirstToken(node));
          }
        } else {
          error("incompatible types for " + node.op.lexeme() + "operator ", getFirstToken(node));
        }
      } // !=, ==
      else if(node.op.lexeme().equals("==") || node.op.lexeme().equals("!=")){
        if(!lhsType.equals(rhsType) && !(lhsType.equals("void") || rhsType.equals("void"))){
          error("incompatible types for " + node.op.lexeme() + "operator ", getFirstToken(node));
        }
        currType = "bool";
      } // <, <=, >, >=
      else if(node.op.lexeme().equals("<") || node.op.lexeme().equals("<=") || node.op.lexeme().equals(">") || node.op.lexeme().equals(">=")){
        if(rhsType.equals("void") || lhsType.equals("void")){
          error("incompatible types for " + node.op.lexeme() + "operator ", getFirstToken(node));
        }
        if(lhsType.equals("int") || lhsType.equals("double") || lhsType.equals("char") || lhsType.equals("string")){
          if(!lhsType.equals(rhsType)){
            error("incompatible types for " + node.op.lexeme() + "operator ", getFirstToken(node));
          }
        } else {
          error("incompatible types for " + node.op.lexeme() + "operator ", getFirstToken(node));
        }
        currType = "bool";
      } // %
      else if(node.op.lexeme().equals("%")){
        if(!lhsType.equals("int") || !rhsType.equals("int")){
          error("incompatible types for % operator", getFirstToken(node));
        }
      }
    }
    if(node.logicallyNegated && !currType.equals("bool")){
      error("logical negation requires bool expression ", getFirstToken(node));
    }
  }


  //----------------------------------------------------------------------
  // terms
  //----------------------------------------------------------------------
  
  public void visit(SimpleTerm node) throws MyPLException {
    node.rvalue.accept(this);
  }
  

  public void visit(ComplexTerm node) throws MyPLException {
    node.expr.accept(this);
  }


}
