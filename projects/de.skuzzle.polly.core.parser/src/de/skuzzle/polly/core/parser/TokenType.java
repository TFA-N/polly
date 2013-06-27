package de.skuzzle.polly.core.parser;

public enum TokenType {
    SEPERATOR("Leerzeichen"),
    LITERAL("Ausdruck"),                     /* only for debug output */
    LIST("Liste"),
    CHANNEL("Channel"),   /* Channel literals */
    USER("User"),               /* User literals */
    IF("if"),
    RADIX("0x"),
    ASSIGNMENT("->"),
    ADD("+"), 
    SUB("-"),
    ADDEQUALS("+="),
    SUBEQUALS("-="),
    WAVE("~"), 
    ADDWAVE("+~"), 
    MUL("*"), 
    DIV("/"), 
    INTDIV("\\"),
    LAMBDA("\\("), 
    MOD("%"), 
    POWER("^"), 
    BOOLEAN_AND("&&"), 
    BOOLEAN_OR("||"), 
    AND_OR("&|"),
    XOR("^^"), 
    GT(">"), 
    LT("<"), 
    LEFT_SHIFT("<<"),
    RIGHT_SHIFT(">>"),
    URIGHT_SHIFT(">>>"),
    EQ("="), 
    NEQ("!="), 
    EGT(">="), 
    ELT("<="),
    INT_AND("&"), 
    INT_OR("|"), 
    DOTDOT(".."), 
    DOT("."),
    COMMA(","), 
    DOLLAR("$"),
    EXCLAMATION("!"), 
    QUESTION("?"),
    QUEST_EXCALAMTION("?!"), 
    COLON(":"), 
    POUND("#"),
    NUMBER("Zahl"), 
    IDENTIFIER("Bezeichner"), 
    TRUE("true"), 
    FALSE("false"), 
    STRING("String"), 
    DATETIME("Zeitangabe"),
    TIMESPAN("Zeitspanne"),
    OPENBR("("), 
    CLOSEDBR(")"), 
    OPENSQBR("["), 
    CLOSEDSQBR("]"), 
    OPENCURLBR("{"), 
    CLOSEDCURLBR("}"), 
    INDEX("Indizierung"),
    KEYWORD("Schl�sselwort"),
    UNKNOWN("Unbekanntes Zeichen"),
    COMMAND("Befehl"),
    POLLY("Polly"),
    PUBLIC("public"),
    SEMICOLON(";"),
    EOS("Ende der Eingabe"), 
    TEMP("temp"), 
    ELSE("else"), 
    DELETE("del"), 
    INSPECT("inspect"),
    ESCAPED("\\"), 
    TRANSPOSE("^T"),
    ERROR("Ung�ltiges Symbol");
    
    private String string;
    
    private TokenType(String string) {
        this.string = string;
    }
    
    
    
    @Override
    public String toString() {
        return this.string;
    }
}
