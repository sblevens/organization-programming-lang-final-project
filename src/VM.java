/*
 * File: VM.java
 * Date: Spring 2022
 * Auth: Sami Blevens
 * Desc: A bare-bones MyPL Virtual Machine. The architecture is based
 *       loosely on the architecture of the Java Virtual Machine
 *       (JVM).  Minimal error checking is done except for runtime
 *       program errors, which include: out of bound indexes,
 *       dereferencing a nil reference, and invalid value conversion
 *       (to int and double).
 */


import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Scanner;



/*----------------------------------------------------------------------

  TODO: Your main job for HW-6 is to finish the VM implementation
        below by finishing the handling of each instruction.

        Note that PUSH, NOT, JMP, READ, FREE, and NOP (trivially) are
        completed already to help get you started. 

        Be sure to look through OpCode.java to get a basic idea of
        what each instruction should do as well as the unit tests for
        additional details regarding the instructions.

        Note that you only need to perform error checking if the
        result would lead to a MyPL runtime error (where all
        compile-time errors are assumed to be found already). This
        includes things like bad indexes (in GETCHR), dereferencing
        and/or using a NIL_OBJ (see the ensureNotNil() helper
        function), and converting from strings to ints and doubles. An
        error() function is provided to help generate a MyPLException
        for such cases.

----------------------------------------------------------------------*/ 


class VM {

  // set to true to print debugging information
  private boolean DEBUG = false;
  
  // the VM's heap (free store) accessible via object-id
  private Map<Integer,Map<String,Object>> heap = new HashMap<>();
  
  // next available object-id
  private int objectId = 1111;
  
  // the frames for the program (one frame per function)
  private Map<String,VMFrame> frames = new HashMap<>();

  // the VM call stack
  private Deque<VMFrame> frameStack = new ArrayDeque<>();

  
  /**
   * For representing "nil" as a value
   */
  public static String NIL_OBJ = new String("nil");
  

  /** 
   * Add a frame to the VM's list of known frames
   * @param frame the frame to add
   */
  public void add(VMFrame frame) {
    frames.put(frame.functionName(), frame);
  }

  /**
   * Turn on/off debugging, which prints out the state of the VM prior
   * to each instruction. 
   * @param debug set to true to turn on debugging (by default false)
   */
  public void setDebug(boolean debug) {
    DEBUG = debug;
  }

