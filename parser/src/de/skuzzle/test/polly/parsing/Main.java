package de.skuzzle.test.polly.parsing;

import java.util.Stack;

import de.skuzzle.polly.parsing.InputParser;
import de.skuzzle.polly.parsing.ParseException;
import de.skuzzle.polly.parsing.Prepare;
import de.skuzzle.polly.parsing.declarations.Namespace;
import de.skuzzle.polly.parsing.tree.Root;
import de.skuzzle.polly.parsing.tree.literals.Literal;


public class Main {

    public static void main(String[] args) throws ParseException {
        Namespace context = new Namespace().copyFor("test");
        
        Prepare.namespaces(context);
        Prepare.operators(context);
        
        InputParser p = new InputParser();
        Root r = p.parse(":foo 5->x 10*x->f(Zahl x) f(math.pi)");
        r.contextCheck(context);
        r.collapse(new Stack<Literal>());
        
        System.out.println(r.toString());
        System.out.println(context);
    }
}