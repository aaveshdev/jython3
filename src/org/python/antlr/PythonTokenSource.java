package org.python.antlr;

import org.python.core.Py;

/*
 [The "BSD licence"]
 Copyright (c) 2004 Terence Parr and Loring Craymer
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import org.antlr.runtime.*;
import java.util.*;

/** Python does not explicitly provide begin and end nesting signals.
 Rather, the indentation level indicates when you begin and end.
 This is an interesting lexical problem because multiple DEDENT
 tokens should be sent to the parser sometimes without a corresponding
 input symbol!  Consider the following example:
 <pre>{@literal
 a=1
 if a>1:
     print a
 b=3
}</pre>
 Here the "b" token on the left edge signals that a DEDENT is needed
 after the "print a \n" and before the "b".  The sequence should be
<pre>
 ... 1 COLON NEWLINE INDENT PRINT a NEWLINE DEDENT b ASSIGN 3 ...
</pre>
 For more examples, see the big comment at the bottom of this file.

 This TokenStream normally just passes tokens through to the parser.
 Upon NEWLINE token from the lexer, however, an INDENT or DEDENT token
 may need to be sent to the parser.  The NEWLINE is the trigger for
 this class to do it's job.  NEWLINE is saved and then the first token
 of the next line is examined.  If non-leading-whitespace token,
 then check against stack for indent vs dedent.  If LEADING_WS, then
 the column of the next non-whitespace token will dictate indent vs
 dedent.  The column of the next real token is number of spaces
 in the LEADING_WS token + 1 (to move past the whitespace).  The
 lexer grammar must set the text of the LEADING_WS token to be
 the proper number of spaces (and do tab conversion etc...).

 A stack of column numbers is tracked and used to detect changes
 in indent level from one token to the next.

 A queue of tokens is built up to hold multiple DEDENT tokens that
 are generated.  Before asking the lexer for another token via
 nextToken(), the queue is flushed first one token at a time.

 Terence Parr and Loring Craymer
 February 2004
 */
public class PythonTokenSource implements TokenSource {
    public static final int MAX_INDENTS = 100;
    public static final int FIRST_CHAR_POSITION = 0;

    /** The stack of indent levels (column numbers) */
    int[] indentStack = new int[MAX_INDENTS];
    /** stack pointer */
    int sp=-1; // grow upwards

    /** The queue of tokens */
    Vector<Token> tokens = new Vector<Token>();

    /** We pull real tokens from this lexer */
    CommonTokenStream stream;

    int lastTokenAddedIndex = -1;

    String filename;
    boolean inSingle;

    public PythonTokenSource(PythonLexer lexer) {
    }


    public PythonTokenSource(CommonTokenStream stream, String filename) {
        this(stream, filename, false);
    }

    public PythonTokenSource(CommonTokenStream stream, String filename, boolean single) {
        this.stream = stream;
        this.filename = filename;
        this.inSingle = single;
        // "state" of indent level is FIRST_CHAR_POSITION
        push(FIRST_CHAR_POSITION);
    }

    /** From http://www.python.org/doc/2.2.3/ref/indentation.html

     "Before the first line of the file is read, a single zero is
     pushed on the stack; this will never be popped off again. The
     numbers pushed on the stack will always be strictly increasing
     from bottom to top. At the beginning of each logical line, the
     line's indentation level is compared to the top of the
     stack. If it is equal, nothing happens. If it is larger, it is
     pushed on the stack, and one INDENT token is generated. If it
     is smaller, it must be one of the numbers occurring on the
     stack; all numbers on the stack that are larger are popped
     off, and for each number popped off a DEDENT token is
     generated. At the end of the file, a DEDENT token is generated
     for each number remaining on the stack that is larger than
     zero."

     I use char position in line 0..n-1 instead.

     The DEDENTS possibly needed at EOF are gracefully handled by forcing
     EOF to have char pos 0 even though with UNIX it's hard to get EOF
     at a non left edge.
     */
    @Override
    public Token nextToken() {
        // if something in queue, just remove and return it
        if (tokens.size() > 0) {
            Token t = tokens.firstElement();
            if (t.getType() != Token.EOF) { // EOF stops further insertImaginaryIndentDedentTokens
                tokens.removeElementAt(0);
            }
            // System.out.println(filename + t);
            return t;
        }

        insertImaginaryIndentDedentTokens();

        return nextToken();
    }

