package de.skuzzle.polly.core.parser;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.skuzzle.polly.core.parser.PrecedenceTable.PrecedenceLevel;
import de.skuzzle.polly.core.parser.ast.Identifier;
import de.skuzzle.polly.core.parser.ast.ResolvableIdentifier;
import de.skuzzle.polly.core.parser.ast.Root;
import de.skuzzle.polly.core.parser.ast.declarations.Declaration;
import de.skuzzle.polly.core.parser.ast.declarations.Namespace;
import de.skuzzle.polly.core.parser.ast.declarations.types.MissingType;
import de.skuzzle.polly.core.parser.ast.declarations.types.ProductType;
import de.skuzzle.polly.core.parser.ast.declarations.types.Type;
import de.skuzzle.polly.core.parser.ast.directives.DelayDirective;
import de.skuzzle.polly.core.parser.ast.directives.Directive;
import de.skuzzle.polly.core.parser.ast.directives.ProblemDirective;
import de.skuzzle.polly.core.parser.ast.directives.ReinterpretDirctive;
import de.skuzzle.polly.core.parser.ast.expressions.Assignment;
import de.skuzzle.polly.core.parser.ast.expressions.Braced;
import de.skuzzle.polly.core.parser.ast.expressions.Call;
import de.skuzzle.polly.core.parser.ast.expressions.Delete;
import de.skuzzle.polly.core.parser.ast.expressions.Delete.DeleteableIdentifier;
import de.skuzzle.polly.core.parser.ast.expressions.Empty;
import de.skuzzle.polly.core.parser.ast.expressions.Expression;
import de.skuzzle.polly.core.parser.ast.expressions.Inspect;
import de.skuzzle.polly.core.parser.ast.expressions.NamespaceAccess;
import de.skuzzle.polly.core.parser.ast.expressions.OperatorCall;
import de.skuzzle.polly.core.parser.ast.expressions.Problem;
import de.skuzzle.polly.core.parser.ast.expressions.VarAccess;
import de.skuzzle.polly.core.parser.ast.expressions.literals.BooleanLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.ChannelLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.DateLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.FunctionLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.HelpLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.ListLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.NumberLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.ProductLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.StringLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.TimespanLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.UserLiteral;
import de.skuzzle.polly.core.parser.ast.lang.Operator.OpType;
import de.skuzzle.polly.core.parser.problems.ProblemReporter;
import de.skuzzle.polly.core.parser.problems.Problems;
import de.skuzzle.polly.tools.collections.LinkedStack;
import de.skuzzle.polly.tools.collections.Stack;


/**
 * <p>This class provides recursive descent parsing for polly expressions and can output
 * an abstract syntax tree for the parsed expression. The root of the AST is represented
 * by the class {@link Root}, all AST nodes are subclasses of {@link Node}.
 * Every AST node that is created by this parser gets assigned its actual 
 * {@link Position} within the input string. This allows to provide detailed error
 * message during parsing, type-checking or execution of the AST.</p>
 * 
 * <p>This parser uses the following context-free syntax, given in EBNF. There may exist
 * some tweaks in the implementation that are not expressed in the following grammar.</p>
 * 
 * <pre>
 *   root        -> ':' ID (assign (WS assign)*)?              // AST root with a WS separated list of expressions
 *   
 *   directives  -> directive (',' directive)*
 *   directive   -> DELAY secTerm
 *                | REINTERPRET
 *   
 *   assign      -> relation '->' PUBLIC? TEMP? ID             // assignment of relation to identifier X
 *   relation    -> conjunction (REL_OP conjunction)*          // relation (<,>,<=,>=,==, !=)
 *   conjunction -> disjunction (CONJ_OP disjunction)*         // conjunction (||)
 *   disjunction -> secTerm (DISJ_OP secTerm)*                 // disjunction (&&)
 *   secTerm     -> term (SECTERM_OP term)*                    // plus minus
 *   term        -> factor (TERM_OP factor)*                   // multiplication and co
 *   factor      -> postfix (FACTOR_OP factor)?                // right-associative (power operator)
 *   postfix     -> autolist (POSTFIX_OP autolist)*            // postfix operator
 *   autolist    -> dotdot (';' dotdot)*                       // implicit list literal
 *   dotdot      -> unary ('..' unary ('$' unary)?)?           // range operator with optional step size
 *   unary       -> UNARY_OP unary                             // right-associative unary operator
 *                | call
 *   call        -> access ( '(' parameters ')' )?
 *   access      -> literal ('.' literal )?                    // namespace access. left operand must be a single identifier (represented by a VarAccess)
 *   literal     -> ID                                         // VarAccess
 *                | '(' relation ')'                           // braced expression
 *                | '\(' parameters ':' relation ')'           // lambda function literal
 *                | '{' exprList '}'                           // concrete list of expressions
 *                | DELETE PUBLIC? ID (',' PUBLIC? ID)*        // delete operator
 *                | INSPECT  PUBLIC ID                         // inspect for public
 *                | INSPECT ID ('.' ID)?                       // inspect operator
 *                | IF relation ':' relation ':' relation      // conditional operator
 *                | TRUE | FALSE                               // boolean literal
 *                | CHANNEL                                    // channel literal
 *                | USER                                       // user literal
 *                | STRING                                     // string literal
 *                | NUMBER                                     // number literal
 *                | DATETIME                                   // date liter
 *                | TIMESPAN                                   // timespan literal
 *                | '?'                                        // HELP literal
 *                | RADIX literal                              // radix operator
 *            
 *   exprList    -> (relation (',' relation)*)?
 *   parameters  -> (parameter (',' parameter)*)?
 *   parameter   -> type? ID
 *   type        -> ID                                         // primitive type
 *                | LIST '&lt;' type '&gt;'                    // list type
 *                | '(' (type (WS type)*)? '->' type ')'       // function type
 *                | '?'
 *                
 *   WS       -> ' ' | \t
 *   TEMP     -> 'temp'
 *   PUBLIC   -> 'public'
 *   IF       -> 'if'
 *   TRUE     -> 'true'
 *   FALSE    -> 'false'
 *   CHANNEL  -> '#' ID
 *   USER     -> '@' ID
 *   STRING   -> '"' .* '"'
 *   NUMBER   -> [0-9]*(\.[0-9]+([eE][0-9]+)?)?
 *   TIMESPAN -> ([0-9]+[ywdhms])+
 *   DATE     -> [0-9]{1,2}\.[0-9]{1,2}\.[0-9]{4}
 *   TIME     -> [0-9]{1,2}:[0-9]{1,2}
 *   DATETIME -> TIME | DATE | DATE@TIME
 *   ID       -> [_a-zA-Z][_a-zA-Z0-9]+
 *             | '\' .                                         // any escaped token
 * </pre>
 * 
 * <p>This parser has simple support to report multiple problems during parsing. For 
 * incomplete expressions, {@link Problem} nodes are inserted in the resulting AST. For
 * missing types will be created temporary types and the same applies to missing 
 * identifiers. Occurring problems will be reported to the outside using a 
 * {@link ProblemReporter} instance.</p>
 * 
 * @author Simon Taddiken
 */
