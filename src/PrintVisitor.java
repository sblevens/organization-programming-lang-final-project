/*
 * File: PrintVisitor.java
 * Date: Spring 2022
 * Auth: Sami Blevens
 * Desc: Print Visitor for HW4
 */

import java.io.PrintStream;


public class PrintVisitor implements Visitor {

  // output stream for printing
  private PrintStream out;
  // current indent level (number of spaces)
  private int indent = 0;
  // indentation amount
  private final int INDENT_AMT = 2;
  
  //------------------------------------------------------------
  // HELPER FUNCTIONS
  //------------------------------------------------------------
  
  private String getIndent() {
    return " ".repeat(indent);
  }

  private void incIndent() {
    indent += INDENT_AMT;
  }

  private void decIndent() {
    indent -= INDENT_AMT;
  }

  //------------------------------------------------------------
  // VISITOR FUNCTIONS
  //------------------------------------------------------------

  // Hint: To help deal with call expressions, which can be statements
  // or expressions, statements should not indent themselves and add
  // newlines. Instead, the function asking statements to print
  // themselves should add the indent and newlines.
  

  // constructor
  public PrintVisitor(PrintStream printStream) {
    out = printStream;
  }

  
  // top-level nodes

  @Override
  public void visit(Program node) throws MyPLException {
    // print type decls first
    for (TypeDecl d : node.tdecls)
      d.accept(this);
    // print function decls second
    for (FunDecl d : node.fdecls)
      d.accept(this);
  }


  // TODO: Finish the rest of the visitor functions ...

  public void visit(TypeDecl node) throws MyPLException{
    System.out.println("type "+node.typeName.lexeme() +" {");
    incIndent();
    for (VarDeclStmt v: node.vdecls){
      System.out.print(getIndent());
      v.accept(this);
      System.out.println();
    }
    decIndent();
    System.out.println("}");
    System.out.println();
  }

  public void visit(FunDecl node) throws MyPLException{
    System.out.print("fun "+node.returnType.lexeme()+ " " +node.funName.lexeme()+"(");
    if(node.params.size() > 0){
      System.out.print(node.params.get(0).paramType.lexeme() + " " + node.params.get(0).paramName.lexeme());
      int i = 1;
      while(i < node.params.size()){
        System.out.print(", "+node.params.get(i).paramType.lexeme() + " " + node.params.get(i).paramName.lexeme());
        i++;
      }
    }
    System.out.println(") {");
    incIndent();
    for (Stmt s: node.stmts){
      System.out.print(getIndent());
      s.accept(this);
      System.out.println();
    }
    decIndent();
    System.out.println("}");
    System.out.println();
  }

  // statement nodes
  public void visit(VarDeclStmt node) throws MyPLException{
    System.out.print("var ");
    if(node.typeName != null){
      System.out.print(node.typeName.lexeme()+" ");
    }
    System.out.print(node.varName.lexeme() + " = ");
    node.expr.accept(this);
    // System.out.println();
  }

  public void visit(AssignStmt node) throws MyPLException{
    System.out.print(node.lvalue.get(0).lexeme());
    int i = 1;
    while(i < node.lvalue.size()){
      System.out.print("."+node.lvalue.get(i).lexeme());
      i++;
    }
    System.out.print(" = ");
    node.expr.accept(this);
    // System.out.println();
  }

  public void visit(CondStmt node) throws MyPLException{
    System.out.print("if ");
    node.ifPart.cond.accept(this);
    System.out.println(" {");
    incIndent();
    for(Stmt s: node.ifPart.stmts){
      System.out.print(getIndent());
      s.accept(this);
      System.out.println();
    }
    decIndent();
    System.out.print(getIndent() + "}");
    if(node.elifs.size() > 0){
      for(BasicIf b: node.elifs){
        System.out.println();
        System.out.print(getIndent() + "elif ");
        b.cond.accept(this);
        System.out.println(" {");
        incIndent();
        for(Stmt s: b.stmts){
          System.out.print(getIndent());
          s.accept(this);
          System.out.println();
        }
        decIndent();
        System.out.print(getIndent() + "}");
      }
    }
    if(node.elseStmts != null){
      System.out.println();
      System.out.println(getIndent() + "else {");
      incIndent();
      for(Stmt s: node.elseStmts){
        System.out.print(getIndent());
        s.accept(this);
        System.out.println();
      }
      decIndent();
      System.out.print(getIndent() + "}");
    }
  }

