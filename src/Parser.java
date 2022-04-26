/* 
 * File: Parser.java
 * Date: Spring 2022
 * Auth: Sami Blevens 
 * Desc: Parser for HW3
 */


public class Parser {

  private Lexer lexer = null; 
  private Token currToken = null;

  
  // constructor
  public Parser(Lexer lexer) {
    this.lexer = lexer;
  }

  // do the parse
  public void parse() throws MyPLException
  {
    // <program> ::= (<tdecl> | <fdecl>)*
    advance();
    while (!match(TokenType.EOS)) {
      if (match(TokenType.TYPE))
        tdecl();
      else
        fdecl();
    }
    advance(); // eat the EOS token
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
  // private void debug(String msg) {
  //   if (DEBUG)
  //     System.out.println("[debug]: " + msg);
  // }

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


  /* TODO: Add the recursive descent functions below */

  private void tdecl() throws MyPLException {
    // <tdel> ::= TYPE ID LBRACE <vdelcs> RBRACE
    eat(TokenType.TYPE,"expecting type");
    eat(TokenType.ID,"expecting id");
    eat(TokenType.LBRACE,"expecting {");
    vdelcs();
    eat(TokenType.RBRACE,"expecting }");
  }

  private void fdecl() throws MyPLException {
    //<fdecl> ::= FUN ( <dtype> | VOID ) ID LPAREN <params> RPAREN LBRACE <stmts> RBRACE
    eat(TokenType.FUN,"expecting fun");
    if(match(TokenType.ID) || isPrimitiveType() || match(TokenType.VOID_TYPE)){
      advance();
    } else {
      error("expecting function return type");
    }
    eat(TokenType.ID,"expecting id");
    eat(TokenType.LPAREN,"expecting (");
    params();
    eat(TokenType.RPAREN, "expecting )");
    eat(TokenType.LBRACE, "expecting {");
    stmts();
    eat(TokenType.RBRACE, "expecting }");
  }

  private void vdelcs()  throws MyPLException{
    // <vdelcs> ::= ( <vdecl_stmt)*
    while(match(TokenType.VAR)){
      vdecl_stmt();
    }
  }

  private void vdecl_stmt()  throws MyPLException {
    //<vdecl_stmt> ::= (const | E) VAR ( <dtype> | E ) ID ASSIGN <expr>
    if(match(TokenType.CONST)){
      advance();
    }
    eat(TokenType.VAR,"expecting var");
    if(isPrimitiveType()){
      //dtype();
      advance();
      eat(TokenType.ID,"expecting id");
    } else if(match(TokenType.ID)){
      advance();
      if(match(TokenType.ID)){
        advance();
      }
    }
    eat(TokenType.ASSIGN,"expecting =");
    expr();
  }

  private void expr()  throws MyPLException{
    //<expr> ::= ( <rvalue> | NOT <expr> | LPAREN <expr> RPAREN ) ( <operator> <expr> | E )
    if(isExpr()){
      if(match(TokenType.NOT)){
        advance();
        expr();
      } else if(match(TokenType.LPAREN)){
        advance();
        expr();
        eat(TokenType.RPAREN,"expecting )");
      } else {
        rvalue();
      }
    } else {
      error("expecting expression");
    }
    if(isOperator()){
      advance(); //eat operator
      expr();
    }
  }

  private void rvalue() throws MyPLException {
    //<rvalue> ::= <pval> | NIL | NEW ID | <idrval> | <call_expr> | NEG <expr>
    if(isPrimitiveValue() || match(TokenType.NIL)){
      advance();
    } else if(match(TokenType.NEW)){
      advance();
      eat(TokenType.ID,"expecting id");
    } else if(match(TokenType.ID)){
      advance();
      if(match(TokenType.DOT)){
        //<idrval> ::= ID ( DOT ID )
        while(match(TokenType.DOT)){
          advance(); //eat .
          eat(TokenType.ID, "expecting id");
        }
      } else if(match(TokenType.LPAREN)){
        //<call_expr> ::= ID LPAREN <args> RPAREN
        advance();
        args();
        eat(TokenType.RPAREN, "expecting )");
      }
    } else if(match(TokenType.NEG)){
      advance();
      expr();
    } else {
      error("expecting rvalue");
    }
  }

  private void args() throws MyPLException{
    //<args> ::= <expr> ( COMMA <expr>)* | E
    if(isExpr()){
      expr();
      while(match(TokenType.COMMA)){
        advance();
        expr();
      }
    }
  }

  private void params() throws MyPLException {
    //<params> ::= (const | E) <dtype> ID ( COMMA <dtype> ID )* | E
    if(match(TokenType.CONST)){
      advance();
    }
    if(match(TokenType.ID) || isPrimitiveType()){
      advance();
      eat(TokenType.ID,"expecting id");
      while(match(TokenType.COMMA)){
        advance();
        if(match(TokenType.CONST)){
          advance();
        }
        if(match(TokenType.ID) || isPrimitiveType()){
          advance();
        } else {
          error("expecting dtype");
        }
        eat(TokenType.ID, "expecting id");
      }
    }
  }

  private void stmts() throws MyPLException {
    //<stmts> ::= ( <stmt> )*
    while(match(TokenType.VAR) || match(TokenType.CONST) || match(TokenType.ID) || match(TokenType.IF) || match(TokenType.WHILE) || match(TokenType.FOR) || match(TokenType.RETURN) || match(TokenType.DELETE)){
      stmt();
    }
  }
  
  private void stmt() throws MyPLException {
    if(match(TokenType.VAR) || match(TokenType.CONST)){
      vdecl_stmt();
    } else if(match(TokenType.ID)){
      //either assign or call expr
      advance();
      //call_expr
      if(match(TokenType.LPAREN)){
        advance();
        args();
        eat(TokenType.RPAREN,"s");
      } else {
        //assign_stmt
        while(match(TokenType.DOT)){
          advance();
          eat(TokenType.ID,"expecting id");
        }
        eat(TokenType.ASSIGN,"expecting =");
        expr();
      }
    } else if(match(TokenType.IF)){
      cond_stmt();
    } else if(match(TokenType.WHILE)){
      while_stmt();
    } else if(match(TokenType.FOR)){
      for_stmt();
    } else if(match(TokenType.RETURN)){
      ret_stmt();
    } else if(match(TokenType.DELETE)){
      delete_stmt();
    } else {
      error("expecting statment");
    }
  }

  private void cond_stmt() throws MyPLException {
    //<cond_stmt> ::= IF <expr> LBRACE <stmts> RBRACE <condt>
    eat(TokenType.IF,"expecting if");
    expr();
    eat(TokenType.LBRACE,"expecting {");
    stmts();
    eat(TokenType.RBRACE,"expecting }");
    condt();
  }

  private void condt() throws MyPLException {
    //<condt> ::= ELIF <expr> LBRACE <stmts> RBRACE <condt> | ELSE LBRACE <stmts> RBRACE | E
    if(match(TokenType.ELIF)){
      advance();
      expr();
      eat(TokenType.LBRACE, "expecting {");
      stmts();
      eat(TokenType.RBRACE, "expecting }");
      condt();
    } else if(match(TokenType.ELSE)){
      advance();
      eat(TokenType.LBRACE,"expecting {");
      stmts();
      eat(TokenType.RBRACE,"expecting }");
    }
  }

  private void while_stmt() throws MyPLException {
    //<while_stmt> ::= WHILE <expr> LBRACE <stmts> RBRACE
    eat(TokenType.WHILE,"expecting while");
    expr();
    eat(TokenType.LBRACE,"expecting {");
    stmts();
    eat(TokenType.RBRACE,"expecting }");
  }

  private void for_stmt() throws MyPLException {
    //<for_stmt> ::= FOR ID FROM <expr> ( UPTO | DOWNTO ) <expr> LBRACE <stmts> RBRACE
    eat(TokenType.FOR,"expecting for");
    eat(TokenType.ID,"expecting id");
    eat(TokenType.FROM,"expecting from");
    expr();
    if(match(TokenType.UPTO) || match(TokenType.DOWNTO)){
      advance();
    } else {
      error("expecting upto or downto");
    }
    expr();
    eat(TokenType.LBRACE,"expecting {");
    stmts();
    eat(TokenType.RBRACE,"expecting }");
  }

  private void ret_stmt() throws MyPLException {
    //<ret_stmt> ::= RETURN ( <expr> | E )
    eat(TokenType.RETURN,"expecting return");
    if(isExpr()){
      expr();
    }
  }

  private void delete_stmt() throws MyPLException {
    //<delete_stmt> ::= DELETE ID
    eat(TokenType.DELETE,"expecting delete");
    eat(TokenType.ID, "expecting id");
  }

}
