 import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;


public class TypeChecker extends SimpleCBaseVisitor<Void> {
  Map<String, SimpleCParser.FunctionContext> functions = new HashMap<String, SimpleCParser.FunctionContext>();
  Map<String, String> variables;

  public static void typeError(ParserRuleContext ctx) {
    Token firstToken = ctx.getStart();
    String file = firstToken.getTokenSource().getSourceName();
    int line = firstToken.getLine();
    int col = firstToken.getCharPositionInLine() + 1;
    System.out.println(String.format("%s:%d:%d type error", file, line, col));
    System.exit(2);
  }

  ExprVisitor exprVisitor = new ExprVisitor();

  protected class ExprVisitor extends SimpleCBaseVisitor<String> {
   @Override
     public String visitCall(SimpleCParser.CallContext ctx) {
    //   // the function name should be in the function table
    //   // the number of parameters should match
    //   // the types of the parameters should all be int for this version of simplec
    //   // the resulting type is the return type of the function, always int for this version of simplec
    // Check out symbol tables for the function'
    //Get the functions name from the call
    String function_name = ctx.ID().getText();
    int args = 0;

     if (! functions.containsKey(function_name)) {
      System.err.println("function not declared");
      typeError(ctx);

    } 

    if((ctx.actualParams() == null))
    {
        typeError(ctx);
     }
     else{
         args = ctx.actualParams().expr().size();
     }
 
    //if exists
    if(functions.containsKey(function_name)){
      SimpleCParser.FunctionContext fctx = functions.get(function_name);

      int numargs = 0;
      //write to set numargs

      if(null == fctx.formalParams()){
        numargs = 0;
      }else{
      for(TerminalNode tn: fctx.formalParams().ID()){
        numargs++;
      }
      }

      if(numargs != args)
      {
      System.err.println("function argument mismatch");
       typeError(ctx);
       return null;
      }
      

      System.err.println(fctx.formalParams());

       return "int";  // function calls should always produce an int
     }

     else{
       //handle case when it isnt defined
       System.err.println("function not defined");
       typeError(ctx);
       return null;
     }
   }
     @Override
     public String visitNegate(SimpleCParser.NegateContext ctx) {
    //   // (int) -> int
      String lhs = ctx.getText();
      lhs = lhs.substring(1);
      String type = variables.get(lhs);

      if(!"int".equals(type))
      {
        System.err.println(type);
        typeError(ctx);
        return null;
      }
      else{
        return "int";
      }

     }

     @Override
     public String visitNot(SimpleCParser.NotContext ctx) {
       // (bool) -> int
       String type = visit(ctx.expr());
       System.err.println(type);
       if(!"bool".equals(type))
       {
         typeError(ctx);
         return null;
       }
       else{
         return "int";
       }

     }
//
     @Override
     public String visitMultiDiv(SimpleCParser.MultiDivContext ctx) {
    //   // (int, int) -> int
    //   // what's the type of the left operand?
    //   // what's the type of the right operand?
    //   // if they are both int, we can conclude this term is an int
    //   // return int, because you have proven it must be an int
    //   return "";
      String lhs = visit(ctx.expr(0));
      String rhs = visit(ctx.expr(1));
      if( "int".equals(lhs) && "int".equals(rhs))
        return "int";

        else{
          System.err.println("an opperand is of wrong type in mult div");
          typeError(ctx);
          return null;
        }
     }

     @Override
     public String visitAddSub(SimpleCParser.AddSubContext ctx) {
    //    (int, int) -> int
        String lhs = visit(ctx.expr(0));
        String rhs = visit(ctx.expr(1));
        if( "int".equals(lhs) && "int".equals(rhs))
          return "int";

        else{
          System.err.println("an opperand is of wrong type in add sub");
          typeError(ctx);
          return null;
        }

     }

     @Override
     public String visitRelational(SimpleCParser.RelationalContext ctx) {
       // (int, int) -> bool
       String lhs = visit(ctx.expr(0));
       String rhs = visit(ctx.expr(1));
       if( "int".equals(lhs) && "int".equals(rhs))
       {
         return "bool";
       }
         else{
           System.err.println("in relational there is a type error");
           typeError(ctx);
           return null;
         }
     }

     @Override
     public String visitEqNeq(SimpleCParser.EqNeqContext ctx) {
       String lhs = visit(ctx.expr(0));
       String rhs = visit(ctx.expr(1));
       if( "int".equals(lhs) && "int".equals(rhs))
       {
         return "bool";
       }
         else{
           System.err.println("in EqNeq there is a type error");
           typeError(ctx);
           return null;
         }
     }