  public void visit(WhileStmt node) throws MyPLException{
    System.out.print("while ");
    node.cond.accept(this);
    System.out.println(" {");
    incIndent();
    for(Stmt s: node.stmts){
      System.out.print(getIndent());
      s.accept(this);
      System.out.println();
    }
    decIndent();
    System.out.print(getIndent() + "}");
  }

  public void visit(ForStmt node) throws MyPLException{
    System.out.print("for "+node.varName.lexeme()+" from ");
    node.start.accept(this);
    if(node.upto){
      System.out.print(" upto ");
    } else {
      System.out.print(" downto ");
    }
    node.end.accept(this);
    System.out.println(" {");
    incIndent();
    for(Stmt s: node.stmts){
      System.out.print(getIndent());
      s.accept(this);
      System.out.println();
    }
    decIndent();
    System.out.print(getIndent() + "}");
  } 

  public void visit(ReturnStmt node) throws MyPLException{
    System.out.print("return ");
    node.expr.accept(this);
    // System.out.println();
  }  

  public void visit(DeleteStmt node) throws MyPLException{
    System.out.print("delete "+ node.varName.lexeme());
  } 

  // statement and rvalue node
  public void visit(CallExpr node) throws MyPLException{
    System.out.print(node.funName.lexeme()+"(");
    if(node.args.size() > 0){
      node.args.get(0).accept(this);
      int i = 1;
      while(i < node.args.size()){
        System.out.print(", ");
        node.args.get(i).accept(this);
        i++;
      }
    }
    System.out.print(")");
    // System.out.println();
  }  

  // rvalue nodes
  public void visit(SimpleRValue node) throws MyPLException{
    if(node.value.type() == TokenType.STRING_VAL){
      System.out.print("\"");
    }
    System.out.print(node.value.lexeme());
    if(node.value.type() == TokenType.STRING_VAL){
      System.out.print("\"");
    }
  }   
  public void visit(NewRValue node) throws MyPLException{
    System.out.print("new "+ node.typeName.lexeme());
  }      
  public void visit(IDRValue node) throws MyPLException{
    System.out.print(node.path.get(0).lexeme());
    int i = 1;
    while(i < node.path.size()){
      System.out.print("."+node.path.get(i).lexeme());
      i++;
    }
  }    
  public void visit(NegatedRValue node) throws MyPLException{
    System.out.print("neg ");
    node.expr.accept(this);
  }      

  // expression node
  public void visit(Expr node) throws MyPLException{
    if(node.logicallyNegated){
      System.out.print("(not ");
    }
    if(node.op!=null){
      System.out.print("(");
    }
    if(node.first!=null){
      node.first.accept(this);
    }
    if(node.op != null){
      System.out.print(" " +node.op.lexeme() + " ");
    }
    if(node.rest != null){
      node.rest.accept(this);
    }
    if(node.op!=null){
      System.out.print(")");
    }
    if(node.logicallyNegated){
      System.out.print(")");
    }
  }

  // terms
  public void visit(SimpleTerm node) throws MyPLException{
    // System.out.print(node.rvalue);
    node.rvalue.accept(this);
  }
  public void visit(ComplexTerm node) throws MyPLException{
    // if(node.expr.first instanceof ComplexTerm || node.expr.op != null){
    //   System.out.print("(");
    // }
    node.expr.accept(this);
    // if(node.expr.first instanceof ComplexTerm || node.expr.op != null){
    //   System.out.print(")");
    // }
  }
  

}
