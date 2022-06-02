import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileWriter;

public class Compiler {
  public static void main(String[] args) throws Exception {
    // Process input file.
    String inputFile = null;
    if (args.length > 0) inputFile = args[0];
    InputStream is = System.in;
    if (inputFile != null) is = new FileInputStream(inputFile);
    ANTLRInputStream input = new ANTLRInputStream(is);

    // Phase 1: Lexing and parsing.
    SimpleCLexer lexer = new SimpleCLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SimpleCParser parser = new SimpleCParser(tokens);

    ParseTree tree = parser.program();
    System.err.println(tree.toStringTree(parser));

    // // Phase 2: Type checking.
    // TypeChecker typechecker = new TypeChecker();
    // typechecker.visit(tree);

    // // Phase 3: Intermediate code gen.
    // CodeGen codegen = new CodeGen();
    // codegen.visit(tree);

    // // Phase 4: Machine-independent optimization.

    // // Process output file.
    String inputFileNoExt = (inputFile.indexOf(".") > 0) ? inputFile.substring(0, inputFile.lastIndexOf(".")) : inputFile;
    String outputFile = inputFileNoExt + ".s";
    PrintWriter outfile = new PrintWriter(new FileWriter(outputFile));

    // // Phase 5: Machine code gen.
    // ASMGen asmgen = new ASMGen(outfile);
    // System.err.println(codegen.functionlist);
    // asmgen.gen(codegen.functionlist);

    // Cleanup output file.
    outfile.close();
  }
}