  /**
   * Run the virtual machine
   */
  public void run() throws MyPLException {

    // grab the main stack frame
    if (!frames.containsKey("main"))
      throw MyPLException.VMError("No 'main' function");
    VMFrame frame = frames.get("main").instantiate();
    frameStack.push(frame);
    
    // run loop (keep going until we run out of frames or
    // instructions) note that we assume each function returns a
    // value, and so the second check below should never occur (but is
    // useful for testing, etc).
    while (frame != null && frame.pc < frame.instructions.size()) {
      // get next instruction
      VMInstr instr = frame.instructions.get(frame.pc);
      // increment instruction pointer
      ++frame.pc;

      // For debugging: to turn on the following, call setDebug(true)
      // on the VM.
      if (DEBUG) {
        System.out.println();
        System.out.println("\t FRAME........: " + frame.functionName());
        System.out.println("\t PC...........: " + (frame.pc - 1));
        System.out.println("\t INSTRUCTION..: " + instr);
        System.out.println("\t OPERAND STACK: " + frame.operandStack);
        System.out.println("\t HEAP ........: " + heap);
      }

      
      //------------------------------------------------------------
      // Consts/Vars
      //------------------------------------------------------------

      if (instr.opcode() == OpCode.PUSH) {
        frame.operandStack.push(instr.operand());
      }

      else if (instr.opcode() == OpCode.POP) {
        frame.operandStack.pop();
      }

      else if (instr.opcode() == OpCode.LOAD) {
        frame.operandStack.push(frame.variables.get((int)instr.operand()));
      }
        
      else if (instr.opcode() == OpCode.STORE) {
        if(frame.variables.size() <= (int)instr.operand()){
          frame.variables.add(frame.operandStack.pop());
        } else {
          frame.variables.set((int)instr.operand(),frame.operandStack.pop());
        }
      }

      
      //------------------------------------------------------------
      // Ops
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.ADD) {
        // pop x and y off stack, push (y + x) onto stack
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if(x instanceof Integer){
          frame.operandStack.push((int)y + (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y + (double)x);
        } else if(x instanceof String || y instanceof String){
          frame.operandStack.push(y.toString() + x.toString());
        }
        
      }

      else if (instr.opcode() == OpCode.SUB) {
        // 
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if(x instanceof Integer){
          frame.operandStack.push((int)y - (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y - (double)x);
        }
      }

      else if (instr.opcode() == OpCode.MUL) {
        // 
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if(x instanceof Integer){
          frame.operandStack.push((int)y * (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y * (double)x);
        }
      }

      else if (instr.opcode() == OpCode.DIV) {
        // 
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if(x instanceof Integer){
          frame.operandStack.push((int)y / (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y / (double)x);
        }
      }

      else if (instr.opcode() == OpCode.MOD) {
        // 
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        frame.operandStack.push((int)y % (int)x);
      }

      else if (instr.opcode() == OpCode.AND) {
        // pop bools x and y, push (y and x)
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        frame.operandStack.push((boolean)y && (boolean)x);
      }

      else if (instr.opcode() == OpCode.OR) {
        // 
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        frame.operandStack.push((boolean)y || (boolean)x);
      }

      else if (instr.opcode() == OpCode.NOT) {
        Object operand = frame.operandStack.pop();
        ensureNotNil(frame, operand);
        frame.operandStack.push(!(boolean)operand);
      }

      else if (instr.opcode() == OpCode.CMPLT) {
        // pop x and y off stack, push (y < x)
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if(x instanceof Integer){
          frame.operandStack.push((int)y < (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y < (double)x);
        } else if(x instanceof String || y instanceof String){
          int comp = y.toString().compareTo(x.toString());
          boolean strComp = false;
          if(comp < 0){
            strComp = true;
          }
          frame.operandStack.push(strComp);
        }
      }

      else if (instr.opcode() == OpCode.CMPLE) {
        // pop x and y off stack, push (y <= x)
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if(x instanceof Integer){
          frame.operandStack.push((int)y <= (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y <= (double)x);
        } else if(x instanceof String || y instanceof String){
          int comp = y.toString().compareTo(x.toString());
          boolean strComp = false;
          if(comp <= 0){
            strComp = true;
          }
          frame.operandStack.push(strComp);
        }
      }

      else if (instr.opcode() == OpCode.CMPGT) {
        // TODO
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if(x instanceof Integer){
          frame.operandStack.push((int)y > (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y > (double)x);
        } else if(x instanceof String || y instanceof String){
          int comp = y.toString().compareTo(x.toString());
          boolean strComp = false;
          if(comp > 0){
            strComp = true;
          }
          frame.operandStack.push(strComp);
        }
      }

      else if (instr.opcode() == OpCode.CMPGE) {
        // TODO
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        ensureNotNil(frame, x);
        ensureNotNil(frame, y);
        if(x instanceof Integer){
          frame.operandStack.push((int)y >= (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y >= (double)x);
        } else if(x instanceof String || y instanceof String){
          int comp = y.toString().compareTo(x.toString());
          boolean strComp = false;
          if(comp >= 0){
            strComp = true;
          }
          frame.operandStack.push(strComp);
        }
      }

      else if (instr.opcode() == OpCode.CMPEQ) {
        // TODO
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if(x instanceof String || y instanceof String){
          int comp = y.toString().compareTo(x.toString());
          boolean strComp = false;
          if(comp == 0){
            strComp = true;
          }
          frame.operandStack.push(strComp);
        }
        else if(x instanceof Integer){
          frame.operandStack.push((int)y == (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y == (double)x);
        }
      }

      else if (instr.opcode() == OpCode.CMPNE) {
        // TODO
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        if(x instanceof String || y instanceof String){
          int comp = y.toString().compareTo(x.toString());
          boolean strComp = false;
          if(comp != 0){
            strComp = true;
          }
          frame.operandStack.push(strComp);
        }
        else if(x instanceof Integer){
          frame.operandStack.push((int)y != (int)x);
        } else if(x instanceof Double){
          frame.operandStack.push((double)y != (double)x);
        }
      }

      else if (instr.opcode() == OpCode.NEG) {
        // TODO
        Object x = frame.operandStack.pop();
        ensureNotNil(frame, x);
        if(x instanceof Integer){
          frame.operandStack.push(-(int)x);
        } else if(x instanceof Double){
          frame.operandStack.push(-(double)x);
        }
      }

      
      //------------------------------------------------------------
      // Jumps
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.JMP) {
        frame.pc = (int)instr.operand();
      }

      else if (instr.opcode() == OpCode.JMPF) {
        // 
        Object x = frame.operandStack.pop();
        if(!(boolean)x){
          frame.pc = (int)instr.operand();
        }
      }
        
      //------------------------------------------------------------
      // Functions
      //------------------------------------------------------------

      else if (instr.opcode() == OpCode.CALL) {
        // TODO: 
        // (1) get frame and instantiate a new copy
        Object name = instr.operand();
        if (!frames.containsKey(name.toString()))
          throw MyPLException.VMError("No " + name.toString() + " function");
        VMFrame newFrame = frames.get(name.toString()).instantiate();
        // (2) Pop argument values off stack and push into the newFrame
        for(int i=0; i < newFrame.argCount(); i++){
          newFrame.operandStack.push(frame.operandStack.pop());
        }
        // (3) Push the new frame onto frame stack
        frameStack.push(newFrame);
        // (4) Set the new frame as the current frame
        frame = newFrame;
      }
        
      else if (instr.opcode() == OpCode.VRET) {
        // TODO:
        // (1) pop return value off of stack
        Object returnVal = frame.operandStack.pop();
        // (2) remove the frame from the current frameStack
        frameStack.pop();
        // (3) set frame to the frame on the top of the stack
        // frameStack.push(frame);
        frame = frameStack.peek();
        // (4) push the return value onto the operand stack of the frame
        if(frame != null){
          frame.operandStack.push(returnVal);
        }
        
      }
        
      //------------------------------------------------------------
      // Built-ins
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.WRITE) {
        // pop x, write to stdout
        Object operand = frame.operandStack.pop();
        // ensureNotNil(frame, operand);
        System.out.print(operand.toString());
      }

      else if (instr.opcode() == OpCode.READ) {
        Scanner s = new Scanner(System.in);
        frame.operandStack.push(s.nextLine());
      }

      else if (instr.opcode() == OpCode.LEN) {
        // pop (string) x, push x.length()
        Object operand = frame.operandStack.pop();
        ensureNotNil(frame, operand);
        int len = operand.toString().length();
        frame.operandStack.push(len);
      }

      else if (instr.opcode() == OpCode.GETCHR) {
        // pop (string) x, pop y, push x.substring(y, y+1)
        Object x = frame.operandStack.pop();
        ensureNotNil(frame,x);
        int y = (int)frame.operandStack.pop();
        ensureNotNil(frame,y);
        if(y < 0 || y >= x.toString().length()){
          error("invalid index "+ y + "for string "+ x.toString(),frame);
        }
        frame.operandStack.push(x.toString().substring(y, y + 1));
      }

      else if (instr.opcode() == OpCode.TOINT) {
        // pop x, push x as an integer
        Object x = frame.operandStack.pop();
        ensureNotNil(frame,x);
        try {
          double i = Double.parseDouble(x.toString());
          frame.operandStack.push((int)i);
        } catch(Exception e) {
          error("incompatible string " + x.toString() + " to parse as int",frame);
        }
      }

      else if (instr.opcode() == OpCode.TODBL) {
        // pop x, push x as a double
        Object x = frame.operandStack.pop();
        ensureNotNil(frame,x);
        try {
          frame.operandStack.push(Double.parseDouble(x.toString()));
        } catch(Exception e) {
          error("incompatible string " + x.toString() + " to parse as double",frame);
        }
        
      }

      else if (instr.opcode() == OpCode.TOSTR) {
        // pop x, push x.toString()
        Object x = frame.operandStack.pop();
        ensureNotNil(frame,x);
        frame.operandStack.push(x.toString());
      }

      //------------------------------------------------------------
      // Heap related
      //------------------------------------------------------------

      else if (instr.opcode() == OpCode.ALLOC) {      
        // allocate obj w/ atts-list, push y (oid)
        List<String> f = (List<String>)instr.operand();
        int oid = objectId;
        objectId = objectId + 1;

        //heap is Map<Integer,Map<String,Object>>
        Map<String,Object> fields = new HashMap<>();
        for(String s: f){
          fields.put(s,null);
        }

        heap.put(oid,fields);
        frame.operandStack.push(oid);
      }

      else if (instr.opcode() == OpCode.FREE) {
        // pop the oid to 
        Object oid = frame.operandStack.pop();
        ensureNotNil(frame, oid);
        // remove the object with oid from the heap
        heap.remove((int)oid);
      }

      else if (instr.opcode() == OpCode.SETFLD) {
        // set field f: pop x and y, set obj(y).f = x
        Object f = instr.operand();
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        //ensureNotNil(frame,x);
        ensureNotNil(frame,y);

        Map<String,Object> obj = heap.get((int)y);
        obj.put(f.toString(),x);
      }

      else if (instr.opcode() == OpCode.GETFLD) {      
        // get field f: pop x, push obj(x).f value
        Object x = frame.operandStack.pop();
        ensureNotNil(frame,x);
        Object f = instr.operand();

        Map<String,Object> obj = heap.get((int)x);
        if(obj == null){
          error("invalid heap access",frame);
        }
        frame.operandStack.push(obj.get(f.toString()));
      }

      //------------------------------------------------------------
      // Special instructions
      //------------------------------------------------------------
        
      else if (instr.opcode() == OpCode.DUP) {
        // pop x, push x, push x
        Object x = frame.operandStack.pop();
        frame.operandStack.push(x);
        frame.operandStack.push(x);
      }

      else if (instr.opcode() == OpCode.SWAP) {
        // pop x, pop y, push x, push y
        Object x = frame.operandStack.pop();
        Object y = frame.operandStack.pop();
        frame.operandStack.push(x);
        frame.operandStack.push(y);
      }

      else if (instr.opcode() == OpCode.NOP) {
        // do nothing
      }

    }
  }

  
  // to print the lists of instructions for each VM Frame
  @Override
  public String toString() {
    String s = "";
    for (Map.Entry<String,VMFrame> e : frames.entrySet()) {
      String funName = e.getKey();
      s += "Frame '" + funName + "'\n";
      List<VMInstr> instructions = e.getValue().instructions;      
      for (int i = 0; i < instructions.size(); ++i) {
        VMInstr instr = instructions.get(i);
        s += "  " + i + ": " + instr + "\n";
      }
      // s += "\n";
    }
    return s;
  }

  
  //----------------------------------------------------------------------
  // HELPER FUNCTIONS
  //----------------------------------------------------------------------

  // error
  private void error(String m, VMFrame f) throws MyPLException {
    int pc = f.pc - 1;
    VMInstr i = f.instructions.get(pc);
    String name = f.functionName();
    m += " (in " + name + " at " + pc + ": " + i + ")";
    throw MyPLException.VMError(m);
  }

  // error if given value is nil
  private void ensureNotNil(VMFrame f, Object v) throws MyPLException {
    if (v == NIL_OBJ)
      error("Nil reference", f);
  }
  
  
}
