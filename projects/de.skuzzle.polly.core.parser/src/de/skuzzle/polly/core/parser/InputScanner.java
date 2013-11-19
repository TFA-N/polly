package de.skuzzle.polly.core.parser;

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.skuzzle.polly.core.parser.problems.Problems;




/*
 * This class is subject to ISSUE: 0000018
 * Need to proof working with different encodings.
 */



/**
 * Extends an {@link AbstractTokenStream} to read actual {@link Token}s from an input 
 * String. The following identifiers are read as keywords:
 * <pre>
 * true
 * false
 * now
 * </pre>
 * 
 * <p>Identifiers may start with a _ or a letter and can then contain any letter, 
 * number or further _.</p>
 * 
 * <p>Numbers read may have a decimal part and optionally a mantiss part 
 * (scientific notation). More formally numbers may have the format
 * {@code \d+(\.\d+)?([+-]?[eE]\d+)?}</p>
 * 
 * <p>There are a few different kinds of date-tokens which after they are read, all 
 * contain a {@link Date} value. A date may be a pure time definition like 
 * <code>\d{1,2}:\d{1,2}</code> where the first number must lie in the interval 
 * {@code [0;23]} and the second number in {@code [0;59]}.</p>
 * 
 * <p>Further a date-token can be a pure date like <code>\d{1,2}.\d{1,2}.\d{4}</code> 
 * where the three parts must be valid for date definitions.</p>
 * 
 * <p>And a date can be specified by an amount of time from now. Like {@code 1d4h10m} 
 * specifies a date with the value one day, four hours and 10 minutes from now.
 * Valid characters for such a date definition are:</p>
 * <pre>
 * y    year    interpreted as 365d
 * w    week    interpreted as 7d
 * d    day     interpreted as 24h
 * h    hour    interpreted as 60m
 * m    minute  interpreted as 60s
 * s    second  interpreted as 1000ms
 * </pre>
 * 
 * <p>Each of this characters may only occur once within one of such date definition but
 * must not be ordered.</p>
 * 
 * @author Simon
 *
 */
public class InputScanner extends AbstractTokenStream {
    
    /**
     * The maximum radix value for radix'ed integers. Higher values will cause a
     * {@link ParseException} to be thrown when hitting on.
     * 
     * Note: The minimum value is always 2 (by nature)
     */
    public final static int MAX_RADIX = Character.MAX_RADIX;
    
    

    protected Map<String, TokenType> keywords;
    private boolean skipWhiteSpaces;
    
    
    
    public InputScanner(String stream) {
        super(stream);
        this.prepareKeywords();
    }
    
    
    
    public InputScanner(String stream, Charset charset) {
        super(stream, charset);
        this.prepareKeywords();
    }
    
    
    
    protected void prepareKeywords() {
        this.keywords = new HashMap<String, TokenType>();
        this.keywords.put("xor", TokenType.XOR); //$NON-NLS-1$
        this.keywords.put("true", TokenType.TRUE); //$NON-NLS-1$
        this.keywords.put("false", TokenType.FALSE); //$NON-NLS-1$
        this.keywords.put("now", TokenType.DATETIME); //$NON-NLS-1$
        this.keywords.put("polly", TokenType.POLLY); //$NON-NLS-1$
        this.keywords.put("public", TokenType.PUBLIC); //$NON-NLS-1$
        this.keywords.put("temp", TokenType.TEMP); //$NON-NLS-1$
        this.keywords.put("help", TokenType.QUESTION); //$NON-NLS-1$
        this.keywords.put("if", TokenType.IF); //$NON-NLS-1$
        this.keywords.put("del", TokenType.DELETE); //$NON-NLS-1$
        this.keywords.put("inspect", TokenType.INSPECT); //$NON-NLS-1$
        this.keywords.put("list", TokenType.LIST); //$NON-NLS-1$
        this.keywords.put("delay", TokenType.DELAY); //$NON-NLS-1$
        this.keywords.put("reinterpret", TokenType.REINTERPRET); //$NON-NLS-1$
        /* To avoid 1char identifiers "_" */
        this.keywords.put("_", TokenType.UNKNOWN); //$NON-NLS-1$
    }
    
    
    
    /**
     * Sets whether whitespaces are currently being skipped.
     * 
     * @param value Whether whitespaces should be skipped.
     */
    public void setSkipWhiteSpaces(boolean value) {
        this.skipWhiteSpaces = value;
    }
    
    
    
    /**
     * Gets whether whitespaces are currently being skipped.
     * 
     * @return Whether whitespaces are currently being skipped.
     */
    public boolean skipWhiteSpaces() {
        return this.skipWhiteSpaces;
    }

    
    
    @Override
    protected synchronized Token readToken() throws ParseException {
        final Token next = this.readTokenInternal();
        if (ParserProperties.should(ParserProperties.ENABLE_SCANNER_DEBUGGING)) {
            System.out.println(next.toString());
        }
        return next;
    }
    
    
    