     @Override
    public String visitAndOr(SimpleCParser.AndOrContext ctx) {
       // (bool, bool) -> bool
       String lhs = visit(ctx.expr(0));
       String rhs = visit(ctx.expr(1));

       if( "bool".equals(lhs) && "bool".equals(rhs))
       {
         return "bool";
       }
         else{
           System.err.println("And or statement does not have bool");
           typeError(ctx);
           return null;
         }


     }

   @Override
     public String visitVar(SimpleCParser.VarContext ctx) {
       // lookup the variable to find its type or report an undeclared variable error
       String temp = ctx.ID().getText();
       String  type = variables.get(temp);
       if("int".equals(type) == true){
        return "int";
      }
      else if("bool".equals(type) == true){
       return "bool";
     }
     else{
        System.err.println("Undeclared variable");
       typeError(ctx);
       return type;
     }
     }

    @Override
    public String visitNum(SimpleCParser.NumContext ctx) {
      // nums are always int
      return "int";
    }
  }

  @Override
  public Void visitProgram(SimpleCParser.ProgramContext ctx) {
    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitFunction(SimpleCParser.FunctionContext ctx) {
    String name = ctx.ID().getText();
    if (! functions.containsKey(name)) {
      functions.put(name, ctx);
    } else {
      System.err.println("function already declared");
      typeError(ctx);
    }
    this.variables = new HashMap<String, String>();
    this.variables.put("true", "bool");
    this.variables.put("false", "bool");
    for (SimpleCParser.DeclContext dctx : ctx.decl()) visit(dctx);
    for (SimpleCParser.StmtContext sctx : ctx.stmt()) visit(sctx);
    return null;
  }

   @Override
  public Void visitDecl(SimpleCParser.DeclContext ctx) {
     // record the type of the declared symbol in the variable symbol table

     // this version of simplec only has variable declarations at the top of function definitions

     // use ctx.type().getStart().getText() to get the type name in the declaration
     String temp = ctx.ID().getText();
     String decl = ctx.type().getStart().getText();
     if(variables.get(temp) != null)
     {
        System.err.println("Variable already declared");
        typeError(ctx);
     }
     this.variables.put(temp,decl);

     return null;
   }

   @Override
   public Void visitAssignment(SimpleCParser.AssignmentContext ctx) {
     // the variable should have been declared already
     // the type of the target variable should match that of the expression
     String rhs_type = exprVisitor.visit(ctx.expr());
     String temp = ctx.ID().getText();

     if(variables.get(temp) == null){
       System.err.println("Undeclared variable");
       typeError(ctx);
       }

     if(! variables.get(temp).equals(rhs_type) || variables.get(temp) == null){
         System.err.println("assignment type should be " + variables.get(temp));
         typeError(ctx);
          return null;

     }


     return null;
   }

   @Override
   public Void visitWhile(SimpleCParser.WhileContext ctx) {

     // the conditional expression should be a bool
     String cond = exprVisitor.visit(ctx.expr());
     String body = exprVisitor.visit(ctx.stmt());
     if("bool".equals(cond))
     {
             return null;
     }
     else
     {
       System.err.println("While loop conditional not a bool");
       typeError(ctx);
       return null;
     }
    // the body statement should also be type safe
   }

   @Override
   public Void visitIfThenElse(SimpleCParser.IfThenElseContext ctx) {
     // the conditional expression should be a bool
     // both the if and the else statements should also be type safe
    String cond = exprVisitor.visit(ctx.expr());
     visit(ctx.stmt(0));
     visit(ctx.stmt(1));
 

    if("bool".equals(cond))
      {
        
      }
      
      else
      {
        System.err.println("Error in the if statement");
        typeError(ctx);
        return null;
      }
             return null;

    }

  @Override
  public Void visitReturn(SimpleCParser.ReturnContext ctx) {
    // the return type will always be int, so make sure the return expression has type int
    // recrusively check the expression's type
    String returntype = exprVisitor.visit(ctx.expr());

    // check that the returned type is int
    if ("int".equals(returntype)) {
      // type-safe 
    } 
    else {
      System.err.println("return type should be int");
      typeError(ctx);
    }
   
    return null; 
  }

   @Override
   public Void visitCompound(SimpleCParser.CompoundContext ctx) {
     for (SimpleCParser.StmtContext sctx : ctx.stmt()) visit(sctx);
     return null;
  }
}
