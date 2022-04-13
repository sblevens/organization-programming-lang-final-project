/* 
 * File: ASTParser.java
 * Date: Spring 2022
 * Auth: Sami Blevens
 * Desc: AST Parser for HW4
 */

import java.util.ArrayList;
import java.util.List;


public class ASTParser {

  private Lexer lexer = null; 
  private Token currToken = null;
  private final boolean DEBUG = false;

  /** 
   */
  public ASTParser(Lexer lexer) {
    this.lexer = lexer;
  }

  /**
   */
  public Program parse() throws MyPLException
  {
    // <program> ::= (<tdecl> | <fdecl>)*
    Program progNode = new Program();
    advance();
    while (!match(TokenType.EOS)) {
      if (match(TokenType.TYPE))
        tdecl(progNode);
      else
        fdecl(progNode);
    }
    advance(); // eat the EOS token
    return progNode;
  }

  
  //------------------------------------------------------------ 
  // Helper Functions
  //------------------------------------------------------------

  // get next token
  private void advance() throws MyPLException {
    currToken = lexer.nextToken();
  }

  // advance if current token is of given type, otherwise error
  private void eat(TokenType t, String msg) throws MyPLException {
    if (match(t))
      advance();
    else
      error(msg);
  }

  // true if current token is of type t
  private boolean match(TokenType t) {
    return currToken.type() == t;
  }
  
  // throw a formatted parser error
  private void error(String msg) throws MyPLException {
    String s = msg + ", found '" + currToken.lexeme() + "' ";
    s += "at line " + currToken.line();
    s += ", column " + currToken.column();
    throw MyPLException.ParseError(s);
  }

  // output a debug message (if DEBUG is set)
  private void debug(String msg) {
    if (DEBUG)
      System.out.println("[debug]: " + msg);
  }

  // return true if current token is a (non-id) primitive type
  private boolean isPrimitiveType() {
    return match(TokenType.INT_TYPE) || match(TokenType.DOUBLE_TYPE) ||
      match(TokenType.BOOL_TYPE) || match(TokenType.CHAR_TYPE) ||
      match(TokenType.STRING_TYPE);
  }

  // return true if current token is a (non-id) primitive value
  private boolean isPrimitiveValue() {
    return match(TokenType.INT_VAL) || match(TokenType.DOUBLE_VAL) ||
      match(TokenType.BOOL_VAL) || match(TokenType.CHAR_VAL) ||
      match(TokenType.STRING_VAL);
  }
    
  // return true if current token starts an expression
  private boolean isExpr() {
    return match(TokenType.NOT) || match(TokenType.LPAREN) ||
      match(TokenType.NIL) || match(TokenType.NEW) ||
      match(TokenType.ID) || match(TokenType.NEG) ||
      match(TokenType.INT_VAL) || match(TokenType.DOUBLE_VAL) ||
      match(TokenType.BOOL_VAL) || match(TokenType.CHAR_VAL) ||
      match(TokenType.STRING_VAL);
  }

  private boolean isOperator() {
    return match(TokenType.PLUS) || match(TokenType.MINUS) ||
      match(TokenType.DIVIDE) || match(TokenType.MULTIPLY) ||
      match(TokenType.MODULO) || match(TokenType.AND) ||
      match(TokenType.OR) || match(TokenType.EQUAL) ||
      match(TokenType.LESS_THAN) || match(TokenType.GREATER_THAN) ||
      match(TokenType.LESS_THAN_EQUAL) || match(TokenType.GREATER_THAN_EQUAL) ||
      match(TokenType.NOT_EQUAL);
  }

  
  //------------------------------------------------------------
  // Recursive Descent Functions 
  //------------------------------------------------------------


  // TODO: Add your recursive descent functions from HW-3
  // and extend them to build up the AST
  private void tdecl(Program progNode) throws MyPLException {
    // <tdel> ::= TYPE ID LBRACE <vdelcs> RBRACE
    TypeDecl t = new TypeDecl();
    eat(TokenType.TYPE,"expecting type");
    t.typeName = currToken;
    eat(TokenType.ID,"expecting id");
    eat(TokenType.LBRACE,"expecting {");
    List<VarDeclStmt> vlist = new ArrayList<>();
    vdelcs(vlist);
    t.vdecls = vlist;
    eat(TokenType.RBRACE,"expecting }");
    progNode.tdecls.add(t);
  }