    protected final Token readTokenInternal() throws ParseException {
        int state = 0;
        int tokenStart = this.getStreamIndex();
        StringBuilder currentString = new StringBuilder();
        
        while (!this.eos()) {
            if (state == 0) {
                int next = this.readChar();
                
                if (next == -1) {
                    final Position pos = new Position(tokenStart, 
                        this.getStreamIndex() + 1);
                    return new Token(TokenType.EOS, pos);
                }
                
                if (Character.isWhitespace(next)) {
                    if (!this.skipWhiteSpaces) {
                        this.pushBack(next);
                        state = 1;
                    } else {
                        // skipping whitespaces, so move token start
                        ++tokenStart;
                    }
                    
                } else if (InputScanner.isIdentifierStart(next)) {
                    this.pushBack(next);
                    return this.readIdentifier();
                    
                } else if (next == '0') {
                    /*
                     * '0' cannot start a number, but starts the 0x: Operator
                     */
                    return this.readRadixOperator();
                } else if (Character.isDigit(next)) {
                    this.pushBack(next);
                    return this.readNumber();
                    
                } else if (next == '"') {
                    this.pushBack(next);
                    return this.readString();
                    
                } else if (next == '#') {
                	return this.readChannel();
                	
                } else if (next == '+') {
                    state = 9;
                } else if (next == '-') {
                    state = 10;
                } else if (next == '.') {
                    state = 8;
                } else if (next == '@') {
                    return this.readUser();
                } else if (next == ',') {
                    return new Token(TokenType.COMMA, this.spanFrom(tokenStart), ","); //$NON-NLS-1$
                } else if (next == '*') {
                    return new Token(TokenType.MUL, this.spanFrom(tokenStart), "*"); //$NON-NLS-1$
                } else if (next == '/') {
                    state = 15;
                } else if (next == '\\') {
                    state = 13;
                } else if (next == '%') {
                    return new Token(TokenType.MOD, this.spanFrom(tokenStart), "%"); //$NON-NLS-1$
                } else if (next == '$') {
                    return new Token(TokenType.DOLLAR, this.spanFrom(tokenStart),"$"); //$NON-NLS-1$
                } else if (next == '^') {
                    state = 14;
                } else if (next == '!') {
                    state = 2;
                } else if (next == '(') {
                    return new Token(TokenType.OPENBR, this.spanFrom(tokenStart), "("); //$NON-NLS-1$
                } else if (next == ')') {
                    return new Token(TokenType.CLOSEDBR, this.spanFrom(tokenStart), ")"); //$NON-NLS-1$
                } else if (next == '[') {
                    return new Token(TokenType.OPENSQBR, this.spanFrom(tokenStart), "["); //$NON-NLS-1$
                } else if (next == ']') {
                    return new Token(TokenType.CLOSEDSQBR, this.spanFrom(tokenStart), "]"); //$NON-NLS-1$
                } else if (next == '{') {
                    return new Token(TokenType.OPENCURLBR, this.spanFrom(tokenStart), "{"); //$NON-NLS-1$
                } else if (next == '}') {
                    return new Token(TokenType.CLOSEDCURLBR, this.spanFrom(tokenStart), "}"); //$NON-NLS-1$
                } else if (next == '?') {
                    state = 12;
                } else if (next == '~') {
                    return new Token(TokenType.WAVE, this.spanFrom(tokenStart), "~"); //$NON-NLS-1$
                } else if (next == ';') {
                    return new Token(TokenType.SEMICOLON, this.spanFrom(tokenStart), ";"); //$NON-NLS-1$
                } else if (next == '&') {
                    state = 7;
                } else if (next == '|') {
                    state = 6;
                } else if (next == ':') {
                    return new Token(TokenType.COLON, this.spanFrom(tokenStart), ":"); //$NON-NLS-1$
                } else if (next == '=') {
                    state = 5;
                } else if (next == '<') {
                    state = 3;
                } else if (next == '>') {
                    state = 4;
                } else {
                    return this.parseException(
                        Problems.format(Problems.ILLEGAL_SYMBOL, (char) next),
                        tokenStart);
                }
                    
            } else if (state == 1) {
                int next = this.readChar();
                
                if (!Character.isWhitespace(next)) {
                    this.pushBack(next);
                    return new Token(TokenType.SEPERATOR, this.spanFrom(tokenStart), 
                            currentString.toString());
                } else {
                    currentString.appendCodePoint(next);
                }
                
            } else if (state == 2) {
                int next = this.readChar();
                
                if (next == '=') {
                    return new Token(TokenType.NEQ, this.spanFrom(tokenStart), "!="); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(
                            TokenType.EXCLAMATION, this.spanFrom(tokenStart), "!"); //$NON-NLS-1$
                }
                
            } else if (state == 3) {
                int next = this.readChar();
                
                if (next == '=') {
                    state = 16;
                } else if (next == '<') {
                    return new Token(
                        TokenType.LEFT_SHIFT, this.spanFrom(tokenStart), "<<"); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.LT, this.spanFrom(tokenStart), "<"); //$NON-NLS-1$
                }
                
            } else if (state == 4) {
                int next = this.readChar();
                
                if (next == '=') {
                    return new Token(TokenType.EGT, this.spanFrom(tokenStart), ">="); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.GT, this.spanFrom(tokenStart), ">"); //$NON-NLS-1$
                }
                
            } else if (state == 5) {
                int next = this.readChar();
                
                if (next == '=') {
                    return new Token(TokenType.EQ, this.spanFrom(tokenStart), "==");
                } else if (next == '>') {
                        return new Token(TokenType.IMPLICATION, 
                                this.spanFrom(tokenStart), "=>");
                } else {
                    return this.parseException(
                        Problems.format(Problems.ILLEGAL_SYMBOL, (char) next),
                        tokenStart, next);
                }
                
            } else if (state == 6) {
                int next = this.readChar();
                
                if (next == '|') {
                    return new Token(TokenType.BOOLEAN_OR, this.spanFrom(tokenStart), "||"); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.INT_OR, this.spanFrom(tokenStart), "|"); //$NON-NLS-1$
                }
                
            } else if (state == 7) {
                int next = this.readChar();
                
                if (next == '&') {
                    return new Token(TokenType.BOOLEAN_AND, this.spanFrom(tokenStart), "&&"); //$NON-NLS-1$
                } else if (next == '|') {
                    return new Token(TokenType.AND_OR, this.spanFrom(tokenStart), "&|"); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.INT_AND, this.spanFrom(tokenStart), "&"); //$NON-NLS-1$
                }
                
            } else if (state == 8) {
                int next = this.readChar();
                
                if (next == '.') {
                    return new Token(TokenType.DOTDOT, this.spanFrom(tokenStart), ".."); //$NON-NLS-1$
                } else if (Character.isDigit(next)) {
                    this.pushBackArtificial('0');
                    this.pushBack('.');
                    this.pushBack(next);
                    return this.readNumber();
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.DOT, this.spanFrom(tokenStart), "."); //$NON-NLS-1$
                }
            } else if (state == 9) {
                int next = this.readChar();
                
                if (next == '~') {
                    return new Token(TokenType.ADDWAVE, this.spanFrom(tokenStart), "+~"); //$NON-NLS-1$
                } else if (next == '=') {
                    return new Token(TokenType.ADDEQUALS, this.spanFrom(tokenStart), 
                        "+="); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.ADD, this.spanFrom(tokenStart), "+"); //$NON-NLS-1$
                }
                
            } else if (state == 10) {
                int next = this.readChar();
                
                if (next == '>') {
                    return new Token(TokenType.ASSIGNMENT, this.spanFrom(tokenStart), "->"); //$NON-NLS-1$
                } else if (next == '=') {
                    return new Token(TokenType.SUBEQUALS, this.spanFrom(tokenStart), 
                        "-="); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.SUB, this.spanFrom(tokenStart), "-"); //$NON-NLS-1$
                }
            } else if (state == 12) {
                int next = this.readChar();
                
                if (next == '!') {
                    return new Token(TokenType.QUEST_EXCALAMTION, 
                        this.spanFrom(tokenStart), "?!"); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.QUESTION, this.spanFrom(tokenStart), "?"); //$NON-NLS-1$
                }
            } else if (state == 13) {
                int next = this.readChar();
                
                if (next == '(') {
                    return new Token(TokenType.LAMBDA, this.spanFrom(tokenStart), "\\("); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    final Token escaped = this.readToken();
                    return new EscapedToken(this.spanFrom(tokenStart), escaped);
                }
            } else if (state == 14) {
                int next = this.readChar();
                
                if (next == '^') {
                    return new Token(TokenType.XOR, this.spanFrom(tokenStart), "^^"); //$NON-NLS-1$
                } else if (next == 'T') {
                    return new Token(TokenType.TRANSPOSE, this.spanFrom(tokenStart), "^T"); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.POWER, this.spanFrom(tokenStart), "^"); //$NON-NLS-1$
                }
            } else if (state == 15) {
                int next = this.readChar();
                
                if (next == '/') {
                    return new Token(TokenType.INTDIV, this.spanFrom(tokenStart), "//"); //$NON-NLS-1$
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.DIV, this.spanFrom(tokenStart), "/"); //$NON-NLS-1$
                }
            } else if (state == 16) {
                int next = this.readChar();
                
                if (next == '>') {
                    return new Token(TokenType.EQUIVALENCE, this.spanFrom(tokenStart), "<=>");
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.ELT, this.spanFrom(tokenStart), "<=");
                }
            } else {
                throw new IllegalStateException("unhandled state: " + state); //$NON-NLS-1$
            }
        }
        
        return new Token(TokenType.EOS, new Position(tokenStart - 1, tokenStart));
    }
    
    
    
    /**
     * Reads the 'radix' operator which changes the representation of a number into
     * a number system with the given radix.
     * 
     * @return A {@link Token} which contains the radix in {@link Token#getLongValue()}.
     * @throws ParseException If a lexical error appears.
     */
    private Token readRadixOperator() throws ParseException {
        int tokenStart = this.getStreamIndex() - 1; // include the skipped '0'
        int state = 0;
        int radix = 0;
        
        while (!this.eos()) {
            if (state == 0) {
                int next = this.readChar();
                
                if (next != 'x') {
                    this.pushBack('0');
                    this.pushBack(next);
                    return this.readNumber();
                } else {
                    state = 1;
                }
            } else if (state == 1) {
                int next = this.readChar();
                
                if (Character.isDigit(next)) {
                    this.pushBack(next);
                    state = 2;
                } else {
                    return this.parseException(
                        Problems.format(Problems.MISSING_RADIX),
                            tokenStart, next);
                }
            } else if (state == 2) {
                int next = this.readChar();
                
                if (Character.isDigit(next)) {
                    radix = radix * 10 + Character.digit(next, 10);
                } else if (next == ':') {
                    
                    if (radix > Character.MAX_RADIX) {
                        return this.parseException(
                            Problems.format(Problems.HIGH_RADIX, radix, 
                                Character.MAX_RADIX),
                            tokenStart);
                    }
                    
                    return new Token(TokenType.RADIX, this.spanFrom(tokenStart), radix);
                }
            }
        }
        
        return this.parseException(Problems.format(Problems.INVALID_0X), tokenStart);
    }
    
    
    
    /**
     * Reads a String-literal. A String-literal starts with a " and ends at the next
     * ". In between there may occur any other char. 
     * 
     * @return A new String Token.
     * @throws ParseException If no closing quotes could be found.
     */
    private Token readString() throws ParseException {
        int tokenStart = this.getStreamIndex();
        StringBuilder lexem = new StringBuilder();
        int state = 0;
        Token escapeError = null;
        
        while (!this.eos()) {
            if (state == 0) {
                int next = this.readChar();
                
                if (next == '"') {
                    //lexem.append(next);   //do not append quotes to string literal 
                    state = 1;
                } else {
                    return this.parseException(Problems.format(Problems.INVALID_STRING), 
                        tokenStart, next);
                }
            } else if (state == 1) {
                int start = this.getStreamIndex();
                int next = this.readChar();
                
                if (next == '\\') {
                    
                    if (!this.readEscapeSequence(lexem)) {
                        next = this.readChar();
                        
                        escapeError = this.parseException(
                            Problems.format(Problems.INVALID_ESCAPE, (char) next), start);
                    }
                } else if (next == '"') {
                    //lexem.append(next);   //see above
                    if (escapeError != null) {
                        return escapeError;
                    }
                    return new Token(TokenType.STRING, this.spanFrom(tokenStart), 
                            lexem.toString());
                } else if (next == -1) {
                    // HACK: to avoid errors if closing quotes are missing
                    //       This is subject to ISSUE: 0000022
                    break;
                } else {
                    lexem.appendCodePoint(next);
                }
            }
        }
        
        this.pushBack(-1);
        return this.parseException(Problems.format(Problems.UNCLOSED_STRING), tokenStart);
    }
    
    
    
    private boolean readEscapeSequence(StringBuilder lexem) throws ParseException {
        // -1 to include the '\' which was consumed by #readString()

        while (!this.eos()) {
            int next = this.readChar();
            
            if (next == '"') {
                lexem.append('"');
                return true;
            } else if (next == '\\') {
                lexem.append('\\');
                return true;
            } else {
                this.pushBack(next);
                return false;
            }
        }
        return false;
    }
    
    
    
    private Token readChannel() throws ParseException {
        int tokenStart = this.getStreamIndex();
        StringBuilder lexem = new StringBuilder();

        while (!this.eos()) {
            int next = this.readChar();
        
            if (InputScanner.isIdentifierPart(next) || next == '-') {
                lexem.appendCodePoint(next);
            } else {
                this.pushBack(next);
                return new Token(TokenType.CHANNEL, 
                		this.spanFrom(tokenStart), "#" + lexem.toString()); //$NON-NLS-1$
            }
        }
        
        return this.parseException(Problems.format(Problems.INVALID_CHANNEL, lexem), 
            tokenStart);
    }
    
    
    
    private Token readUser() throws ParseException {
        int tokenStart = this.getStreamIndex() - 1; // include @ sign
        int state = 0;
        StringBuilder lexem = new StringBuilder();

        // ISSUE: 0000031
        // Userliterals can contain "-" which interferes with a following assignment
        // operator. This is now fixed by a special treatment with a lookahead to check
        // if an assignment operator is following.
        while (!this.eos()) {
            if (state == 0) {
                int next = this.readChar();
            
                if (InputScanner.isIdentifierPart(next) || next == '[' || next == ']') {
                    lexem.appendCodePoint(next);
                } else if (next == '-') {
                    state = 1;
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.USER, 
                            this.spanFrom(tokenStart), lexem.toString());
                }
            } else if (state == 1) {
                int next = this.readChar();
                
                if (next == '>') {
                    this.pushBack('-');
                    this.pushBack('>');
                    return new Token(TokenType.USER, 
                        this.spanFrom(tokenStart), lexem.toString());
                } else {
                    lexem.append("-"); //$NON-NLS-1$
                    this.pushBack(next);
                    state = 0;
                }
            }
        }
        
        return this.parseException(Problems.format(Problems.INVALID_USER, lexem), 
            tokenStart);
    }
    
    
    
    /**
     * Reads an Identifier from the stream. Identifier can start with a letter or a
     * underscore followed by any letters, numbers or underscores.
     * If the identifier ends with a colon (':'), a user literal is returned. Before
     * an identifier Token is returned, it is checked by
     * {@link #identifierToToken(String, int)} whether it is a reserved keyword.
     * 
     * @return An identifier Token or a User Token.
     * @throws ParseException If any invalid character occurs.
     */
    private Token readIdentifier() throws ParseException {
        int tokenStart = this.getStreamIndex();
        int state = 0;
        StringBuilder lexem = new StringBuilder();
        
        // ISSUE: 0000027
        // Fixed by adding state 0, which expects an identifier-start-character and then
        // switches to state 1 which expects identifier-part-characters.
        
        while (!this.eos()) {
            if (state == 0) {
                int next = this.readChar();
                
                if (InputScanner.isIdentifierStart(next)) {
                    state = 1;
                    lexem.appendCodePoint(next);
                } else {
                    return this.parseException(Problems.format(Problems.INVALID_IDENTIFIER), 
                        tokenStart);
                }
            } else if (state == 1) {
                int next = this.readChar();
                
                if (InputScanner.isIdentifierPart(next)) {
                    lexem.appendCodePoint(next);
                /*} else if (next == ':') {
                    //lexem.append(next); // do not append ':' to username
                    return new Token(TokenType.USER, this.spanFrom(tokenStart), 
                            lexem.toString());*/
                } else {
                    this.pushBack(next);
                    return this.identifierToToken(lexem.toString(), tokenStart);
                }
            }
        }
        
        return this.parseException(Problems.format(Problems.INVALID_IDENTIFIER2, lexem), 
            tokenStart);
    }
    
    
    
    /**
     * Converts an identifier to a keyword token if it represents any. Otherwise, an
     * identifier is returned.
     * 
     * @param string The String, representing the identifier.
     * @param tokenStart The stream index where this token begins.
     * @return An Identifier Token or a reserved keyword token.
     * @throws ParseException If {@code string} is an invalid identifier.
     */
    private Token identifierToToken(String string, int tokenStart) throws ParseException {
        TokenType lookup = this.keywords.get(string);
        if (lookup == null) {
            return new Token(TokenType.IDENTIFIER, this.spanFrom(tokenStart), string);
        } else if (lookup == TokenType.UNKNOWN) {
            return this.parseException(
                Problems.format(Problems.INVALID_IDENTIFIER2, string), 
                tokenStart);
        } else {
            return new Token(lookup, this.spanFrom(tokenStart));
        }
    }
    
        
    
    /*
     * States for this method:
     * 0: entry state
     * 1: read at least one number and a dot
     * 3: read at least one number and a dot and know that there is at least one number
     *    to come.
     * 4. read a float or int literal followed by a '�'
     * 5: read the beginning of a date: a number, a dot, a number, a dot
     * 6: 
     */
    
    /**
     * Main function to read all kinds of literals which start with numbers.
     * 
     * It calls {@link #readTimeSpan(int, int)} if it assumes that this is a timespan,
     * or {@link #readTime(int, int, boolean)} if it assumes that this is a time. If it
     * is a normal number (int or float), this method reads it to the end.
     * 
     * @return The read token.
     * @throws ParseException If the read characters form no valid Number or 
     *      DateTime-Token.
     */
    private Token readNumber() throws ParseException {
        int tokenStart = this.getStreamIndex();
        int state = 0;
        int firstPart = 0;  // first part of a time or a date. Also used as radix when
                            // reading a radixed integer literal
        int secondPart = 0; // second part of a date (months)
        int thirdPart = 0;  // year-part of a date
        int tmp = 0;        // first part of a time, if read after a date
        Token timeToken = null;//new Token(TokenType.DATETIME, this.spanFrom(0), new Date());    
        double dec = 1;
        double value = 0.0;
        double exp = 0.0;
        double exp_sign = 1.0;

        
        while (!this.eos()) {
            if (state == 0) {
                int next = this.readChar();
                
                if (Character.isDigit(next)) {
                    value = value * 10 + Character.digit(next, 10);
                    firstPart = firstPart * 10 + Character.digit(next, 10);
                } else if (next == '#') {
                    return this.readRadixedInteger(tokenStart, firstPart);
                } else if (InputScanner.isTimeLiteralChar(next)) {
                    this.pushBack(next);
                    return this.readTimeSpan(firstPart, tokenStart);
                } else if (next == ':') {
                    return this.readTime(firstPart, tokenStart, true);
                } else if (next == '.') {
                    state = 1;
                } else if (next == '°') {  // degree character
                    state = 4;
                } else if (next == 'E' || next == 'e') {
                    state = 9;
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.NUMBER, this.spanFrom(tokenStart), value);
                }
                
            } else if (state == 1) {
                int next = this.readChar();
                
                if (Character.isDigit(next)) {
                    this.pushBack(next);
                    state = 3;
                } else if (next == '.') {
                    this.pushBack('.');
                    this.pushBack('.');
                    return new Token(TokenType.NUMBER, this.spanFrom(tokenStart), value);
                } else {
                    return this.parseException(Problems.format(Problems.MISSING_DECIMALS), 
                        tokenStart, next);
                }
                
            } else if (state == 3) {
                int next = this.readChar();

                if (Character.isDigit(next)) {
                    dec *= 0.1;
                    value += (double) Character.digit(next, 10) * dec;
                    secondPart = secondPart * 10 + Character.digit(next, 10);
                } else if (next == '.') {
                    /* Till now we read the beginning of a date literal missing the
                     * year: xx.xx.
                     * or this might be a decimal number followed by a dotdot operator
                     * if the next char is a '.'
                     * In the latter case, we return the so far read number and pushback 
                     * two dots.
                     */
                    state = 5;
                } else if (next == 'E' || next == 'e') {
                    state = 9;
                } else if (next == '°') { //degree character
                    state = 4;
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.NUMBER, this.spanFrom(tokenStart), value);
                }
                
            } else if (state == 4) {
                return new Token(TokenType.NUMBER, this.spanFrom(tokenStart), 
                        Math.toRadians(value));
                
            } else if (state == 5) {
                int next = this.readChar();
                
                /* This is no Date Literal, but a decimal number followed by a
                 * dotdot operator. So pushback the two dots and return the number.
                 */
                if (next == '.') {
                    this.pushBack('.');
                    this.pushBack('.');
                    return new Token(TokenType.NUMBER, this.spanFrom(tokenStart), value);
                }
                
                // HACK: Need to ensure that at least on number has been read before 
                //       reading on.
                if (firstPart > 31 || secondPart > 12) {
                    return this.parseException(Problems.format(Problems.INVALID_DATE_TIME), 
                        tokenStart);
                }
                
                if (Character.isDigit(next)) {
                    thirdPart = thirdPart * 10 + Character.digit(next, 10);
                } else if (next == '@') {
                    state = 6;
                } else {
                    this.pushBack(next);
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.DAY_OF_MONTH, firstPart);
                    c.set(Calendar.MONTH, secondPart);
                    c.set(Calendar.YEAR, thirdPart);
                    timeToken = new Token(TokenType.DATETIME, this.spanFrom(tokenStart), 
                        c.getTime());
                    state = 8;
                }                

            } else if (state == 6) {
                int next = this.readChar();
                
                if (Character.isDigit(next)) {
                    this.pushBack(next);
                    state = 7;
                } else {
                    return this.parseException(Problems.format(Problems.INVALID_DATE_TIME), 
                        tokenStart, next);
                }
            } else if (state == 7) {
                int next = this.readChar();
                
                if (Character.isDigit(next)) {
                    tmp = tmp * 10 + Character.digit(next, 10);
                } else if (next == ':') {
                    timeToken = this.readTime(tmp, tokenStart, false);
                    if (timeToken.matches(TokenType.ERROR)) {
                        return timeToken;
                    }
                    state = 8;
                } else {
                    return this.parseException(Problems.format(Problems.INVALID_DATE_TIME), 
                        tokenStart, next);
                }
                
            } else if (state == 8) {
                if (thirdPart < 1900 || thirdPart > 9999) {
                    return this.parseException(Problems.format(Problems.INVALID_DATE_TIME), 
                        tokenStart);
                }
                
                // CONSIDER ISSUE 0000115
                Calendar c = Calendar.getInstance();
                c.setTime(timeToken.getDateValue());
                c.set(thirdPart, secondPart - 1, firstPart);
                return new Token(TokenType.DATETIME, this.spanFrom(tokenStart), 
                        c.getTime());
                
            } else if (state == 9) {
                int next = this.readChar();
                
                if (Character.isDigit(next)) {
                    this.pushBack(next);
                    state = 10;
                } else if (next == '-') {
                    exp_sign = -1.0;
                    state = 10;
                } else if (next == '+') {
                    exp_sign = 1.0;
                    state = 10;
                } else {
                    return this.parseException(Problems.format(Problems.INVALID_NUMBER), 
                        tokenStart, next);
                }
                
            } else if (state == 10) {
                int next = this.readChar();
                
                if (Character.isDigit(next)) {
                    exp = exp * 10 + Character.digit(next, 10);
                } else {
                    this.pushBack(next);
                    
                    // HACK: Ensure that at least one number has been read
                    if (exp == 0.0) {
                        return this.parseException(
                            Problems.format(Problems.INVALID_NUMBER), tokenStart);
                    }
                    value = value * Math.pow(10, exp * exp_sign);
                    return new Token(TokenType.NUMBER, this.spanFrom(tokenStart), value);
                }
            } // state
        } // while
        
        throw new IllegalStateException("should not be reachable"); //$NON-NLS-1$
    }
    
    

    private Token readTimeSpan(int value, int tokenStart) throws ParseException {
        Set<Integer> odd = new TreeSet<Integer>();
        int state = 0;
        int tmp = value;
        value = 0;
        
        while (!this.eos()) {
            if (state == 0) {
                int next = this.readChar();
                
                if (InputScanner.isTimeLiteralChar(next)) {
                    
                    if (odd.contains(next)) {
                        return this.parseException(
                            Problems.format(Problems.INVALID_DATE_TIME), 
                            tokenStart);
                    }
                    odd.add(next);
                    value += tmp * InputScanner.timeLiteralValue(next);
                    tmp = 0;
                } else if (Character.isDigit(next)) {
                    this.pushBack(next);
                    state = 1;
                } else {
                    this.pushBack(next);
                    
                    /*Calendar c = Calendar.getInstance();
                    c.add(Calendar.SECOND, (int) value);
                    return new Token(TokenType.DATETIME, this.spanFrom(tokenStart),
                            c.getTime());*/
                    return new Token(TokenType.TIMESPAN, this.spanFrom(tokenStart),
                        value);
                }
                
            } else if (state == 1) {
                int next = this.readChar();
                
                if (InputScanner.isTimeLiteralChar(next)) {
                    this.pushBack(next);
                    state = 0;
                } else if (Character.isDigit(next)) {
                    tmp = tmp * 10 + Character.digit(next, 10);
                }
            }
        }
        
        return this.parseException(Problems.format(Problems.INVALID_DATE_TIME), 
            tokenStart);
    }
    
    
    
    /**
     * <p>Reads the end of a time specification from a String. 'End' means, that the 
     * first part (the hour part) must already have been read.</p>
     * 
     * <p>To determine if this is really a time rather than a normal number, the method
     * {@link #readNumber()} consumes numbers until it encounters a {@code colon}.
     * It then passes the so far read numbers to this method to read the rest of the 
     * time. If the next char encountered is no number and {@code exceptNumber} is
     * set to {@code true}, this method will return a Number-Token, representing the
     * so far read characters. If set to {@code false}, this method will throw a
     * {@link ParseException}.</p>
     * 
     * @param firstPart The so far read part of the time (the hour-part).
     * @param tokenStart The beginning index of the currently read token.
     * @param exceptNumber Determines if this method breaks up if it is not a completely
     *      valid time, or if it will return a Number-Token instead (see description 
     *      above!).
     * @return Most likely a DateTime-Token.
     * @throws ParseException If the read characters form no valid DateTime-Token.
     */
    private Token readTime(int firstPart, int tokenStart, 
            boolean exceptNumber) throws ParseException {
        int state = 0;
        int secondPart = 0;
        
        if (firstPart > 23) {
            return this.parseException(Problems.format(Problems.INVALID_DATE_TIME), 
                tokenStart);
        }
        
        while (!this.eos()) {
            if (state == 0) {
                int next = this.readChar();
                
                if (Character.isDigit(next)) {
                    state = 1;
                    secondPart = secondPart * 10 + Character.digit(next, 10);
                } else if (exceptNumber){
                    this.pushBack(':');
                    return new Token(TokenType.NUMBER, this.spanFrom(tokenStart), 
                            (double) firstPart);
                } else {
                    return this.parseException(
                        Problems.format(Problems.INVALID_DATE_TIME), tokenStart, next);
                }
            } else if (state == 1) {
                int next = this.readChar();
                
                if (!Character.isDigit(next)) {
                    pushBack(next);
                } else {
                    secondPart = secondPart * 10 + Character.digit(next, 10);
                }
                
                if (secondPart > 59) {
                    return this.parseException(
                        Problems.format(Problems.INVALID_DATE_TIME), tokenStart);
                }
                
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, firstPart);
                c.set(Calendar.MINUTE, secondPart);
                c.set(Calendar.SECOND, 0);

                return new Token(TokenType.DATETIME, this.spanFrom(tokenStart), 
                        c.getTime());
                
            }
        }
        
        return this.parseException(Problems.format(Problems.INVALID_DATE_TIME), 
            tokenStart);
    }
    
    
    
    private Token readRadixedInteger(int tokenStart, int radix) throws ParseException {
        int value = 0;
        int state = 0;
        
        if (radix > Character.MAX_RADIX) {
            return this.parseException(
                Problems.format(Problems.HIGH_RADIX, radix, Character.MAX_RADIX), 
                tokenStart);
        }
        
        while (!this.eos()) {
            if (state == 0) {
                int next = this.readChar();
                
                if (InputScanner.isDigit(next, radix)) {
                    this.pushBack(next);
                    state = 1;
                } else {
                    return this.parseException(
                        Problems.format(Problems.INVALID_RADIXED_INT), tokenStart);
                }
            } else if (state == 1) {
                int next = this.readChar();
                
                if (InputScanner.isDigit(next, radix)) {
                    value = value * radix + Character.digit(next, radix);
                } else {
                    this.pushBack(next);
                    return new Token(TokenType.NUMBER, this.spanFrom(tokenStart), 
                        (double) value);
                }
            }
        }
        
        // not reachable
        return this.parseException(Problems.format(Problems.INVALID_RADIXED_INT), 
            tokenStart);
    }
    
    
    
    /**
     * Reports a lexical error to the {@link ProblemReporter} of this scanner.
     * 
     * @param errorMessage The parse error message.
     * @param tokenStart The beginning of the errornous token.
     * @return A token with type {@link TokenType#ERROR}
     * @throws ParseException If the {@link ProblemReporter} only supports one
     *          Problem.
     */
    protected Token parseException(String errorMessage, int tokenStart) 
            throws ParseException {
        final Position pos = this.spanFrom(tokenStart);
        return new Token(TokenType.ERROR, pos, errorMessage);
    }
    
    
    
    protected Token parseException(String errorMessage, int tokenStart, int next) 
            throws ParseException {
        this.pushBack(next);
        final Position pos = this.spanFrom(tokenStart);
        this.readChar();
        return new Token(TokenType.ERROR, pos, errorMessage);
    }
    
    
    
    /**
     * Determines whether the char c is a valid symbol for a number literal with
     * the given radix. E.g. for radix = 16, this method would return <code>true</code>
     * if c was eiter of <code>0123456789ABCDEFabcdef</code>.
     *  
     * @param c The character to test.
     * @param radix The radix.
     * @return <code>true</code> iff the char is a valid symbol for the given radix.  
     */
    protected static boolean isDigit(int c, int radix) {        
        return Character.digit(c, radix) != -1;
    }
    
    
    
    /**
     * Determines whether the given codepoint os a valid part of a polly identifier.
     * 
     * @param token The character to check.
     * @return <code>true</code> if it is a valid identifier part.
     */
    protected static boolean isIdentifierPart(int token) {
        return Character.isJavaIdentifierPart(token) && token != '$';
    }
    
    
    
    /**
     * Determines whether the given codepoint is a valid char to start a polly 
     * identifier.
     * 
     * @param token The character to check.
     * @return <code>true</code> if the character can start a polly identifier.
     */
    protected static boolean isIdentifierStart(int token) {
        return Character.isJavaIdentifierStart(token) && token != '$';
    }
    
    
    
    // Fixed ISSUE: 0000010: Added characters 'w' and 'y' for week and year.
    /**
     * Determines if a character is a valid time modifier. That is if it is any of the
     * following: {@code 'h' | 'm' | 's' | 'd' | 'w' | 'y'}.
     * @param token The lexical token to check.
     * @return Whether the token is a time modifier.
     */
    protected static boolean isTimeLiteralChar(int token) {
        return token == 'h' || token == 'm' || token == 's' || token == 'd' || 
                token == 'w' || token == 'y';
    }
    
    
    
    /**
     * Gets the value for a time modifier. That is for 
     * {@code 's' = 1, 'm' = 60, 'h' = 3600, 'd' = 86400, 'w' = 604800, 'y' = 31536000}.
     * 
     * @param token TimeModifier character. 
     * @return The value the character represents in terms of a TimeModifier token.
     * @throws IllegalArgumentException If no valid time modifier char as determined by 
     *      {@link #isTimeLiteralChar(int)} is entered.
     */
    protected static int timeLiteralValue(int token) {
        switch (token) {
            case 's': return 1;
            case 'm': return 60;
            case 'h': return 3600;
            case 'd': return 86400;
            case 'w': return 604800;
            case 'y': return 31536000;
            default:
                throw new IllegalArgumentException("No valid time modifier char."); //$NON-NLS-1$
        }
    }
}