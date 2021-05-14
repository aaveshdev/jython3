package org.python.antlr;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.base.mod;

public class BaseParser {

    protected final CharStream charStream;
    @Deprecated
    protected final boolean partial;
    protected final String filename;
    protected final String encoding;
    protected ErrorHandler errorHandler = new FailFastHandler();
    
    public BaseParser(CharStream stream, String filename, String encoding) {
        this(stream, filename, encoding, false);
    }
    
    @Deprecated
    public BaseParser(CharStream stream, String filename, String encoding, boolean partial) {
        this.charStream = stream;
        this.filename = filename;
        this.encoding = encoding;
        this.partial = partial;
    }

    public void setAntlrErrorHandler(ErrorHandler eh) {
        this.errorHandler = eh;
    }

    protected PythonParser setupParser(boolean single) {
        PythonLexer lexer = new PythonLexer(charStream);
        lexer.setErrorHandler(errorHandler);
        lexer.single = single;
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens, filename, single);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens, encoding);
        parser.setErrorHandler(errorHandler);
        parser.setTreeAdaptor(new PythonTreeAdaptor());
        return parser;
    }

    public mod parseExpression() {
        mod tree = null;
        PythonParser parser = setupParser(false);
        try {
            PythonParser.eval_input_return r = parser.eval_input();
            tree = (mod)r.tree;
        } catch (RecognitionException e) {
            //XXX: this can't happen.  Need to strip the throws from antlr
            //     generated code.
        }
        return tree;
    }

    public mod parseInteractive() {
        mod tree = null;
        PythonParser parser = setupParser(true);
        try {
            PythonParser.single_input_return r = parser.single_input();
            tree = (mod)r.tree;
        } catch (RecognitionException e) {
            //I am only throwing ParseExceptions, but "throws RecognitionException" still gets
            //into the generated code.
            System.err.println("FIXME: pretty sure this can't happen -- but needs to be checked");
        }
        return tree;
    }

    public mod parseModule() {
        mod tree = null;
        PythonParser parser = setupParser(false);
        try {
            PythonParser.file_input_return r = parser.file_input();
            tree = (mod)r.tree;
        } catch (RecognitionException e) {
            //XXX: this can't happen.  Need to strip the throws from antlr
            //     generated code.
        }
        return tree;
    }
}