  private void fdecl(Program progNode) throws MyPLException {
    //<fdecl> ::= FUN ( <dtype> | VOID ) ID LPAREN <params> RPAREN LBRACE <stmts> RBRACE
    FunDecl f = new FunDecl();
    eat(TokenType.FUN,"expecting fun");
    if(match(TokenType.ID) || isPrimitiveType() || match(TokenType.VOID_TYPE)){
      f.returnType = currToken;
      advance();
    } else {
      error("expecting function return type");
    }
    f.funName = currToken;
    eat(TokenType.ID,"expecting id");
    List<FunParam> p = new ArrayList<>();
    eat(TokenType.LPAREN,"expecting (");
    params(p);
    f.params = p;
    eat(TokenType.RPAREN, "expecting )");
    eat(TokenType.LBRACE, "expecting {");
    List<Stmt> s = new ArrayList<>();
    stmts(s);
    f.stmts = s;
    eat(TokenType.RBRACE, "expecting }");
    progNode.fdecls.add(f);
  }

  private void vdelcs(List<VarDeclStmt> vlist)  throws MyPLException{
    // <vdelcs> ::= ( <vdecl_stmt)*
    while(match(TokenType.VAR)){
      VarDeclStmt v = new VarDeclStmt();
      vdecl_stmt(v);
      vlist.add(v);
    }
  }

  private void vdecl_stmt(VarDeclStmt v)  throws MyPLException{
    //<vdecl_stmt> ::= VAR ( <dtype> | E ) ID ASSIGN <expr>
    eat(TokenType.VAR,"expecting var");
    if(isPrimitiveType()){
      //dtype();
      v.typeName = currToken;
      advance();
      v.varName = currToken;
      eat(TokenType.ID,"expecting id");
    } else if(match(TokenType.ID)){
      Token curr = currToken;
      advance();
      // v.typeName = currToken;
      if(match(TokenType.ID)){
        v.typeName = curr;
        v.varName = currToken;
        advance();
      } else {
        v.varName = curr;
      }
    }
    eat(TokenType.ASSIGN,"expecting =");
    Expr e = new Expr();
    expr(e);
    v.expr = e;
  }

  private void expr(Expr e)  throws MyPLException{
    //<expr> ::= ( <rvalue> | NOT <expr> | LPAREN <expr> RPAREN ) ( <operator> <expr> | E )
    if(isExpr()){
      ExprTerm t;
      if(match(TokenType.NOT)){
        e.logicallyNegated = true;
        advance();
        ComplexTerm c = new ComplexTerm();
        Expr ec = new Expr();
        expr(ec);
        c.expr = ec;
        t = c;
        e.first = t;
      } else if(match(TokenType.LPAREN)){
        advance();
        ComplexTerm c = new ComplexTerm();
        Expr ec = new Expr();
        expr(ec);
        c.expr = ec;
        t = c;
        e.first = t;
        eat(TokenType.RPAREN,"expecting )");
      } else {
        SimpleTerm s = new SimpleTerm();
        // RValue r = null;
        rvalue(s);
        // s.rvalue = r;
        t = s;
        e.first = t;
      }
    } else {
      error("expecting expression");
    }
    if(isOperator()){
      e.op = currToken;
      advance(); //eat operator
      Expr re = new Expr();
      expr(re);
      e.rest = re;
    }
  }

