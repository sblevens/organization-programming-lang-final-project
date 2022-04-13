/*
 * File: CodeGenerator.java
 * Date: Spring 2022
 * Auth: Sami Blevens
 * Desc: Code Generator for HW-7
 */

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class CodeGenerator implements Visitor {

  // the user-defined type and function type information
  private TypeInfo typeInfo = null;

  // the virtual machine to add the code to
  private VM vm = null;

  // the current frame
  private VMFrame currFrame = null;

  // mapping from variables to their indices (in the frame)
  private Map<String,Integer> varMap = null;

  // the current variable index (in the frame)
  private int currVarIndex = 0;

  // to keep track of the typedecl objects for initialization
  Map<String,TypeDecl> typeDecls = new HashMap<>();


  //----------------------------------------------------------------------
  // HELPER FUNCTIONS
  //----------------------------------------------------------------------
  
  // helper function to clean up uneeded NOP instructions
  private void fixNoOp() {
    int nextIndex = currFrame.instructions.size();
    // check if there are any instructions
    if (nextIndex == 0)
      return;
    // get the last instuction added
    VMInstr instr = currFrame.instructions.get(nextIndex - 1);
    // check if it is a NOP
    if (instr.opcode() == OpCode.NOP)
      currFrame.instructions.remove(nextIndex - 1);
  }

  private void fixCallStmt(Stmt s) {
    // get the last instuction added
    if (s instanceof CallExpr) {
      VMInstr instr = VMInstr.POP();
      instr.addComment("clean up call return value");
      currFrame.instructions.add(instr);
    }

  }
  
  //----------------------------------------------------------------------  
  // Constructor
  //----------------------------------------------------------------------

  public CodeGenerator(TypeInfo typeInfo, VM vm) {
    this.typeInfo = typeInfo;
    this.vm = vm;
  }

  
  //----------------------------------------------------------------------
  // VISITOR FUNCTIONS
  //----------------------------------------------------------------------
  
  public void visit(Program node) throws MyPLException {

    // store UDTs for later
    for (TypeDecl tdecl : node.tdecls) {
      // add a mapping from type name to the TypeDecl
      typeDecls.put(tdecl.typeName.lexeme(), tdecl);
    }
    // only need to translate the function declarations
    for (FunDecl fdecl : node.fdecls)
      fdecl.accept(this);
  }

  public void visit(TypeDecl node) throws MyPLException {
    // Intentionally left blank -- nothing to do here
  }
  
  public void visit(FunDecl node) throws MyPLException {
    // TODO: 
    // 1. create a new frame for the function and add it to the VM - assign to currFrame
    VMFrame newFrame = new VMFrame(node.funName.lexeme(),node.params.size());
    vm.add(newFrame);
    currFrame = newFrame;
    // 2. create a variable mapping for the frame
    varMap = new HashMap<String,Integer>();
    currVarIndex = 0;
    // 3. store args from operand stack
    for(FunParam p: node.params){
      varMap.put(p.paramName.lexeme(),currVarIndex);
      //currFrame.instructions.add(VMInstr.POP())
      currFrame.instructions.add(VMInstr.STORE(currVarIndex));
      currVarIndex++;
    }
    // 4. visit statement nodes
    boolean isReturn = false;
    for(Stmt s: node.stmts){
      if(s instanceof ReturnStmt){
        isReturn = true;
      }
      s.accept(this);
      fixCallStmt(s);
    }
    // 5. check to see if the last statement was a return (if not, add
    //    return nil)
    if(!isReturn){
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
      currFrame.instructions.add(VMInstr.VRET());
    }
  }
  
  public void visit(VarDeclStmt node) throws MyPLException {
    // TODO
    node.expr.accept(this);
    varMap.put(node.varName.lexeme(),currVarIndex);
    // is not already in varmap bc declaration
    currFrame.instructions.add(VMInstr.STORE(currVarIndex));
    currVarIndex = currVarIndex + 1;
  }
  
  public void visit(AssignStmt node) throws MyPLException {
    int size = node.lvalue.size();
    node.expr.accept(this);
    //lvalue
    if(size > 1){
      // load lvalue.get(0) varmap
      int i = varMap.get(node.lvalue.get(0).lexeme());
      currFrame.instructions.add(VMInstr.LOAD(i));

      for(int j = 1; j< size-1; j++){
        // getfld next
        currFrame.instructions.add(VMInstr.GETFLD(node.lvalue.get(j).lexeme()));
      }
      //at end swap
      currFrame.instructions.add(VMInstr.SWAP());
      currFrame.instructions.add(VMInstr.SETFLD(node.lvalue.get(size-1).lexeme()));
    } else {
      int i = varMap.get(node.lvalue.get(0).lexeme());
      currFrame.instructions.add(VMInstr.STORE(i));
    }
  }
  
  public void visit(CondStmt node) throws MyPLException {
    // TODO
    node.ifPart.cond.accept(this);
    //placeholder to either end or elif or else
    currFrame.instructions.add(VMInstr.JMPF(-1));
    int ifJump = currFrame.instructions.size() - 1;
    for(Stmt s: node.ifPart.stmts){
      s.accept(this);
    }
    //jump to end 
    currFrame.instructions.add(VMInstr.JMP(-1));
    int endJumpIndex = currFrame.instructions.size() - 1;
    int elifJump = -1;
    List<Integer> endElifJumps = new ArrayList<>();
    if(node.elifs.size() > 0){
      int elifStart = currFrame.instructions.size();
      currFrame.instructions.get(ifJump).updateOperand(elifStart);
      for(BasicIf b: node.elifs){
        elifStart = currFrame.instructions.size() - 1;
        if(elifJump != -1){
          // then this is not the first elif section - 
          // have to set previous elif jump if false to this new elif Start
          currFrame.instructions.get(elifJump).updateOperand(elifStart);
        }
        
        b.cond.accept(this);
        //placeholder
        currFrame.instructions.add(VMInstr.JMPF(-1));
        elifJump = currFrame.instructions.size() - 1;
        for(Stmt s: b.stmts){
          s.accept(this);
        }
        //jump to end 
        currFrame.instructions.add(VMInstr.JMP(-1));
        endElifJumps.add(currFrame.instructions.size() - 1);
      }
    }
    if(node.elseStmts != null){
      int elseJump = currFrame.instructions.size();
      if(currFrame.instructions.get(ifJump).operand().toString().equals("-1")){
        currFrame.instructions.get(ifJump).updateOperand(elseJump);
      } else {
        currFrame.instructions.get(elifJump).updateOperand(elseJump);
      }
      for(Stmt s: node.elseStmts){
        s.accept(this);
      }
    }
    //set all end jumps to nop.
    currFrame.instructions.add(VMInstr.NOP());
    int nop = currFrame.instructions.size() - 1;
    if(node.elseStmts == null){
      if(currFrame.instructions.get(ifJump).operand().toString().equals("-1")){
        currFrame.instructions.get(ifJump).updateOperand(nop);
      } else {
        currFrame.instructions.get(elifJump).updateOperand(nop);
      }
    }
    currFrame.instructions.get(endJumpIndex).updateOperand(nop);
    for(int i: endElifJumps){
      currFrame.instructions.get(i).updateOperand(nop);
    }
  }

  public void visit(WhileStmt node) throws MyPLException {
    //grab starting index
    int start = currFrame.instructions.size();
    //cond visitor
    node.cond.accept(this);
    //jump to end
    currFrame.instructions.add(VMInstr.JMPF(-1));
    int jump = currFrame.instructions.size() - 1;
    for(Stmt s: node.stmts){
      s.accept(this);
    }
    //jump to starting index
    currFrame.instructions.add(VMInstr.JMP(start));
    currFrame.instructions.add(VMInstr.NOP());
    int end = currFrame.instructions.size() - 1;
    currFrame.instructions.get(jump).updateOperand(end);
  }

  public void visit(ForStmt node) throws MyPLException {
    // TODO
    int index = currVarIndex;
    currVarIndex++;
    node.start.accept(this);
    varMap.put(node.varName.lexeme(),index);
    currFrame.instructions.add(VMInstr.STORE(index));


    currFrame.instructions.add(VMInstr.LOAD(index));
    int startIndex = currFrame.instructions.size() - 1;
    node.end.accept(this);
    if(node.upto){
      currFrame.instructions.add(VMInstr.CMPLE());
    } else {
      currFrame.instructions.add(VMInstr.CMPGE());
    }
    //placeholder - jump out of for loop
    currFrame.instructions.add(VMInstr.JMPF(-1));
    int jumpIndex = currFrame.instructions.size() - 1;

    for(Stmt s: node.stmts){
      s.accept(this);
    }

    currFrame.instructions.add(VMInstr.LOAD(index));
    currFrame.instructions.add(VMInstr.PUSH(1));
    if(node.upto){
      currFrame.instructions.add(VMInstr.ADD());
    } else {
      currFrame.instructions.add(VMInstr.SUB());
    }
    currFrame.instructions.add(VMInstr.STORE(index));


    currFrame.instructions.add(VMInstr.JMP(startIndex));
    currFrame.instructions.add(VMInstr.NOP());
    int nop = currFrame.instructions.size() - 1;
    currFrame.instructions.get(jumpIndex).updateOperand(nop);


  }
  
  public void visit(ReturnStmt node) throws MyPLException {
    if(node.expr != null){
      node.expr.accept(this);
    } else {
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
    }
    currFrame.instructions.add(VMInstr.VRET());

  }
  
  
  public void visit(DeleteStmt node) throws MyPLException {
    int index = varMap.get(node.varName.lexeme());
    currFrame.instructions.add(VMInstr.LOAD(index));
    currFrame.instructions.add(VMInstr.FREE());
  }

  public void visit(CallExpr node) throws MyPLException {
    // TODO: Finish the following (partially completed)

    // push args (in order)
    for (Expr arg : node.args)
      arg.accept(this);
    // built-in functions:
    if (node.funName.lexeme().equals("print")) {
      currFrame.instructions.add(VMInstr.WRITE());
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
    }
    else if (node.funName.lexeme().equals("read"))
      currFrame.instructions.add(VMInstr.READ());

    // TODO: add remaining built in functions
    else if(node.funName.lexeme().equals("length")){
      currFrame.instructions.add(VMInstr.LEN());
    }
    else if(node.funName.lexeme().equals("get")){
      currFrame.instructions.add(VMInstr.GETCHR());
    }
    else if(node.funName.lexeme().equals("stoi")){
      currFrame.instructions.add(VMInstr.TOINT());
    }
    else if(node.funName.lexeme().equals("stod")){
      currFrame.instructions.add(VMInstr.TODBL());
    }
    else if(node.funName.lexeme().equals("itos")){
      currFrame.instructions.add(VMInstr.TOSTR());
    }
    else if(node.funName.lexeme().equals("itod")){
      currFrame.instructions.add(VMInstr.TODBL());
    }
    else if(node.funName.lexeme().equals("dtos")){
      currFrame.instructions.add(VMInstr.TOSTR());
    }
    else if(node.funName.lexeme().equals("dtoi")){
      currFrame.instructions.add(VMInstr.LEN());
    }
    // user-defined functions
    else
      currFrame.instructions.add(VMInstr.CALL(node.funName.lexeme()));

  }
  
  public void visit(SimpleRValue node) throws MyPLException {
    if (node.value.type() == TokenType.INT_VAL) {
      int val = Integer.parseInt(node.value.lexeme());
      currFrame.instructions.add(VMInstr.PUSH(val));
    }
    else if (node.value.type() == TokenType.DOUBLE_VAL) {
      double val = Double.parseDouble(node.value.lexeme());
      currFrame.instructions.add(VMInstr.PUSH(val));
    }
    else if (node.value.type() == TokenType.BOOL_VAL) {
      if (node.value.lexeme().equals("true"))
        currFrame.instructions.add(VMInstr.PUSH(true));
      else
        currFrame.instructions.add(VMInstr.PUSH(false));        
    }
    else if (node.value.type() == TokenType.CHAR_VAL) {
      String s = node.value.lexeme();
      s = s.replace("\\n", "\n");
      s = s.replace("\\t", "\t");
      s = s.replace("\\r", "\r");
      s = s.replace("\\\\", "\\");
      currFrame.instructions.add(VMInstr.PUSH(s));
    }
    else if (node.value.type() == TokenType.STRING_VAL) {
      String s = node.value.lexeme();
      s = s.replace("\\n", "\n");
      s = s.replace("\\t", "\t");
      s = s.replace("\\r", "\r");
      s = s.replace("\\\\", "\\");
      currFrame.instructions.add(VMInstr.PUSH(s));
    }
    else if (node.value.type() == TokenType.NIL) {
      currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
    }
  }
  
  public void visit(NewRValue node) throws MyPLException {
    // TODO
    // check in type decls for same name
    TypeDecl t = typeDecls.get(node.typeName.lexeme());
    //get names of vars
    List<String> names = new ArrayList<>();
    for(VarDeclStmt v: t.vdecls){
      names.add(v.varName.lexeme());
    }
    // alloc
    currFrame.instructions.add(VMInstr.ALLOC(names));
    // push and set fields for vars
    for(VarDeclStmt s: t.vdecls){
      currFrame.instructions.add(VMInstr.DUP());
      s.expr.accept(this);
      currFrame.instructions.add(VMInstr.SETFLD(s.varName.lexeme()));
    }
  }
  
  public void visit(IDRValue node) throws MyPLException {
    // TODO
    //load node.path.get(0) - find in varMap
    int i = varMap.get(node.path.get(0).lexeme());
    currFrame.instructions.add(VMInstr.LOAD(i));
  
    //getfld node.path.get(1) , etc
    for(int j=1; j<node.path.size(); j++){
      currFrame.instructions.add(VMInstr.GETFLD(node.path.get(j).lexeme()));
    }

  }
      
  public void visit(NegatedRValue node) throws MyPLException {
    node.expr.accept(this);
    currFrame.instructions.add(VMInstr.NEG());
  }

  public void visit(Expr node) throws MyPLException {
    // TODO
    if(node.op != null){
      //has rhs of expr
      node.first.accept(this);
      node.rest.accept(this);

      if(node.op.lexeme().equals("+")){
        currFrame.instructions.add(VMInstr.ADD());
      }else if(node.op.lexeme().equals("-")){
        currFrame.instructions.add(VMInstr.SUB());
      }else if(node.op.lexeme().equals("*")){
        currFrame.instructions.add(VMInstr.MUL());
      }else if(node.op.lexeme().equals("/")){
        currFrame.instructions.add(VMInstr.DIV());
      }else if(node.op.lexeme().equals("%")){
        currFrame.instructions.add(VMInstr.MOD());
      }else if(node.op.lexeme().equals("and")){
        currFrame.instructions.add(VMInstr.AND());
      }else if(node.op.lexeme().equals("or")){
        currFrame.instructions.add(VMInstr.OR());
      }else if(node.op.lexeme().equals("<")){
        currFrame.instructions.add(VMInstr.CMPLT());
      }else if(node.op.lexeme().equals("<=")){
        currFrame.instructions.add(VMInstr.CMPLE());
      }else if(node.op.lexeme().equals(">")){
        currFrame.instructions.add(VMInstr.CMPGT());
      }else if(node.op.lexeme().equals(">=")){
        currFrame.instructions.add(VMInstr.CMPGE());
      }else if(node.op.lexeme().equals("==")){
        currFrame.instructions.add(VMInstr.CMPEQ());
      }else if(node.op.lexeme().equals("!=")){
        currFrame.instructions.add(VMInstr.CMPNE());
      }
    } else {
      node.first.accept(this);
    }

    //at end - if log neg. add not
    if(node.logicallyNegated){
      currFrame.instructions.add(VMInstr.NOT());
    }
  }

  public void visit(SimpleTerm node) throws MyPLException {
    // defer to contained rvalue
    node.rvalue.accept(this);
  }
  
  public void visit(ComplexTerm node) throws MyPLException {
    // defer to contained expression
    node.expr.accept(this);
  }

}
