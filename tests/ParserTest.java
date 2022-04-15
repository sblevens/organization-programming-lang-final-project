/*
 * File: ParserTest.java
 * Date: Spring 2022
 * Auth: Sami Blevens
 * Desc: Basic unit tests for the MyPL parser class.
 */


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


public class ParserTest {

  //------------------------------------------------------------
  // HELPER FUNCTIONS
  //------------------------------------------------------------
  
  private static Parser buildParser(String s) throws Exception {
    InputStream in = new ByteArrayInputStream(s.getBytes("UTF-8"));
    Parser parser = new Parser(new Lexer(in));
    return parser;
  }

  private static String buildString(String... args) {
    String str = "";
    for (String s : args)
      str += s + "\n";
    return str;
  }


  //------------------------------------------------------------
  // POSITIVE TEST CASES
  //------------------------------------------------------------

  @Test
  public void emptyParse() throws Exception {
    Parser parser = buildParser("");
    parser.parse();
  }

  @Test
  public void implicitVariableDecls() throws Exception {
    String s = buildString
      ("fun void main() {",
       "  var v1 = 0",
       "  var v2 = 0.0",
       "  var v3 = false",
       "  var v4 = 'a'",
       "  var v5 = \"abc\"",
       "  var v6 = new Node",
       "}");
    Parser parser = buildParser(s);
    parser.parse();
  }

  @Test
  public void explicitVariableDecls() throws Exception {
    String s = buildString
      ("fun void main() {",
       "  var int v1 = 0",
       "  var double v2 = 0.0",
       "  var bool v3 = false",
       "  var char v4 = 'a'",
       "  var string v5 = \"abc\"",
       "  var Node v5 = new Node",
       "}");
    Parser parser = buildParser(s);
    parser.parse();
  }

  /* TODO: 

       (1). Add your own test cases below as you create your recursive
            descent functions. By the end you should have a full suite
            of positive test cases that "pass" the tests. 

       (2). Ensure your program (bazel-bin/mypl --parse) works for the
            example file (examples/parser.txt). 
 
       (3). For the parser, the "negative" tests below are just as
            important as the "positive" test cases. Like in (1), be
            sure to add negative test cases as you build out your
            parser. By the end you should also have a full set of
            negative cases as well.
  */

  @Test
  public void typeDeclarations() throws Exception {
    String s = buildString
    ("type Node {",
     " var int v1 = 0",
     "}");
    Parser parser = buildParser(s);
    parser.parse();
  }

  @Test
  public void functionReturnDeclarations() throws Exception {
    String s = buildString 
    ("fun int f(int i, double d) {",
     " var int v = 0",
     "}");
    Parser parser = buildParser(s);
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
    Parser parser = buildParser(s);
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
    Parser parser = buildParser(s);
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
     Parser parser = buildParser(s);
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
    Parser parser = buildParser(s);
    parser.parse();
  }
  
  @Test
  public void returnDeleteStatement() throws Exception {
    String s = buildString
    ("fun void r() {",
     " delete d",
     " return f",
     "}");
    Parser parser = buildParser(s);
    parser.parse();
  }


  
  //------------------------------------------------------------
  // NEGATIVE TEST CASES
  //------------------------------------------------------------
  
  @Test
  public void statementOutsideOfFunction() throws Exception {
    String s = "var v1 = 0";
    Parser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e) {
      // can check message here if desired
      // e.g., assertEquals("...", e.getMessage());
    }
  }

  @Test
  public void functionWithoutReturnType() throws Exception {
    String s = "fun main() {}";
    Parser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e) {
      // can check message here if desired
      // e.g., assertEquals("...", e.getMessage());
    }
  }

  @Test
  public void functionWithoutClosingBrace() throws Exception {
    String s = "fun void main() {";
    Parser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e) {
      // can check message here if desired
      // e.g., assertEquals("...", e.getMessage());
    }
  }
  
  /* add additional negative test cases here */ 
  
  @Test
  public void lvalueWithoutId() throws Exception {
    String s = "fun void main() { l.8 = 8 }";
    Parser parser = buildParser(s);
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
    Parser parser = buildParser(s);
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
    Parser parser = buildParser(s);
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
    Parser parser = buildParser(s);
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
    Parser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e){
      // assertEquals()
    }
  }


  /* Tests for const */

  @Test
  public void basicConstVarDecl() throws Exception {
    String s = "fun void main() { const var x = 0 }";
    Parser parser = buildParser(s);
    parser.parse();
  }

  @Test
  public void moreStmtsConst() throws Exception {
    String s = buildString 
    ("fun void main() {",
     " var x = 0",
     " const var k = 12",
     "}");
    Parser parser = buildParser(s);
    parser.parse();
  }


  @Test
  public void invalidConstVarAssignment() throws Exception {
    String s = "fun void main() { const x = 0 }";
    Parser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e){
      //assertEquals()
    }
  }

  @Test
  public void invalidConst() throws Exception {
    String s = "fun void main() { while const { var x = 0 } }";
    Parser parser = buildParser(s);
    try {
      parser.parse();
      fail("syntax error not detected");
    } catch(MyPLException e){
      //assertEquals()
    }
  }

}
