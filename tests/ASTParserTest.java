/*
 * File: ASTParserTest.java
 * Date: Spring 2022
 * Auth: 
 * Desc: Basic unit tests for the MyPL ast-based parser class.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;


public class ASTParserTest {

  //------------------------------------------------------------
  // HELPER FUNCTIONS
  //------------------------------------------------------------
  
  private static ASTParser buildParser(String s) throws Exception {
    InputStream in = new ByteArrayInputStream(s.getBytes("UTF-8"));
    ASTParser parser = new ASTParser(new Lexer(in));
    return parser;
  }

  private static String buildString(String... args) {
    String str = "";
    for (String s : args)
      str += s + "\n";
    return str;
  }

  //------------------------------------------------------------
  // TEST CASES
  //------------------------------------------------------------

  @Test
  public void emptyParse() throws Exception {
    ASTParser parser = buildParser("");
    Program p = parser.parse();
    assertEquals(0, p.tdecls.size());
    assertEquals(0, p.fdecls.size());
  }

  @Test
  public void oneTypeDeclInProgram() throws Exception {
    String s = buildString
      ("type Node {",
       "}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.tdecls.size());
    assertEquals(0, p.fdecls.size());
  }
  
  @Test
  public void oneFunDeclInProgram() throws Exception {
    String s = buildString
      ("fun void main() {",
       "}"
       );
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(0, p.tdecls.size());
    assertEquals(1, p.fdecls.size());
  }

  @Test
  public void multipleTypeAndFunDeclsInProgram() throws Exception {
    String s = buildString
      ("type T1 {}",
       "fun void F1() {}",
       "type T2 {}",
       "fun void F2() {}",
       "fun void main() {}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(2, p.tdecls.size());
    assertEquals(3, p.fdecls.size());
  }

  /* additional HW4 tests */

  @Test
  public void varDecl() throws Exception {
    String s = buildString
    ("type Node {",
     "var x = 4",
     "}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.tdecls.size());
    assertEquals(1, p.tdecls.get(0).vdecls.size());
    assertEquals(null,p.tdecls.get(0).vdecls.get(0).typeName);
    assertEquals("x",p.tdecls.get(0).vdecls.get(0).varName.lexeme());
    assertEquals(false,p.tdecls.get(0).vdecls.get(0).isConst);
  }

  @Test
  public void assignStmt() throws Exception {
    String s = buildString
    ("fun void main() {",
     "var x = 4",
     "x = 45",
     "x.b = f()",
     "}");
     ASTParser parser = buildParser(s);
     Program p = parser.parse();
     assertEquals(1, p.fdecls.size());
     assertEquals(3, p.fdecls.get(0).stmts.size());
     AssignStmt s1 = (AssignStmt)p.fdecls.get(0).stmts.get(1);
     AssignStmt s2 = (AssignStmt)p.fdecls.get(0).stmts.get(2);
     assertEquals(1, s1.lvalue.size());
     assertEquals(2, s2.lvalue.size());
     SimpleTerm t1 = (SimpleTerm)s1.expr.first;
     SimpleRValue r1 = (SimpleRValue)t1.rvalue;
     SimpleTerm t2 = (SimpleTerm)s2.expr.first;
     CallExpr r2 = (CallExpr)t2.rvalue;

     assertEquals("45", r1.value.lexeme());
     assertEquals("f", r2.funName.lexeme());
  }

  @Test
  public void returnStmt() throws Exception {
    String s = buildString
    ("fun bool m() {",
     "return true",
     "}");
     ASTParser parser = buildParser(s);
     Program p = parser.parse();
     assertEquals(1, p.fdecls.size());
     assertEquals(1, p.fdecls.get(0).stmts.size());
     ReturnStmt r = (ReturnStmt)p.fdecls.get(0).stmts.get(0);
     SimpleTerm t = (SimpleTerm)r.expr.first;
     SimpleRValue sr = (SimpleRValue)t.rvalue;
     assertEquals("true",sr.value.lexeme());
  }

  @Test
  public void deleteStmt() throws Exception {
    String s = buildString
    ("fun bool m() {",
     "delete x",
     "}");
     ASTParser parser = buildParser(s);
     Program p = parser.parse();
     assertEquals(1, p.fdecls.size());
     assertEquals(1, p.fdecls.get(0).stmts.size());
     DeleteStmt d = (DeleteStmt)p.fdecls.get(0).stmts.get(0);
     assertEquals("x",d.varName.lexeme());
  }

  @Test
  public void whileStmt() throws Exception {
    String s = buildString
    ("fun void w() {",
     " while g == k {",
     "  var g = 9", 
     " }",
     "}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    WhileStmt w = (WhileStmt)p.fdecls.get(0).stmts.get(0);
    assertEquals("==",w.cond.op.lexeme());
    SimpleTerm e = (SimpleTerm)w.cond.first;
    SimpleTerm e2 = (SimpleTerm)w.cond.rest.first;
    IDRValue r = (IDRValue)e.rvalue;
    IDRValue r2 = (IDRValue)e2.rvalue;
    assertEquals("g",r.path.get(0).lexeme());
    assertEquals("k",r2.path.get(0).lexeme());
  }

  @Test
  public void forStmt() throws Exception {
    String str = buildString
    ("fun void f() {",
     " for h from 8 upto 12 {",
     "  var j = \"test\" ",
     " }",
     "}");
    ASTParser parser = buildParser(str);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    ForStmt f = (ForStmt)p.fdecls.get(0).stmts.get(0);
    assertEquals("h",f.varName.lexeme());
    assertEquals(true,f.upto);
    SimpleTerm s = (SimpleTerm)f.start.first;
    SimpleTerm s2 = (SimpleTerm)f.end.first;
    SimpleRValue r = (SimpleRValue)s.rvalue;
    SimpleRValue r2 = (SimpleRValue)s2.rvalue;
    assertEquals("8",r.value.lexeme());
    assertEquals("12",r2.value.lexeme());
  }

  @Test
  public void condStmt() throws Exception {
    String s = buildString 
    ("fun void c() {",
     " if g > h {",
     "  var k = 12",
     " } elif g < k {",
     "  var j = 13",
     " } else {",
     "  var k = 14",
     " }",
     "}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(1, p.fdecls.get(0).stmts.size());
    CondStmt c = (CondStmt)p.fdecls.get(0).stmts.get(0);
    assertEquals(true,c.ifPart!=null);
    assertEquals(1,c.elifs.size());
    assertEquals(1,c.elseStmts.size());
    SimpleTerm t = (SimpleTerm)c.ifPart.cond.first;
    SimpleTerm t2 = (SimpleTerm)c.ifPart.cond.rest.first;
    IDRValue r = (IDRValue)t.rvalue;
    IDRValue r2 = (IDRValue)t2.rvalue;
    SimpleTerm t3 = (SimpleTerm)c.elifs.get(0).cond.first;
    SimpleTerm t4 = (SimpleTerm)c.elifs.get(0).cond.rest.first;
    IDRValue r3 = (IDRValue)t3.rvalue;
    IDRValue r4 = (IDRValue)t4.rvalue;
    assertEquals("g",r.path.get(0).lexeme());
    assertEquals("h",r2.path.get(0).lexeme());
    assertEquals("g",r3.path.get(0).lexeme());
    assertEquals("k",r4.path.get(0).lexeme());
    assertEquals(">",c.ifPart.cond.op.lexeme());
    assertEquals("<",c.elifs.get(0).cond.op.lexeme());
  }


  /* previous pos tests */
  @Test
  public void typeDeclarations() throws Exception {
    String s = buildString
    ("type Node {",
     " var int v1 = 0",
     "}");
    ASTParser parser = buildParser(s);
    parser.parse();
  }

  @Test
  public void functionReturnDeclarations() throws Exception {
    String s = buildString 
    ("fun int f(int i, double d) {",
     " var int v = 0",
     "}");
    ASTParser parser = buildParser(s);
    parser.parse();
  }

  @Test
  public void expressions() throws Exception {
    String s = buildString
    ("fun void e() {",
     " f = 42 + 43 ",
     " f = 6%4",
     " f = new g ",
     " f = g.d > h",
     " f = g(s)",
     " f = neg 43",
     " f = not k",
     " f = (43 != 89)",
     " f = not (g or f)",
     "}");
    ASTParser parser = buildParser(s);
    parser.parse();
  }

  @Test
  public void conditionalStatement() throws Exception {
    String s = buildString 
    ("fun void c() {",
     " if g > h {",
     "  var k = 12",
     " } elif g < k {",
     "  var j = 13",
     " } else {",
     "  var k = 14",
     " }",
     "}");
    ASTParser parser = buildParser(s);
    parser.parse();
  }

  @Test
  public void whileStatement() throws Exception {
    String s = buildString
    ("fun void w() {",
     " while g == k {",
     "  var g = 9", 
     " }",
     "}");
     ASTParser parser = buildParser(s);
     parser.parse();
  }

  @Test
  public void forStatement() throws Exception {
    String s = buildString
    ("fun void f() {",
     " for h from 8 upto 12 {",
     "  var j = \"test\" ",
     " }",
     " for h from 8 downto 2 {",
     "  var g = 9",
     " }",
     "}");
    ASTParser parser = buildParser(s);
    parser.parse();
  }
  
  @Test
  public void returnDeleteStatement() throws Exception {
    String s = buildString
    ("fun void r() {",
     " delete d",
     " return f",
     "}");
    ASTParser parser = buildParser(s);
    parser.parse();
  }

  /* previous neg tests */
  @Test
  public void lvalueWithoutId() throws Exception {
    String s = "fun void main() { l.8 = 8 }";
    ASTParser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e){
      // assertEquals()
    }
  }

  @Test
  public void invalidParams() throws Exception {
    String s = "fun void main(fun j) { }" ;
    ASTParser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e){
      // assertEquals()
    }
  }

  @Test
  public void invalidIfStatement() throws Exception {
    String s = buildString 
    ("fun void c() {",
     " if g > h {",
     "  var k = 12",
     " } else g < k {",
     "  var j = 13",
     " } elif {",
     "  var k = 14",
     " }",
     "}");
    ASTParser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e){
      // assertEquals()
    }
  }

  @Test
  public void twoOperators() throws Exception {
    String s = "fun void main() { var x = 8 + - 9 }";
    ASTParser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e){
      // assertEquals()
    }
  }

  @Test
  public void noExpressionWhileStatement() throws Exception {
    String s = "fun void main() { while { var j = 9 } }";
    ASTParser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e){
      // assertEquals()
    }
  }

  /* const var tests */

  @Test
  public void constVarDecl() throws Exception {
    String s = buildString
    ("type Node {",
     "const var x = 4",
     "}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.tdecls.size());
    assertEquals(1, p.tdecls.get(0).vdecls.size());
    assertEquals(null,p.tdecls.get(0).vdecls.get(0).typeName);
    assertEquals("x",p.tdecls.get(0).vdecls.get(0).varName.lexeme());
    assertEquals(true,p.tdecls.get(0).vdecls.get(0).isConst);
  }

  @Test
  public void wrongConstDecl() throws Exception {
    String s = buildString
    ("type Node {",
     "const x = 4",
     "}");
    ASTParser parser = buildParser(s);
    try {
      Program p = parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e){
      // assertEquals()
    }
  }

  @Test
  public void basicConstParam() throws Exception {
    String s = buildString 
    ("fun void c(int i, const int k) {",
     "}");
    ASTParser parser = buildParser(s);
    Program p = parser.parse();
    assertEquals(1, p.fdecls.size());
    assertEquals(2, p.fdecls.get(0).params.size());
    assertEquals(false, p.fdecls.get(0).params.get(0).isConst);
    assertEquals("int", p.fdecls.get(0).params.get(0).paramType.lexeme());
    assertEquals(true, p.fdecls.get(0).params.get(1).isConst);
    assertEquals("int", p.fdecls.get(0).params.get(1).paramType.lexeme());
  }

  

}