    private void generateNewline(Token t) {
        //System.out.println("generating newline from token: " + t);
        CommonToken newline = new CommonToken(PythonLexer.NEWLINE, "\n");
        newline.setLine(t.getLine());
        newline.setCharPositionInLine(t.getCharPositionInLine());
        tokens.addElement(newline);
    }

    private void handleEOF(CommonToken eof, CommonToken prev) {
        //System.out.println("processing eof with token: " + prev);
        if (prev != null) {
            eof.setStartIndex(prev.getStopIndex());
            eof.setStopIndex(prev.getStopIndex());
            eof.setLine(prev.getLine());
            eof.setCharPositionInLine(prev.getCharPositionInLine());
        }
    }

    protected void insertImaginaryIndentDedentTokens() {
        Token t = stream.LT(1);

        if (t.getType() == Token.EOF) {
            Token prev = stream.LT(-1);
            handleEOF((CommonToken)t, (CommonToken)prev);
            if (!inSingle) {
                if (prev == null) {
                    generateNewline(t);
                } else if (prev.getType() == PythonLexer.LEADING_WS) {
                    handleDedents(-1, (CommonToken)t);
                    generateNewline(t);
                } else if (prev.getType() != PythonLexer.NEWLINE) {
                    generateNewline(t);
                    handleDedents(-1, (CommonToken)t);
                }
            } else {
                handleDedents(-1, (CommonToken)t);
            }
            enqueue(t);
        } else if (t.getType() == PythonLexer.NEWLINE) {
            // save NEWLINE in the queue
            //System.out.println("found newline: "+t+" stack is "+stackString());
            enqueue(t);
            Token newline = t;
            stream.consume();

            // grab first token of next line
            t = stream.LT(1);

            List<Token> commentedNewlines = enqueueHiddens(t);

            // compute cpos as the char pos of next non-WS token in line
            int cpos = t.getCharPositionInLine(); // column dictates indent/dedent
            if (t.getType() == Token.EOF) {
                handleEOF((CommonToken)t, (CommonToken)newline);
                cpos = -1; // pretend EOF always happens at left edge
            }
            else if (t.getType() == PythonLexer.LEADING_WS) {
                stream.consume();
                Token next = stream.LT(1);
                if (next != null && next.getType() == Token.EOF) {
                    return;
                } else {
                    cpos = t.getText().length();
                }
            } else {
                stream.consume();
            }

            //System.out.println("next token is: "+t);

            // compare to last indent level
            int lastIndent = peek();
            //System.out.println("cpos, lastIndent = "+cpos+", "+lastIndent);
            if (cpos > lastIndent) { // they indented; track and gen INDENT
                handleIndents(cpos, (CommonToken)t);
            }
            else if (cpos < lastIndent) { // they dedented
                handleDedents(cpos, (CommonToken)t);
            }

            if (t.getType() == Token.EOF && inSingle) {
                String newlines = newline.getText();
                for(int i=1;i<newlines.length();i++) {
                    generateNewline(newline);
                }
                for (Token c : commentedNewlines) {
                    generateNewline(c);
                }
            }

            if (t.getType() != PythonLexer.LEADING_WS) { // discard WS
                tokens.addElement(t);
            }

        } else {
            enqueue(t);
            stream.consume();
        }
    }

    private void enqueue(Token t) {
        enqueueHiddens(t);
        tokens.addElement(t);
    }