  private void rvalue(SimpleTerm r) throws MyPLException {
    //<rvalue> ::= <pval> | NIL | NEW ID | <idrval> | <call_expr> | NEG <expr>
    if(isPrimitiveValue() || match(TokenType.NIL)){
      SimpleRValue s = new SimpleRValue();
      s.value = currToken;
      advance();
      r.rvalue = s;
    } else if(match(TokenType.NEW)){
      NewRValue n = new NewRValue();
      advance();
      n.typeName = currToken;
      eat(TokenType.ID,"expecting id");
      r.rvalue = n;
    } else if(match(TokenType.ID)){
      Token curr = currToken;
      advance();
      if(match(TokenType.DOT)){
        //<idrval> ::= ID ( DOT ID )
        IDRValue i = new IDRValue();
        i.path.add(curr);
        while(match(TokenType.DOT)){
          advance(); //eat .
          i.path.add(currToken);
          eat(TokenType.ID, "expecting id");
        }
        r.rvalue = i;
      } else if(match(TokenType.LPAREN)){
        //<call_expr> ::= ID LPAREN <args> RPAREN
        CallExpr c = new CallExpr();
        c.funName = curr;
        advance();
        List<Expr> a = new ArrayList<>();
        args(a);
        c.args = a;
        eat(TokenType.RPAREN, "expecting )");
        r.rvalue = c;
      } else {
        IDRValue i = new IDRValue();
        i.path.add(curr);
        r.rvalue = i;
      }
    } else if(match(TokenType.NEG)){
      NegatedRValue n = new NegatedRValue();
      advance();
      Expr e = new Expr();
      expr(e);
      n.expr = e;
      r.rvalue = n;
    } else {
      error("expecting rvalue");
    }
  }

  private void args(List<Expr> a) throws MyPLException{
    //<args> ::= <expr> ( COMMA <expr>)* | E
    if(isExpr()){
      Expr e = new Expr();
      expr(e);
      a.add(e);
      while(match(TokenType.COMMA)){
        advance();
        e = new Expr();
        expr(e);
        a.add(e);
      }
    }
  }

  private void params(List<FunParam> p) throws MyPLException {
    //<params> ::= <dtype> ID ( COMMA <dtype> ID )* | E
    FunParam f = new FunParam();
    if(match(TokenType.ID) || isPrimitiveType()){
      f.paramType = currToken;
      advance();
      f.paramName = currToken;
      eat(TokenType.ID,"expecting id");
      p.add(f);
      while(match(TokenType.COMMA)){
        f = new FunParam();
        advance();
        if(match(TokenType.ID) || isPrimitiveType()){
          f.paramType = currToken;
          advance();
        } else {
          error("expecting dtype");
        }
        f.paramName = currToken;
        eat(TokenType.ID, "expecting id");
        p.add(f);
      }
    }
  }

  private void stmts(List<Stmt> s) throws MyPLException {
    //<stmts> ::= ( <stmt> )*
    while(match(TokenType.VAR) || match(TokenType.ID) || match(TokenType.IF) || match(TokenType.WHILE) || match(TokenType.FOR) || match(TokenType.RETURN) || match(TokenType.DELETE)){
      // Stmt st = null;
      stmt(s);
      // s.add(st);
    }
  }
  
  private void stmt(List<Stmt> s) throws MyPLException {
    if(match(TokenType.VAR)){
      VarDeclStmt v = new VarDeclStmt();
      vdecl_stmt(v);
      s.add(v);
    } else if(match(TokenType.ID)){
      //either assign or call expr
      Token curr = currToken;
      advance();
      //call_expr
      if(match(TokenType.LPAREN)){
        CallExpr c = new CallExpr();
        c.funName = curr;
        advance();
        List<Expr> a = new ArrayList<>();
        args(a);
        c.args = a;
        eat(TokenType.RPAREN,"s");
        s.add(c);
      } else {
        //assign_stmt
        AssignStmt a = new AssignStmt();
        a.lvalue.add(curr);
        while(match(TokenType.DOT)){
          advance();
          a.lvalue.add(currToken);
          eat(TokenType.ID,"expecting id");
        }
        eat(TokenType.ASSIGN,"expecting =");
        Expr e = new Expr();
        expr(e);
        a.expr = e;
        s.add(a);
      }
    } else if(match(TokenType.IF)){
      CondStmt c = new CondStmt();
      cond_stmt(c);
      s.add(c);
    } else if(match(TokenType.WHILE)){
      WhileStmt w = new WhileStmt();
      while_stmt(w);
      s.add(w);
    } else if(match(TokenType.FOR)){
      ForStmt f = new ForStmt();
      for_stmt(f);
      s.add(f);
    } else if(match(TokenType.RETURN)){
      ReturnStmt r = new ReturnStmt();
      ret_stmt(r);
      s.add(r);
    } else if(match(TokenType.DELETE)){
      DeleteStmt d = new DeleteStmt();
      delete_stmt(d);
      s.add(d);
    } else {
      error("expecting statment");
    }
  }

