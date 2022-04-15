/*
 * File: Lexer.java
 * Date: Spring 2022
 * Auth: Sami Blevens
 * Desc: HW2 - implement nextToken() -
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;


public class Lexer {

  private BufferedReader buffer; // handle to input stream
  private int line = 1;          // current line number
  private int column = 0;        // current column number


  //--------------------------------------------------------------------
  // Constructor
  //--------------------------------------------------------------------
  
  public Lexer(InputStream instream) {
    buffer = new BufferedReader(new InputStreamReader(instream));
  }


  //--------------------------------------------------------------------
  // Private helper methods
  //--------------------------------------------------------------------

  // Returns next character in the stream. Returns -1 if end of file.
  private int read() throws MyPLException {
    try {
      return buffer.read();
    } catch(IOException e) {
      error("read error", line, column + 1);
    }
    return -1;
  }

  
  // Returns next character without removing it from the stream.
  private int peek() throws MyPLException {
    int ch = -1;
    try {
      buffer.mark(1);
      ch = read();
      buffer.reset();
    } catch(IOException e) {
      error("read error", line, column + 1);
    }
    return ch;
  }


  // Print an error message and exit the program.
  private void error(String msg, int line, int column) throws MyPLException {
    msg = msg + " at line " + line + ", column " + column;
    throw MyPLException.LexerError(msg);
  }

  
  // Checks for whitespace 
  public static boolean isWhitespace(int ch) {
    return Character.isWhitespace((char)ch);
  }

  
  // Checks for digit
  private static boolean isDigit(int ch) {
    return Character.isDigit((char)ch);
  }

  
  // Checks for letter
  private static boolean isLetter(int ch) {
    return Character.isLetter((char)ch);
  }

  
  // Checks if given symbol
  private static boolean isSymbol(int ch, char symbol) {
    return (char)ch == symbol;
  }

  
  // Checks if end-of-file
  private static boolean isEOF(int ch) {
    return ch == -1;
  }
  

  //--------------------------------------------------------------------
  // Public next_token function
  //--------------------------------------------------------------------
  
  // Returns next token in input stream
  public Token nextToken() throws MyPLException {
    // TODO: implement nextToken()
    column++;
  
    int next = read();


    //check if whitespace
    while(isWhitespace(next)){
      if(isSymbol(next,'\n')){
        line++;
        column = 1;
      } else {
        column++;
      }
      next = read();
    }

    //check if comment
    if(isSymbol(next,'#')){
      // int com = next;
      while(isSymbol(next,'#')){
        // System.out.println("found a comment");
        while(!isSymbol(next,'\n')){
          next = read();
          column++;
        }
        line++;
        column = 1;
        next = read();
        // com = next;
        if(isWhitespace(next)){
          while(isWhitespace(next) || isSymbol(next,'\n')){
            if(isSymbol(next,'\n')){
              line++;
              column = 1;
            }
            next = read();
            column++;
          }
        }
      }
      //check if whitespace
      while(isWhitespace(next)){
        next = read();
        column++;
      }
    }

    //check if end of file
    if(isEOF(next)){
      return new Token(TokenType.EOS, "end-of-file", line, column);
    }

    //check comparators
    // ==, !=, >=, <=, <, >
    if(isSymbol(next,'<')){
      int startCol = column;
      if(isSymbol(peek(),'=')){
        read();
        column++;
        return new Token(TokenType.LESS_THAN_EQUAL,"<=",line,startCol);
      } 
      // else if(!isLetter(peek()) && !isDigit(peek()) && !isWhitespace(peek())){
      //   column++;
      //   error("expecting '=', found '"+ (char)peek() + "'",line,column);
      // }
      return new Token(TokenType.LESS_THAN,"<",line,startCol);
    }
    if(isSymbol(next,'>')){
      int startCol = column;
      if(isSymbol(peek(),'=')){
        read();
        column++;
        return new Token(TokenType.GREATER_THAN_EQUAL,">=",line,startCol);
      } 
      // else if(!isLetter(peek()) && !isDigit(peek()) && !isWhitespace(peek())){
      //   column++;
      //   error("expecting '=', found '"+ (char)peek() + "'",line,column);
      // }
      return new Token(TokenType.GREATER_THAN,">",line,startCol);
    }
    if(isSymbol(next,'=')){
      int startCol = column;
      if(isSymbol(peek(),'=')){
        read();
        column++;
        return new Token(TokenType.EQUAL,"==",line,startCol);
      } 
      // else if(!isLetter(peek()) && !isDigit(peek()) && !isWhitespace(peek())){
      //   column++;
      //   error("expecting '=', found '"+ (char)peek() + "'",line,column);
      // }
      //check assignment
      return new Token(TokenType.ASSIGN,"=",line,startCol);
    }
    if(isSymbol(next,'!')){
      int startCol = column;
      if(isSymbol(peek(),'=')){
        read();
        column++;
        return new Token(TokenType.NOT_EQUAL,"!=",line,startCol);
      } else if(!isLetter(peek()) && !isDigit(peek()) && !isWhitespace(peek())){
        column++;
        error("expecting '=', found '"+ (char)peek() + "'",line,column);
      }
      //check boolean op !
      return new Token(TokenType.NOT,"!",line,startCol);
    }

    //check primivite values
    //check char vals
    if(isSymbol(next,'\'')){
      int startCol = column;
      int val = read();
      if(isSymbol(val,'\'') || isEOF(peek())){
        error("empty character",line,column);
      }
      column++;
      String valChar="";
      if(isSymbol(val,'\n')){
        error("found newline in character",line,column);
      }
      if(isSymbol(val,'\\')){
        char peek = (char)peek();
        // if(isSymbol(peek(),'\\')){
        //   // \\
        //   read();
        //   column++;
        //   if(isSymbol(peek(),'\'')){
        //     valChar = "\\\\";
        //   } else if((char)peek() == 'n' || (char)peek() == 't'){
        //     valChar = "\\\\" + (char)read();
        //   } else {
        //     error("unknown escape sequence",line,column);
        //   }
        // } else if(((char)peek() == '\'') || ((char)peek() == '\"')){
        //   // \' , \",
        //   valChar = "\\" + (char)read();
        // } else if((char)peek() == 'n'){
        //   // \n
        //   error("found newline in character",line,column);
        // } else {
        //   error("escape character not allowed",line,column);
        // }

        if((peek =='t') || (peek=='n') || (peek=='b') || (peek=='r') || (peek=='f') || (peek=='\'') || (peek=='\"') || (peek=='\\')){
          valChar = "\\" + (char)read();
          column++;
        } else {
          error("unknown escape sequence",line,column);
        }
      } else {
        valChar = ""+(char)val;
      }
      if(!isSymbol(peek(),'\'')){
        //error
        error("expecting ' found, '"+ (char)peek() + "'",line,column);
      }
      read();
      column++;
      return new Token(TokenType.CHAR_VAL,valChar,line,startCol);
    }



    //check string vals
    if(isSymbol(next,'\"')){
      int startCol = column;
      String valString = "";
      while(!isSymbol(peek(),'\"')){
        if(isEOF(peek())){
          error("found end-of-file in string",line,column);
        }
        column++;
        if(isSymbol(peek(),'\n')){
          error("found newline within string",line,column);
        }
        valString = valString + (char)read();
        
      }
      read();
      column++;
      return new Token(TokenType.STRING_VAL,valString,line,startCol);
    }

    //check int / double vals
    if(isDigit(next)){
      int startCol = column;
      String i = "";
      i = i + (char)next;
      while(isDigit(peek())){
        i = i + (char)read();
        column++;
      }
      String start = i;
      if(isSymbol(peek(),'.')){
        //double
        i = i + (char)read();
        column++;
        if(isDigit(peek())){
          while(isDigit(peek())){
            i = i + (char)read();
            column++;
          }
        } else {
          error("missing decimal digit in double value '"+ i + "'",line,startCol);
        }
        if(isSymbol(peek(),'.')){
          error("too many decimal points in double value '"+ i + "'",line,startCol);
        }
        if(isLetter(peek())){
          error("illegal characters in number",line,startCol);
        }
        if((start.length() > 1) && (char)next == '0'){
          error("leading zero in '" + i + "'",line,startCol);
        }
        return new Token(TokenType.DOUBLE_VAL,i,line,startCol);
      } else {
        //int
        if(isLetter(peek())){
          error("illegal characters in number",line,startCol);
        }
        if((i.length() > 1) && (char)next == '0'){
          error("leading zero in '" + i + "'",line,startCol);
        }
        return new Token(TokenType.INT_VAL,i,line,startCol);
      }
    }

    //reserved words
    if(isLetter(next)){
      int startCol = column;
      String str = "";
      str = str + (char)next;
      while(isLetter(peek()) || isSymbol(peek(),'_') || isDigit(peek())){
        str = str + (char)read();
        column++;
      }
      //bool val
      if(str.equals("true") || str.equals("false")){
        return new Token(TokenType.BOOL_VAL,str,line,startCol);
      }
      //reserved words
      if(str.equals("var")){
        return new Token(TokenType.VAR,str,line,startCol);
      }
      if(str.equals("type")){
        return new Token(TokenType.TYPE,str,line,startCol);
      }
      if(str.equals("while")){
        return new Token(TokenType.WHILE,str,line,startCol);
      }
      if(str.equals("for")){
        return new Token(TokenType.FOR,str,line,startCol);
      }
      if(str.equals("from")){
        return new Token(TokenType.FROM,str,line,startCol);
      }
      if(str.equals("upto")){
        return new Token(TokenType.UPTO,str,line,startCol);
      }
      if(str.equals("downto")){
        return new Token(TokenType.DOWNTO,str,line,startCol);
      }
      if(str.equals("if")){
        return new Token(TokenType.IF,str,line,startCol);
      }
      if(str.equals("elif")){
        return new Token(TokenType.ELIF,str,line,startCol);
      }
      if(str.equals("else")){
        return new Token(TokenType.ELSE,str,line,startCol);
      }
      if(str.equals("fun")){
        return new Token(TokenType.FUN,str,line,startCol);
      }
      if(str.equals("new")){
        return new Token(TokenType.NEW,str,line,startCol);
      }
      if(str.equals("delete")){
        return new Token(TokenType.DELETE,str,line,startCol);
      }
      if(str.equals("return")){
        return new Token(TokenType.RETURN,str,line,startCol);
      }
      if(str.equals("nil")){
        return new Token(TokenType.NIL,str,line,startCol);
      }
      if(str.equals("const")){
        return new Token(TokenType.CONST,str,line,startCol);
      }
      //data types
      if(str.equals("int")){
        return new Token(TokenType.INT_TYPE,str,line,startCol);
      }
      if(str.equals("double")){
        return new Token(TokenType.DOUBLE_TYPE,str,line,startCol);
      }
      if(str.equals("char")){
        return new Token(TokenType.CHAR_TYPE,str,line,startCol);
      }
      if(str.equals("string")){
        return new Token(TokenType.STRING_TYPE,str,line,startCol);
      }
      if(str.equals("bool")){
        return new Token(TokenType.BOOL_TYPE,str,line,startCol);
      }
      if(str.equals("void")){
        return new Token(TokenType.VOID_TYPE,str,line,startCol);
      }
      //bool operators
      if(str.equals("and")){
        return new Token(TokenType.AND,str,line,startCol);
      }
      if(str.equals("or")){
        return new Token(TokenType.OR,str,line,startCol);
      }
      if(str.equals("not")){
        return new Token(TokenType.NOT,str,line,startCol);
      }
      if(str.equals("neg")){
        return new Token(TokenType.NEG,str,line,startCol);
      }
      //else must be an id
      return new Token(TokenType.ID,str,line,startCol);
    }

    
    //check basic symbol tokens
    //,, ., +, -, *, /, %, {, }, (, ),
    if(isSymbol(next,',')){
      return new Token(TokenType.COMMA, ",",line,column);
    }else if(isSymbol(next,'.')){
      return new Token(TokenType.DOT,".",line,column);
    }else if(isSymbol(next,'+')){
      return new Token(TokenType.PLUS,"+",line,column);
    }else if(isSymbol(next,'-')){
      return new Token(TokenType.MINUS,"-",line,column);
    }else if(isSymbol(next,'*')){
      return new Token(TokenType.MULTIPLY,"*",line,column);
    }else if(isSymbol(next,'/')){
      return new Token(TokenType.DIVIDE,"/",line,column);
    }else if(isSymbol(next,'%')){
      return new Token(TokenType.MODULO,"%",line,column);
    }else if(isSymbol(next,'{')){
      return new Token(TokenType.LBRACE,"{",line,column);
    }else if(isSymbol(next,'}')){
      return new Token(TokenType.RBRACE,"}",line,column);
    }else if(isSymbol(next,'(')){
      return new Token(TokenType.LPAREN,"(",line,column);
    }else if(isSymbol(next,')')){
      return new Token(TokenType.RPAREN,")",line,column);
    }else{
      error("invalid symbol '"+ (char)next+ "'",line,column);
      return null;
    }


    // Note: Use the error() function to report errors, e.g.:
    //         error("error msg goes here", line, column);

    // Reminder: Comment your code and fill in the file header comment
    // above
    
    // The following returns the end-of-file token, you'll need to
    // remove this to implement nextToken (it is only here so that the
    // program can compile initially)
    
    // return new Token(TokenType.CHAR_TYPE, "test-not-end-of-file", line, column);
  }

}