    private List<Token> enqueueHiddens(Token t) {
        List<Token> newlines = new ArrayList<Token>();
        if (inSingle && t.getType() == Token.EOF) {
            int k = 1;
            while (stream.size() > lastTokenAddedIndex + k) {
                Token hidden = stream.get(lastTokenAddedIndex + k);
                if (hidden.getType() == PythonLexer.COMMENT) {
                    String text = hidden.getText();
                    int i = text.indexOf("\n");
                    i = text.indexOf("\n", i + 1);
                    while(i != -1) {
                        newlines.add(hidden);
                        lastTokenAddedIndex++;
                        i = text.indexOf("\n", i + 1);
                    }
                    k++;
                } else if (hidden.getType() == PythonLexer.NEWLINE) {
                    generateNewline(hidden);
                    lastTokenAddedIndex++;
                    break;
                } else if (hidden.getType() == PythonLexer.LEADING_WS) {
                    k++;
                } else {
                    break;
                }
            }
        }

        List<? extends Token> hiddenTokens =
                stream.getTokens(lastTokenAddedIndex + 1, t.getTokenIndex() - 1);
        if (hiddenTokens != null) {
            tokens.addAll(hiddenTokens);
        }
        lastTokenAddedIndex = t.getTokenIndex();
        return newlines;
    }

    private void handleIndents(int cpos, CommonToken t) {
        push(cpos);
        //System.out.println("push("+cpos+"): "+stackString());
        CommonToken indent = new CommonToken(PythonParser.INDENT,"");
        indent.setCharPositionInLine(t.getCharPositionInLine());
        indent.setLine(t.getLine());
        indent.setStartIndex(t.getStartIndex() - 1);
        indent.setStopIndex(t.getStartIndex() - 1);
        tokens.addElement(indent);
    }

    private void handleDedents(int cpos, CommonToken t) {
        // how far back did we dedent?
        int prevIndex = findPreviousIndent(cpos, t);
        //System.out.println("dedented; prevIndex of cpos="+cpos+" is "+prevIndex);
        // generate DEDENTs for each indent level we backed up over
        for (int d = sp - 1; d >= prevIndex; d--) {
            CommonToken dedent = new CommonToken(PythonParser.DEDENT,"");
            dedent.setCharPositionInLine(t.getCharPositionInLine());
            dedent.setLine(t.getLine());

            dedent.setStartIndex(t.getStartIndex() - 1);
            dedent.setStopIndex(t.getStartIndex() - 1);

            tokens.addElement(dedent);
        }
        sp = prevIndex; // pop those off indent level
    }

    //  T O K E N  S T A C K  M E T H O D S

    protected void push(int i) {
        if (sp >= MAX_INDENTS) {
            throw new IllegalStateException("stack overflow");
        }
        sp++;
        indentStack[sp] = i;
    }

    protected int pop() {
        if (sp<0) {
            throw new IllegalStateException("stack underflow");
        }
        int top = indentStack[sp];
        sp--;
        return top;
    }

    protected int peek() {
        return indentStack[sp];
    }

    /** Return the index on stack of previous indent level == i else -1 */
    protected int findPreviousIndent(int i, Token t) {
        for (int j = sp - 1; j >= 0; j--) {
            if (indentStack[j] == i) {
                return j;
            }
        }
        //The -2 is for the special case of getCharPositionInLine in multiline str nodes.
        if (i == -1 || i == -2) {
            return FIRST_CHAR_POSITION;
        }
        ParseException p = new ParseException("unindent does not match any outer indentation level", t.getLine(), t.getCharPositionInLine());
        p.setType(Py.IndentationError);
        throw p;
    }

    public String stackString() {
        StringBuffer buf = new StringBuffer();
        for (int j = sp; j >= 0; j--) {
            buf.append(" ");
            buf.append(indentStack[j]);
        }
        return buf.toString();
    }

    @Override
    public String getSourceName() {
        return filename;
    }

}

/* More example input / output pairs with code simplified to single chars
------- t1 -------
a a
        b b
        c
d
a a \n INDENT b b \n c \n DEDENT d \n EOF
------- t2 -------
a  c
 b
c
a c \n INDENT b \n DEDENT c \n EOF
------- t3 -------
a
        b
                c
d
a \n INDENT b \n INDENT c \n DEDENT DEDENT d \n EOF
------- t4 -------
a
    c
                  d
    e
    f
             g
             h
             i
              j
    k
a \n INDENT c \n INDENT d \n DEDENT e \n f \n INDENT g \n h \n i \n INDENT j \n DEDENT DEDENT k \n DEDENT EOF
------- t5 -------
a
        b
        c
                d
                e
a \n INDENT b \n c \n INDENT d \n e \n DEDENT DEDENT EOF
*/