  private void cond_stmt(CondStmt c) throws MyPLException {
    //<cond_stmt> ::= IF <expr> LBRACE <stmts> RBRACE <condt>
    BasicIf f = new BasicIf();
    eat(TokenType.IF,"expecting if");
    Expr e = new Expr();
    expr(e);
    f.cond = e;
    eat(TokenType.LBRACE,"expecting {");
    List<Stmt> s = new ArrayList<>();
    stmts(s);
    f.stmts = s;
    eat(TokenType.RBRACE,"expecting }");
    c.ifPart = f;
    condt(c);
  }

  private void condt(CondStmt c) throws MyPLException {
    //<condt> ::= ELIF <expr> LBRACE <stmts> RBRACE <condt> | ELSE LBRACE <stmts> RBRACE | E
    if(match(TokenType.ELIF)){
      BasicIf f = new BasicIf();
      advance();
      Expr e = new Expr();
      expr(e);
      f.cond = e;
      eat(TokenType.LBRACE, "expecting {");
      List<Stmt> s = new ArrayList<>();
      stmts(s);
      f.stmts = s;
      eat(TokenType.RBRACE, "expecting }");
      c.elifs.add(f);
      condt(c);
    } else if(match(TokenType.ELSE)){
      List<Stmt> s = new ArrayList<>();
      advance();
      eat(TokenType.LBRACE,"expecting {");
      stmts(s);
      c.elseStmts = s;
      eat(TokenType.RBRACE,"expecting }");
    }
  }

  private void while_stmt(WhileStmt w) throws MyPLException {
    //<while_stmt> ::= WHILE <expr> LBRACE <stmts> RBRACE
    eat(TokenType.WHILE,"expecting while");
    Expr e = new Expr();
    expr(e);
    w.cond = e;
    eat(TokenType.LBRACE,"expecting {");
    List<Stmt> s = new ArrayList<>();
    stmts(s);
    w.stmts = s;
    eat(TokenType.RBRACE,"expecting }");
  }

  private void for_stmt(ForStmt f) throws MyPLException {
    //<for_stmt> ::= FOR ID FROM <expr> ( UPTO | DOWNTO ) <expr> LBRACE <stmts> RBRACE
    eat(TokenType.FOR,"expecting for");
    f.varName = currToken;
    eat(TokenType.ID,"expecting id");
    eat(TokenType.FROM,"expecting from");
    Expr e = new Expr();
    expr(e);
    f.start = e;
    if(match(TokenType.UPTO)){
      advance();
    } else if (match(TokenType.DOWNTO)){
      f.upto = false;
      advance();
    } else {
      error("expecting upto or downto");
    }
    Expr end = new Expr();
    expr(end);
    f.end = end;
    eat(TokenType.LBRACE,"expecting {");
    List<Stmt> s = new ArrayList<>();
    stmts(s);
    f.stmts = s;
    eat(TokenType.RBRACE,"expecting }");
  }

  private void ret_stmt(ReturnStmt r) throws MyPLException {
    //<ret_stmt> ::= RETURN ( <expr> | E )
    eat(TokenType.RETURN,"expecting return");
    if(isExpr()){
      Expr e = new Expr();
      expr(e);
      r.expr = e;
    }
  }

  private void delete_stmt(DeleteStmt d) throws MyPLException {
    //<delete_stmt> ::= DELETE ID
    eat(TokenType.DELETE,"expecting delete");
    d.varName = currToken;
    eat(TokenType.ID, "expecting id");
  }
  
}
