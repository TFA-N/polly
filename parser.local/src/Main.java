import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.skuzzle.polly.parsing.Evaluator;
import de.skuzzle.polly.parsing.ast.declarations.Namespace;
import de.skuzzle.polly.parsing.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.parsing.ast.visitor.ASTVisualizer;
import de.skuzzle.polly.process.KillingProcessWatcher;
import de.skuzzle.polly.process.ProcessExecutor;



public class Main {
    
    private final static String DOT_PATH = 
            "C:\\Program Files (x86)\\Graphviz 2.28\\bin\\dot.exe";

    public static void main(String[] args) throws IOException, ASTTraversalException {
        final BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String nsName = "default";
        
        System.out.println("Welcome to PPC - Polly Parser Console");
        System.out.println("Just enter any valid polly expression, or type :q to exit.");
        System.out.println("Type :ns <name> to switch namespaces.");
        System.out.println();
        
        while (true) {
            Namespace ns = Namespace.forName(nsName);
            System.out.print(nsName + " > ");
            final String cmd = r.readLine();
            
            if (cmd.equals(":q")) {
                return;
            } else if (cmd.startsWith(":ns")) {
                final String[] parts = cmd.split(" ");
                if (parts.length != 2) {
                    System.out.println("Invalid parameters for ':ns'");
                } else {
                    nsName = parts[1];
                }
                
                continue;
            } else if (cmd.equals(":cns")) {
                //System.out.println();
                continue;
            }
            
            final Evaluator eval = new Evaluator(":result \">\" " + cmd, "ISO-8859-1");
            
            eval.evaluate(ns);
            
            if (eval.errorOccurred()) {
                final ASTTraversalException e = eval.getLastError();
                System.out.println(e.getMessage());
                System.out.println("            " + cmd);
                System.out.println(e.getPosition().errorIndicatorString());
            } else if (eval.getRoot() != null){
                System.out.println(eval.getRoot().toString());
                
                final ASTVisualizer av = new ASTVisualizer();
                av.toFile("lastAst.dot", eval.getRoot());
                
                ProcessExecutor pe = ProcessExecutor.getOsInstance(false);
                pe.addCommand(DOT_PATH);
                pe.addCommandsFromString("-Tpdf -o lastAst.pdf");
                pe.addCommand("lastAST.dot");
                pe.setProcessWatcher(new KillingProcessWatcher(10000, true));
                pe.start();
            }
        }
    }
}