public class InputParser {

    /** Operator precedence table */
    protected final PrecedenceTable operators;
    
    /** Stack which contains closing token types for currently parsed sub expressions */
    private final Stack<TokenType> expressions;
    
    /** Scanner that reads tokens from the input */
    protected InputScanner scanner;
    
    /** Cache for missing type references */
    private final Map<String, Type> typeCache = new HashMap<String, Type>();
    
    /** ID generator for missing identifiers */
    private int missingId;
    
    /** Used to report problems during parsing */
    private final ProblemReporter reporter;
    
    
    
    /**
     * Creates a new parser which will use the provided scanner to read the tokens from.
     * It will use the same {@link ProblemReporter} as the provided scanner.
     * 
     * @param scanner The {@link InputScanner} which provides the token stream.
     * @param reporter The ProblemReporter for this parser.
     */
    public InputParser(InputScanner scanner, ProblemReporter reporter) {
        this.scanner = scanner;
        this.operators = new PrecedenceTable();
        this.expressions = new LinkedStack<TokenType>();
        this.reporter = reporter;
    }
    
    
    
    /**
     * Creates a new parser which will parse the given input string using the default 
     * encoding.
     * 
     * @param input The string to parse.
     * @param reporter The ProblemReporter for this parser.
     */
    public InputParser(String input, ProblemReporter reporter) {
        this.scanner = new InputScanner(input);
        this.operators = new PrecedenceTable();
        this.expressions = new LinkedStack<TokenType>();
        this.reporter = reporter;
    }
    
    
    
    /**
     * Creates a new parser which will parse the given input string using the provided
     * encoding.
     * 
     * @param input The string to parse.
     * @param encoding The charset name to use.
     * @param reporter The ProblemReporter for this parser.
     * @throws UnsupportedEncodingException If the charset name was invalid.
     */
    public InputParser(String input, String encoding, ProblemReporter reporter) 
            throws UnsupportedEncodingException {
        this.scanner = new InputScanner(input, Charset.forName(encoding));
        this.operators = new PrecedenceTable();
        this.expressions = new LinkedStack<TokenType>();
        this.reporter = reporter;
    }
    
    
    
    /**
     * Tries to parse the input string and returns the root of the AST. If the string was
     * not valid, this method returns <code>null</code>.
     * 
     * @return The parsed AST root or <code>null</code> if the string was not well
     *          formatted.
     */
    public Root tryParse() {
        try {
            return this.parse();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    
    /**
     * Parses the input string and returns the root of the AST. If the string was not 
     * valid, this method will throw a {@link ParseException}.
     * 
     * @return The parsed AST root.
     * @throws ParseException If the string was not well formatted.
     */
    public Root parse() throws ParseException {
        return this.parseRoot();
    }
    
    
    
    /**
     * Parses a single polly expression with no assignments and command prefix.
     * 
     * @return The parsed AST expression.
     * @throws ParseException If the string was not well formatted.
     */
    public Expression parseSingleExpression() throws ParseException {
        final Expression result = this.parseRelation();
        this.expect(TokenType.EOS, false);
        return result;
    }
    
    
    
    /**
     * Creates a new {@link Identifier} with a generated name.
     * 
     * @param position Position of the generated identifier.
     * @return A new identifier.
     */
    private Identifier missingIdentifier(Position position) {
        return new Identifier(position, "$missing_" + (this.missingId++)); //$NON-NLS-1$
    }
    
    
    
    /**
     * Tries to look up a primitive type by name. If no such type exists, a new
     * temporary type with the requested name is created and stored in a cache. The
     * next time a type with the same name is requested, that cached type will
     * be returned. If polymorphism is allowed by {@link ParserProperties}, this method
     * will create and cache a type variable of the passed identifiers name.
     * 
     * @param name Type name to resolve.
     * @return The resolved type.
     * @throws ParseException 
     */
    private Type lookupType(Identifier name) throws ParseException {
        Type result = Type.resolve(name);
        if (result == null) {
            result = this.typeCache.get(name.getId());
            
            if (result == null 
                && ParserProperties.should(ParserProperties.ALLOW_POLYMORPHIC_DECLS)) {
                
                result = Type.newTypeVar(name);
            } else if (result == null) {
                result = new MissingType(name);
                this.reporter.semanticProblem(Problems.UNKNOWN_TYPE, name.getPosition(), 
                    name);
            }
            this.typeCache.put(name.getId(), result);
        }
        return result;
    }
    
    
    
    /**
     * Reports a syntax error when an unexpected token is hit.
     * 
     * @param expected The token that was expected.
     * @param actual The token that actually occurred.
     * @throws ParseException
     */
    protected void reportExpected(TokenType expected, Token actual) 
            throws ParseException {
        /*if (actual.matches(TokenType.CLOSEDBR)) {
            this.reporter.syntaxProblem(Problems.MISSING_OBR, 
                this.scanner.spanFrom(actual));
        } else {*/
            this.reporter.syntaxProblem(expected, actual, this.scanner.spanFrom(actual));
        //}
    }
    
    
    
    /**
     * Expects the next token to have the type <code>expected</code>. If the next token is
     * the expected one, it is consumed. If the next token represents a lexical error or 
     * has not the expected type, a problem is reported.
     * 
     * <p>If a problem occurred and <code>insert</code> is <code>true</code>, this method
     * pretends that the occurred token was the expected one and does not consume the
     * token that occurred instead.</p>
     * 
     * <p>If <code>insert</code> is <code>false</code>, the token that occurred instead 
     * of the expected one is consumed. This behaves like replacing the unexpected token
     * with the expected.</p>
     * 
     * 
     * @param expected Expected token type.
     * @param insert Whether method should pretend that expected token occurred if it
     *          does not.
     * @throws ParseException If the ProblemRetporter does not support multiple problems.
     */
    protected void expect(TokenType expected, boolean insert) throws ParseException {
        final Token la = this.scanner.lookAhead();
        if (la.matches(TokenType.ERROR)) {
            // report lexical error
            this.scanner.consume();
            this.reporter.lexicalProblem(la.getStringValue(), la.getPosition());
            if (!insert) {
                this.scanner.pushBackFirst(la);
            }
        } else if (!la.matches(expected)) {
            // report unexpected token
            this.scanner.consume();
            this.reportExpected(expected, la);
            this.scanner.pushBackFirst(la);
        }
        if (!insert || la.matches(expected)) {
            // consume if token should not be inserted or was the expected one
            this.scanner.consume();
        }
    }
    
    
    
    /**
     * Expects the next token to be an {@link Identifier}. If it is, it will be consumed
     * and a new Identifier will be returned. If the next token represents a lexical
     * error or is no identifier, a problem is reported.
     * 
     * @return An {@link Identifier} created from the next token.
     * @throws ParseException If the next token is no identifier.
     */
    protected Identifier expectIdentifier() throws ParseException {
        final Token la = this.scanner.lookAhead();
        if (la.matches(TokenType.ERROR)) {
            // report lexical error
            this.reporter.lexicalProblem(la.getStringValue(), la.getPosition());
        } else if (ParserProperties.should(ParserProperties.ENABLE_TOKEN_ESCAPING) && 
                la.matches(TokenType.ESCAPED)) {
            // create escaped identifier
            this.scanner.consume();
            final EscapedToken esc = (EscapedToken) la;
            return new Identifier(esc.getPosition(), esc.getEscaped().getStringValue(), 
                true);
        } else if (!la.matches(TokenType.IDENTIFIER)) {
            // report missing identifier
            this.scanner.consume();
            this.reportExpected(TokenType.IDENTIFIER, la);
            this.scanner.pushBackFirst(la);
            return this.missingIdentifier(la.getPosition());
        }
        this.scanner.consume();
        return new Identifier(la.getPosition(), la.getStringValue());
    }

    
    
    /**
     * Consumes a single whitespace if the next token is one. If not, nothing happens.
     * @throws ParseException If parsing fails.
     */
    protected void allowSingleWhiteSpace() throws ParseException {
        if (!this.scanner.skipWhiteSpaces()) {
            this.scanner.match(TokenType.SEPERATOR);
        }
    }
    
    
    
    /**
     * Enters a new sub expression. If at least one expression is "entered", the scanner 
     * will ignore whitespaces.
     * @param end The tokentype that could close this sub expression.
     */
    protected void enterExpression(TokenType end) {
        this.expressions.push(end);
        this.scanner.setSkipWhiteSpaces(true);
    }
    
    
    
    /**
     * Determines whether we currently parse a subexpression (<=> whether whitespaces
     * are skipped.
     * 
     * @return Whether we are currently parsing a subexpression where whitespaces are
     *          allowed.
     */
    protected boolean inExpression() {
        return !this.expressions.isEmpty();
    }
    
    
    
    /**
     * Leaves an entered expression. If the last expression was left, the scanner will 
     * stop ignoring whitespaces.
     */
    protected void leaveExpression() {
        this.expressions.pop();
        if (this.expressions.isEmpty()) {
            this.scanner.setSkipWhiteSpaces(false);
        }
    }
    
    
    
    protected Root parseRoot() throws ParseException {
        
        Root root = null;
        Token la = null;
        Position start = null;
        try {
            la = this.scanner.lookAhead();
            start = la.getPosition();
            
            if (!this.scanner.match(TokenType.COLON)) {
                return null;
            }
            la = this.scanner.lookAhead();
            if (!this.scanner.match(TokenType.IDENTIFIER)) {
                return null;
            }
        } catch (ParseException ignore) {
            // if an error occurs at this early stage of parsing, return null to 
            // show that input was invalid.
            return null;
        }
        
        final Identifier cmd = new Identifier(
            new Position(start.getStart(), la.getPosition().getEnd()),
            la.getStringValue());
        
        // min length hack to ignore smilies
        if (cmd.getId().length() < ParserProperties.getInt(
            ParserProperties.COMMAND_MIN_LENGTH)) {
            return null;
        }
        
        final List<Expression> signature = new ArrayList<Expression>();
        if (this.scanner.match(TokenType.SEPERATOR)) {
            do {
                final Expression next = this.parseAssignment();
                signature.add(next);
            } while (this.scanner.match(TokenType.SEPERATOR));
        }
        
        final Map<TokenType, Directive> directives = new HashMap<>();
        if (this.scanner.match(TokenType.COMMA)) {
            this.parseDirectives(directives);
        }
        
        this.expect(TokenType.EOS, false);
        root = new Root(this.scanner.spanFrom(start), cmd, signature, 
            this.reporter.hasProblems(), directives);
        
        return root;
    }
    
    
    
    /**
     * Parses a list of comma separated directives
     * <pre>
     * directives -> directive (',' directive)*
     * </pre>
     * @param directives List into which parsed directives are inserted
     * @throws ParseException If parsing fails.
     */
    protected void parseDirectives(Map<TokenType, Directive> directives) throws ParseException {
        do {
            final Directive dir = this.parseDirective();
            if (directives.containsKey(dir.getDirectiveType())) {
                // directive already exists
                this.reporter.semanticProblem(Problems.DUPLICATED_DIRECTIVE, 
                        dir.getPosition(), dir.getDirectiveType());
            } else {
                directives.put(dir.getDirectiveType(), dir);
            }
            
        } while (this.scanner.match(TokenType.COMMA));
    }
    
    
    
    /**
     * Parses a single directive
     * <pre>
     * directive -> DELAY ' ' secTerm
     *            | REINTERPRET
     * </pre>
     * @return The parsed directive
     * @throws ParseException If parsing fails.
     */
    protected Directive parseDirective() throws ParseException {
        final Token la = this.scanner.lookAhead();
        
        switch (la.getType()) {
        case DELAY:
            this.scanner.consume();
            this.expect(TokenType.SEPERATOR, true);
            final Expression target = this.parseSecTerm();
            return new DelayDirective(this.scanner.spanFrom(la), target);

        case REINTERPRET:
            this.scanner.consume();
            return new ReinterpretDirctive(this.scanner.spanFrom(la));
        default:
            this.expect(TokenType.DIRECTIVE, true);
            return new ProblemDirective(this.scanner.spanFrom(la));
        }
    }

    
    
    /**
     * Parses an assignment. If no ASSIGN_OP is found, the result of the next
     * higher precedence level is returned. This is the root of all expressions and has 
     * thus lowest precedence level.
     * <pre>
     * assign -> relation '->'PUBLIC? TEMP? ID // assignment of relation to identifier X
     * </pre>
     * @return The parsed Assignment or the result of the next higher precedence level
     *          if no ASSIGN_OP was found.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseAssignment() throws ParseException {
        final Expression lhs = this.parseRelation();
        
        if (this.scanner.match(TokenType.ASSIGNMENT)) {
            this.allowSingleWhiteSpace();
            final boolean pblc = this.scanner.match(TokenType.PUBLIC);
            this.allowSingleWhiteSpace();
            final boolean temp = this.scanner.match(TokenType.TEMP);
            this.allowSingleWhiteSpace();
            
            final Identifier id = this.expectIdentifier();
            
            return new Assignment(
                new Position(lhs.getPosition(), id.getPosition()), 
                lhs, id, pblc, temp);
        }
        return lhs;
    }
    
    
    
    /**
     * Parses RELATION precedence level operators.
     * <pre>
     * relation -> conjunction (REL_OP conjunction)*     // relation (<,>,<=,>=,==)
     * </pre>
     * @return The parsed operator call or the result from the next higher precedence 
     *          level if no REL_OP was found.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseRelation() throws ParseException {
        Expression expr = this.parseConjunction();
        
        Token la = this.scanner.lookAhead();
        while (this.operators.match(la, PrecedenceLevel.RELATION)) {
            this.scanner.consume();
            
            // ISSUE #12: this was a right shift
            if (la.matches(TokenType.GT) && this.scanner.lookAhead().matches(TokenType.GT)) {
                return expr;
            }
            
            final Expression rhs = this.parseConjunction();
            
            expr = OperatorCall.binary(
                new Position(expr.getPosition(), rhs.getPosition()), 
                OpType.fromToken(la), expr, rhs);
            
            la = this.scanner.lookAhead();
        }
        return expr;
    }
    
    
    
    /**
     * Parses CONJUNCTION precedence level operators.
     * <pre>
     * conjunction -> disjunction (CONJ_OP disjunction)*   // conjunction (||)
     * </pre>
     * @return The parsed operator call or the result from the next higher precedence 
     *          level if no CONJ_OP was found.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseConjunction() throws ParseException {
        Expression expr = this.parseDisjunction();
        
        Token la = this.scanner.lookAhead();
        while (this.operators.match(la, PrecedenceLevel.CONJUNCTION)) {
            this.scanner.consume();
            
            final Expression rhs = this.parseDisjunction();
            
            expr = OperatorCall.binary(
                new Position(expr.getPosition(), rhs.getPosition()), 
                OpType.fromToken(la), expr, rhs);
            
            la = this.scanner.lookAhead();
        }
        return expr;
    }
    
    
    
    /**
     * Parses DISJUNCTION precedence level operators.
     * <pre>
     * disjunction -> secTerm (DISJ_OP secTerm)*     // disjunction (&&)
     * </pre>
     * @return The parsed operator call or the result from the next higher precedence 
     *          level if no DISJ_OP was found.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseDisjunction() throws ParseException {
        Expression expr = this.parseSecTerm();
        
        Token la = this.scanner.lookAhead();
        while (this.operators.match(la, PrecedenceLevel.DISJUNCTION)) {
            this.scanner.consume();
            
            final Expression rhs = this.parseSecTerm();
            
            expr = OperatorCall.binary(
                new Position(expr.getPosition(), rhs.getPosition()), 
                OpType.fromToken(la), expr, rhs);
            
            la = this.scanner.lookAhead();
        }
        return expr;
    }
    
    
    
    /**
     * Parses SECTERM precedence level operators.
     * <pre>
     * secTerm -> term (SECTERM_OP term)*   // plus minus
     * </pre>
     * @return The parsed operator call or the result from the next higher precedence 
     *          level if no SECTERM_OP was found.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseSecTerm() throws ParseException {
        Expression expr = this.parseTerm();
        
        Token la = this.scanner.lookAhead();
        while (this.operators.match(la, PrecedenceLevel.SECTERM)) {
            this.scanner.consume();
            
            // ISSUE #12: this is a (unsigned) right shift
            if (la.matches(TokenType.GT) && this.scanner.match(TokenType.GT)) {
                if (this.scanner.match(TokenType.GT)) {
                    la = new Token(TokenType.URIGHT_SHIFT, this.scanner.spanFrom(la));
                } else {
                    la = new Token(TokenType.RIGHT_SHIFT, this.scanner.spanFrom(la));
                }
            }
            
            
            final Expression rhs = this.parseTerm();
            
            expr = OperatorCall.binary(
                new Position(expr.getPosition(), rhs.getPosition()), 
                OpType.fromToken(la), expr, rhs);
            
            la = this.scanner.lookAhead();
        }
        return expr;
    }
    
    
    
    /**
     * Parses TERM precedence level operators.
     * <pre>
     * term -> factor (TERM_OP factor)*  // multiplication and co
     * </pre>
     * @return The parsed operator call or the result from the next higher precedence 
     *          level if no TERM_OP was found.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseTerm() throws ParseException {
        Expression expr = this.parseFactor();
        
        Token la = this.scanner.lookAhead();
        while (this.operators.match(la, PrecedenceLevel.TERM)) {
            // ISSUE 0000099: If Identifier or open brace, do not consume the token but
            //                pretend it was a multiplication
            if (la.matches(TokenType.IDENTIFIER) || la.matches(TokenType.OPENBR)) {
                la = new Token(TokenType.MUL, la.getPosition());
            } else {
                this.scanner.consume();
            }
            final Expression rhs = this.parseFactor();
            
            expr = OperatorCall.binary(
                new Position(expr.getPosition(), rhs.getPosition()), 
                OpType.fromToken(la), expr, rhs);
            
            la = this.scanner.lookAhead();
        }
        return expr;
    }
    
    
    
    /**
     * Parses FACTOR precedence operators. Result will be a nested OperatorCall or the
     * result of the next higher precedence level if no FACTOR_OP was found.
     * FACTOR operators are right-associative.
     * <pre>
     * factor -> postfix (FACTOR_OP factor)?    // right-associative (power operator)
     * </pre>
     * @return The parsed operator call or the result from the next higher precedence 
     *          level if no FACTOR_OP was found.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseFactor() throws ParseException {
        Expression expr = this.parsePostfix();
        
        Token la = this.scanner.lookAhead();
        if (this.operators.match(la, PrecedenceLevel.FACTOR)) {
            this.scanner.consume();
            final Expression rhs = this.parseFactor();
            
            expr = OperatorCall.binary(
                new Position(expr.getPosition(), rhs.getPosition()), 
                OpType.fromToken(la), expr, rhs);
        }
        return expr;
    }
    
    
    
    /**
     * Parses a postfix operator. This may be either of the random index- or the
     * concrete index operator.
     * <pre>
     * postfix -> autolist (POSTFIX_OP autolist)*       // postfix operator
     * </pre>
     * @return Either the parsed postifx operator call or the expression from the next
     *          higher precedence level if no POSTFIX_OP was found.
     * @throws ParseException If parsing fails.
     */
    protected Expression parsePostfix() throws ParseException {
        Expression lhs = this.parseAutoList();
        
        Token la = this.scanner.lookAhead();
        while (this.operators.match(la, PrecedenceLevel.POSTFIX)) {
            this.scanner.consume();
            
            if (la.matches(TokenType.OPENSQBR)) {
                // index operator
                final Expression rhs = this.parseAutoList();
                
                this.expect(TokenType.CLOSEDSQBR, true);
                
                lhs = OperatorCall.binary(
                    this.scanner.spanFrom(la), 
                    OpType.fromToken(la), lhs, rhs);
            } else {
                // ? or ?! operator
                final Position endPos = this.scanner.spanFrom(la);
                return OperatorCall.unary(
                    new Position(lhs.getPosition(), endPos), 
                    OpType.fromToken(la), lhs, true);
            }
            la = this.scanner.lookAhead();
        }
        
        return lhs;
    }
    
    
    
    /**
     * Parses an implicit list literal.
     * <pre>
     * autolist -> dotdot (';' dotdot)*   // implicit list literal
     * </pre>
     * @return Either a {@link ListLiteral} containing the following expressions or the 
     *          expression returned by the next higher precedence level.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseAutoList() throws ParseException {
        Expression lhs = this.parseDotDot();
        
        final Token la = this.scanner.lookAhead();
        if (la.matches(TokenType.SEMICOLON)) {
            final List<Expression> content = new ArrayList<Expression>();
            content.add(lhs);

            Expression last = null;
            while (this.scanner.match(TokenType.SEMICOLON)) {
                last = this.parseDotDot();
                content.add(last);
            }
            
            // invariant: last cannot be null here!
            return new ListLiteral(
                new Position(lhs.getPosition(), last.getPosition()), 
                content);
        }
        
        return lhs;
    }
    
    
    
    /**
     * Parses the '..' range operator, which can either be a binary operator or
     * a ternary operator if an additional step size is recognized.
     * 
     * <pre>
     * dotdot -> unary ('..' unary ('$' unary)?)?   // range operator with optional 
     *                                              // step size
     * </pre>
     * @return The parsed operator or the expression from the next higher precedence level
     *          if no DOTDOT operator was found. 
     * @throws ParseException If parsing fails.
     */
    protected Expression parseDotDot() throws ParseException {
        final Expression lhs = this.parseUnary();
        
        final Token la = this.scanner.lookAhead();
        if (this.operators.match(la, PrecedenceLevel.DOTDOT)) {
            this.scanner.consume();
            
            final Expression endRange = this.parseUnary();
            
            // default step width of 1 (if dollar is ommitted)
            Expression operand3 = new NumberLiteral(endRange.getPosition(), 1.0);
            if (this.scanner.match(TokenType.DOLLAR)) {
                operand3 = this.parseUnary();
            }
            return OperatorCall.ternary(
                new Position(lhs.getPosition(), operand3.getPosition()), 
                OpType.fromToken(la), lhs, endRange, operand3);
        }
        return lhs;
    }
    
    
    
    /**
     * Parses an unary operator call.
     * <pre>
     * unary -> UNARY_OP unary    // right-associative unary operator
     *        | call
     * </pre>
     * @return A unary operator call or the expression returned by the next higher
     *          precedence level if no UNARY_OP was found.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseUnary() throws ParseException {
        final Token la = this.scanner.lookAhead();
        
        if (this.operators.match(la, PrecedenceLevel.UNARY)) {
            this.scanner.consume();
            final Expression rhs = this.parseUnary();
            return OperatorCall.unary(new Position(la.getPosition(), 
                    rhs.getPosition()), OpType.fromToken(la), rhs, false);
        } else {
            return this.parseCall();
        }
    }
    
    
    
    /**
     * Parses a function call. If no open braces was matched, the result of the next
     * higher precedence level will be returned.
     * 
     * <pre>
     * call -> access ( '(' parameters ')' )?
     * </pre>
     * @return The call statement of the result of the next higher precedence level if
     *          this was no call.
     * @throws ParseException If parsing fails
     */
    protected Expression parseCall() throws ParseException {
        final Expression lhs = this.parseNamespaceAccess();
        
        final Token la = this.scanner.lookAhead();
        if (this.scanner.match(TokenType.OPENBR)) {
            final List<Expression> params = this.parseExpressionList(
                TokenType.CLOSEDBR);
            final ProductLiteral pl = new ProductLiteral(
                this.scanner.spanFrom(la), params);
            this.expect(TokenType.CLOSEDBR, true);
            
            return new Call(
                new Position(lhs.getPosition().getStart(), this.scanner.getStreamIndex()), 
                lhs, pl);
        }
        return lhs;
    }
    
    
    
    /**
     * Parses a {@link Namespace} access.
     * <pre>
     * access -> literal ('.' literal)?
     * </pre>
     * 
     * @return The parsed literal if no DOT operator was found, or a {@link Namespace}
     *          access if the was a dot followed by a VarOrCall.
     * @throws ParseException If parsing fails
     */
    protected Expression parseNamespaceAccess() throws ParseException {
        final Expression lhs = this.parseLiteral();
        
        final Token la = this.scanner.lookAhead();
        if (this.scanner.match(TokenType.DOT)) {
            final Expression rhs = this.parseLiteral();
            
            return new NamespaceAccess(new Position(lhs.getPosition(), 
                this.scanner.spanFrom(la)), lhs, rhs);
        }
        
        return lhs;
    }
    
    
    
    /**
     * Parses the highest precedence level which is mostly a single literal, but also 
     * a delete or if statement.
     * 
     * <pre>
     * literal -> ID                                       // VarAccess
     *          | ESCAPED                                  // token escape
     *          | '(' relation ')'                         // braced expression
     *          | '\(' parameters ':' relation ')'         // lambda function literal
     *          | '{' exprList '}'                         // concrete list of expressions
     *          | DELETE PUBLIC? ID (',' PUBLIC? ID)*      // delete operator
     *          | INSPECT PUBLIC ID                        // inspect for public
     *          | INSPECT ID ('.' ID)?                     // inspect operator
     *          | IF expr ':' relation ':' relation        // conditional operator
     *          | TRUE | FALSE                             // boolean literal
     *          | CHANNEL                                  // channel literal
     *          | USER                                     // user literal
     *          | STRING                                   // string literal
     *          | NUMBER                                   // number literal
     *          | DATETIME                                 // date literal
     *          | TIMESPAN                                 // timespan literal
     *          | '?'                                      // HELP literal
     *          | RADIX literal                            // radixed int
     * </pre>
     *  
     * @return The parsed expression.
     * @throws ParseException If parsing fails.
     */
    protected Expression parseLiteral() throws ParseException {      
        final Token la = this.scanner.lookAhead();
        Expression exp = null;
        
        switch(la.getType()) {
        case ESCAPED:
            this.scanner.consume();
            final EscapedToken escaped = (EscapedToken) la;
            final ResolvableIdentifier escId = new ResolvableIdentifier(la.getPosition(), 
                escaped.getEscaped().getStringValue(), true);
            return new VarAccess(la.getPosition(), escId);
            
        case IDENTIFIER:
            this.scanner.consume();
            final ResolvableIdentifier id = new ResolvableIdentifier(
                    la.getPosition(), la.getStringValue(), false);
            return new VarAccess(id.getPosition(), id);
            
        case OPENBR:
            this.scanner.consume();
            /*
             * Now we can ignore whitespaces until the matching closing brace is 
             * read.
             */
            this.enterExpression(TokenType.CLOSEDBR);
            
            exp = this.parseRelation();
            this.expect(TokenType.CLOSEDBR, true);
            
            this.leaveExpression();
            return new Braced(this.scanner.spanFrom(la), exp);
            
        case LAMBDA:
            this.scanner.consume();
            
            this.enterExpression(TokenType.CLOSEDBR);
            
            final Collection<Declaration> formal = this.parseParameters(
                TokenType.COLON);
            this.expect(TokenType.COLON, true);
            
            exp = this.parseRelation();
            
            this.expect(TokenType.CLOSEDBR, true);
            
            final FunctionLiteral func = new FunctionLiteral(
                this.scanner.spanFrom(la), formal, exp);
            
            this.leaveExpression();
            
            return func;
            
        case OPENCURLBR:
            this.scanner.consume();
            
            this.enterExpression(TokenType.CLOSEDCURLBR);
            final List<Expression> elements = this.parseExpressionList(
                TokenType.CLOSEDCURLBR);
            
            this.expect(TokenType.CLOSEDCURLBR, true);
            this.leaveExpression();
            
            final ListLiteral list = new ListLiteral(this.scanner.spanFrom(la), 
                elements);
            
            list.setPosition(this.scanner.spanFrom(la));
            return list;
            
        case DELETE:
            this.scanner.consume();
            this.allowSingleWhiteSpace();
            
            final List<DeleteableIdentifier> ids = new ArrayList<DeleteableIdentifier>();
            do {
                this.allowSingleWhiteSpace();
                boolean global = this.scanner.match(TokenType.PUBLIC);
                if (global) {
                    this.allowSingleWhiteSpace();
                }
                ids.add(new DeleteableIdentifier(this.expectIdentifier(), global));
            } while (this.scanner.match(TokenType.COMMA));
                
            
            return new Delete(this.scanner.spanFrom(la), ids);
            
        case INSPECT:
            this.scanner.consume();
            this.allowSingleWhiteSpace();
            
            final Token glob = this.scanner.lookAhead();
            final boolean global = this.scanner.match(TokenType.PUBLIC);
            this.allowSingleWhiteSpace();
            
            final ResolvableIdentifier name = new ResolvableIdentifier(
                this.expectIdentifier());
            final VarAccess va1 = new VarAccess(name.getPosition(), name);
            Expression result = va1;
            
            if (global) {
                // syntactic sugar for global inspect
                final ResolvableIdentifier name2 = new ResolvableIdentifier(
                    glob.getPosition(), Namespace.PUBLIC_NAMESPACE_NAME);
                final VarAccess va2 = new VarAccess(name2.getPosition(), name2);
                result = new NamespaceAccess(this.scanner.spanFrom(la), va2, va1);
                
            } else if (this.scanner.match(TokenType.DOT)) {
                final ResolvableIdentifier name2 = new ResolvableIdentifier(
                    this.expectIdentifier());
                final VarAccess va2 = new VarAccess(name2.getPosition(), name2);
                
                result = new NamespaceAccess(this.scanner.spanFrom(la), va1, va2);
            }
            return new Inspect(this.scanner.spanFrom(la), result, global);
            
        case IF:
            this.scanner.consume();
            this.allowSingleWhiteSpace();
            
            final Expression condition = this.parseRelation();
            this.allowSingleWhiteSpace();
            
            this.expect(TokenType.COLON, true);
            this.allowSingleWhiteSpace();
            
            final Expression second = this.parseRelation();
            
            this.allowSingleWhiteSpace();
            this.expect(TokenType.COLON, true);
            this.allowSingleWhiteSpace();
            
            final Expression third = this.parseRelation();
            
            return OperatorCall.ternary(this.scanner.spanFrom(la), OpType.IF, 
                condition, second, third);
            
        case TRUE:
            this.scanner.consume();
            return new BooleanLiteral(la.getPosition(), true);
            
        case FALSE:
            this.scanner.consume();
            return new BooleanLiteral(la.getPosition(), false);
                
        case CHANNEL:
            this.scanner.consume();
            return new ChannelLiteral(la.getPosition(), la.getStringValue());

        case USER:
            this.scanner.consume();
            return new UserLiteral(la.getPosition(), la.getStringValue());
            
        case STRING:
            this.scanner.consume();
            return new StringLiteral(la.getPosition(), la.getStringValue());

        case NUMBER:
            this.scanner.consume();
            return new NumberLiteral(la.getPosition(), la.getFloatValue());
            
        case DATETIME:
            this.scanner.consume();
            return new DateLiteral(la.getPosition(), la.getDateValue());
            
        case TIMESPAN:
            this.scanner.consume();
            return new TimespanLiteral(la.getPosition(), (int)la.getLongValue());
            
        case QUESTION:
            this.scanner.consume();
            return new HelpLiteral(la.getPosition());
            
        case RADIX:
            this.scanner.consume();
            final NumberLiteral radix = new NumberLiteral(la.getPosition(), 
                la.getLongValue());
            final Expression rhs = this.parseLiteral();
            return OperatorCall.binary(this.scanner.spanFrom(la), OpType.RADIX, 
                radix, rhs);
            
        default:
            this.expect(TokenType.LITERAL, true);
            return new Problem(this.scanner.spanFrom(la));
        }
    }
    
    
    
    /**
     * Parses a comma separated list of expressions. The <code>end</code> token type
     * exists only for determining empty lists.
     * 
     * <pre>
     * exprList -> end   // empty list
     *           | relation (',' relation)*
     * </pre>
     * @param end The token which should end the list. Only used to determine empty lists.
     * @return A collection of parsed expressions.
     * @throws ParseException If parsing fails.
     */
    protected List<Expression> parseExpressionList(TokenType end) 
            throws ParseException {
        
        // do not consume here. end token is consume by the caller
        if (this.scanner.lookAhead().matches(end)) {
            // empty list
            return new ArrayList<Expression>(0);
        }
        
        this.enterExpression(end);
        final List<Expression> result = new ArrayList<Expression>();
        result.add(this.parseRelation());
        
        while (this.scanner.match(TokenType.COMMA)) {
            this.allowSingleWhiteSpace();
            result.add(this.parseRelation());
        }
        this.leaveExpression();
        return result;
    }
    
    
    
    /**
     * Parses a list of formal parameters that ends with the token type <code>end</code>.
     * 
     * <pre>
     * parameters -> end   // empty list
     *             | parameter (',' parameter)*
     * </pre>
     * @param end The token that the list is supposed to end with (to determine empty 
     *          lists, token won't be consumed if hit).
     * @return Collection of parsed formal parameters.
     * @throws ParseException If parsing fails.
     */
    protected List<Declaration> parseParameters(TokenType end) 
            throws ParseException {
        if (this.scanner.lookAhead().matches(end)) {
            // empty list.
            return new ArrayList<Declaration>(0);
        }
        
        this.enterExpression(end);
        final List<Declaration> result = new ArrayList<Declaration>();
        result.add(this.parseParameter());
        
        while (this.scanner.match(TokenType.COMMA)) {
            this.allowSingleWhiteSpace();
            result.add(this.parseParameter());
        }
        this.leaveExpression();
        return result;
    }
    
    
    
    /**
     * <pre>
     * parameter -> type? ID
     * </pre>
     * @return The parsed parameter.
     * @throws ParseException If parsing fails.
     */
    protected Declaration parseParameter() throws ParseException {
        final Type type;
        final Identifier name;
        final Token la = this.scanner.lookAhead();
        if (la.matches(TokenType.IDENTIFIER)) {
            this.scanner.consume();
            
            final Token la2 = this.scanner.lookAhead();
            if (la2.matches(TokenType.IDENTIFIER)) {
                // ID ID
                type = this.lookupType(new Identifier(la.getPosition(), 
                    la.getStringValue()));
                name = this.expectIdentifier();
            } else {
                type = Type.newTypeVar();
                name = new Identifier(la.getPosition(), la.getStringValue());
            }
        } else {
            type = this.parseType();
            name = this.expectIdentifier();
        }
        
        return new Declaration(this.scanner.spanFrom(la), name, 
            new Empty(type, this.scanner.spanFrom(la)));
    }
    
    
    
    /**
     * <pre>
     * type -> ID                                     // primitive type
     *       | LIST '&lt;' type '&gt;'                // list type
     *       | '(' (type (WS type)*)? '->' type ')'   // function type
     *       | '?'
     * </pre>
     * @return A resolvable type.
     * @throws ParseException If parsing fails.
     */
    protected Type parseType() throws ParseException {
        if (this.scanner.match(TokenType.OPENBR)) {
            final List<Type> signature = new ArrayList<Type>();
            final boolean skipWS = this.scanner.skipWhiteSpaces();
            this.scanner.setSkipWhiteSpaces(false);
            
            do {
                if (scanner.lookAhead().matches(TokenType.ASSIGNMENT)) {
                    break;
                }
                signature.add(this.parseType());
            } while (this.scanner.match(TokenType.SEPERATOR) &&
                !this.scanner.lookAhead().matches(TokenType.ASSIGNMENT));
            
            this.scanner.setSkipWhiteSpaces(skipWS);
            this.allowSingleWhiteSpace();
            this.expect(TokenType.ASSIGNMENT, true);
            final Type resultType = this.parseType();
            this.allowSingleWhiteSpace();

            this.expect(TokenType.CLOSEDBR, true);
            
            if (signature.isEmpty()) {
                signature.add(Type.VOID);
            }
            return new ProductType(signature).mapTo(resultType);

        } else if (this.scanner.match(TokenType.LIST)) {
            this.expect(TokenType.LT, true);
            final Type subType = this.parseType();

            this.expect(TokenType.GT, true);
            return subType.listOf();
        } else if (ParserProperties.should(ParserProperties.ALLOW_POLYMORPHIC_DECLS) 
                && this.scanner.match(TokenType.QUESTION)) {
            return Type.newTypeVar();
        } else {
            return this.lookupType(this.expectIdentifier());
        }
    }
}