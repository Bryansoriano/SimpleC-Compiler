import java.util.List;
import java.util.LinkedList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

public class CodeGen extends SimpleCBaseVisitor<Void> {
  public List<TACFunction> functionlist = new LinkedList<TACFunction>();
  TACFunction currentfunc;
  ExprVisitor exprVisitor = new ExprVisitor();

  protected class ExprVisitor extends SimpleCBaseVisitor<String> {
    // TODO: fill out the rest of the visitors

    @Override
    public String visitCall(SimpleCParser.CallContext ctx) {
    //Make Variables
    int args;
    String temp;
    String f_name = ctx.ID().getText();
    String dest = currentfunc.freshTemp();
    //Test to see if function is declared. I know this is in the type checker but just to make sure
       if((ctx.actualParams() == null))
    {
        args = 0;
     }
     else{
         args = ctx.actualParams().expr().size();
     }
     //Checking for each param
      for(int i = 0; i < args; i++)
      {
        //Recursively generate for each parameter
        temp = visit(ctx.actualParams().expr(i));
        //Emit the param
        currentfunc.add(new TAC(TAC.Op.PARAM, temp));
      }
      //Emit the call
      currentfunc.add(new TAC(TAC.Op.CALL,dest ,f_name));
      //Given
      return ctx.ID().getText();
    }
    @Override
    public String visitNegate(SimpleCParser.NegateContext ctx){
        //Recursively generate code for negate while also storing the variable
        String temp = exprVisitor.visit(ctx.expr());
        //Making temp variable
        String result = currentfunc.freshTemp();
        //Emit const 0
        currentfunc.add(new TAC(TAC.Op.CONST,result,"0"));
        //Emit subtraction
        currentfunc.add(new TAC(TAC.Op.SUB,result, temp));
        //Return the negate destination
        return result;
    }
     @Override
    public String visitNot(SimpleCParser.NotContext ctx) {
    //Recursively generate code for NOT
      exprVisitor.visit(ctx.expr());
      //Making my temp variable and end result
      String t = currentfunc.freshTemp();
      String result = currentfunc.freshTemp();
      //Making false and end labels
      String f_l = currentfunc.freshLabel();
      String end_l = currentfunc.freshLabel();
      //Emit the equivalent logic using goto instructions
      currentfunc.add(new TAC(TAC.Op.CONST,t,"1"));
      currentfunc.add(new TAC(TAC.Op.GOTOZE, f_l, t));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"0"));
      currentfunc.add(new TAC(TAC.Op.GOTO, end_l));
      currentfunc.add(new TAC(TAC.Op.LABEL, f_l));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"1"));
      currentfunc.add(new TAC(TAC.Op.LABEL, end_l));

      return result;
    }
      @Override
    public String visitAddSub(SimpleCParser.AddSubContext ctx) {
    //Recursively generate the code for the left and right operand while storing destinations in variables
     String lhs = visit(ctx.expr(0));
     String rhs = visit(ctx.expr(1));
     // make destination variable
     String dest = currentfunc.freshTemp();
     //Check if operand is Plus then emit equivalent arithmetic opcode
     if(ctx.op.getType() == SimpleCParser.PLUS){
      currentfunc.add(new TAC(TAC.Op.ADD,dest,lhs,rhs));
     }
     //Check if operand is Minus then emit equivalent arithmetic opcode
    else if(ctx.op.getType() == SimpleCParser.MINUS){
          currentfunc.add(new TAC(TAC.Op.SUB,dest,lhs,rhs));
    }           
    //Return destination
      return dest;
    }
    @Override
     public String visitMultiDiv(SimpleCParser.MultiDivContext ctx) {
     //Recursively generate the code for the left and right operand while storing destinations in variables
     String lhs = visit(ctx.expr(0));
     String rhs = visit(ctx.expr(1));
     // make destination variable
     String dest = currentfunc.freshTemp();
     //Check if operand is Mult then emit equivalent arithmetic opcode
     if(ctx.op.getType() == SimpleCParser.MULT){
      currentfunc.add(new TAC(TAC.Op.MULT,dest,lhs,rhs));
     }      
     //Check if operand is Div then emit equivalent arithmetic opcode
    else if(ctx.op.getType() == SimpleCParser.DIV){
          currentfunc.add(new TAC(TAC.Op.DIV,dest,lhs,rhs));
    }           
    //Return dest
      return dest;
    }
    @Override
     public String visitRelational(SimpleCParser.RelationalContext ctx) {
     //Recursively generate the code for the left and right operand while storing destinations in variables
     String lhs = visit(ctx.expr(0));
     String rhs = visit(ctx.expr(1));

     //Make false and end labels along with the result of the relational operands
     String f_l = currentfunc.freshLabel();
     String end_l = currentfunc.freshLabel();
     String result = currentfunc.freshTemp();

     //Emit the equivalent logic using goto instructions
     if(ctx.op.getType() == SimpleCParser.LT){
      currentfunc.add(new TAC(TAC.Op.LABEL,"LT TEST"));

      currentfunc.add(new TAC(TAC.Op.GOTOGE,f_l,lhs,rhs));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"1"));
      currentfunc.add(new TAC(TAC.Op.GOTO,end_l));
      currentfunc.add(new TAC(TAC.Op.LABEL,f_l));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"0"));
      currentfunc.add(new TAC(TAC.Op.LABEL,end_l));
      
     }      
     
    else if(ctx.op.getType() == SimpleCParser.GT){
      currentfunc.add(new TAC(TAC.Op.GOTOLE,f_l,lhs,rhs));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"1"));
      currentfunc.add(new TAC(TAC.Op.GOTO,end_l));
      currentfunc.add(new TAC(TAC.Op.LABEL,f_l));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"0"));
      currentfunc.add(new TAC(TAC.Op.LABEL,end_l));
    }    
    
     else if(ctx.op.getType() == SimpleCParser.LE){
      currentfunc.add(new TAC(TAC.Op.GOTOGT,f_l,lhs,rhs));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"1"));
      currentfunc.add(new TAC(TAC.Op.GOTO,end_l));
      currentfunc.add(new TAC(TAC.Op.LABEL,f_l));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"0"));
      currentfunc.add(new TAC(TAC.Op.LABEL,end_l));
    }  
     else if(ctx.op.getType() == SimpleCParser.GE){
      currentfunc.add(new TAC(TAC.Op.GOTOLT,f_l,lhs,rhs));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"1"));
      currentfunc.add(new TAC(TAC.Op.GOTO,end_l));
      currentfunc.add(new TAC(TAC.Op.LABEL,f_l));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"0"));
      currentfunc.add(new TAC(TAC.Op.LABEL,end_l));
    }  
    
      return result;
    }

     @Override
     public String visitEqNeq(SimpleCParser.EqNeqContext ctx) {
     //Recursively generate the code for the left and right operand while storing destinations in variables
     String result = currentfunc.freshTemp();
     String lhs = visit(ctx.expr(0));
     String rhs = visit(ctx.expr(1));
     //Make false and end labels along with the result of the relational operands
     String f_l = currentfunc.freshLabel();
     String end_l = currentfunc.freshLabel();

     //String operation = ctx.getText();

     //emit equivalent logic with goto instructions
     if(ctx.op.getType() == SimpleCParser.EQ){
      currentfunc.add(new TAC(TAC.Op.GOTONE,f_l,lhs,rhs));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"1"));
      currentfunc.add(new TAC(TAC.Op.GOTO, end_l));
      currentfunc.add(new TAC(TAC.Op.LABEL, f_l));
      currentfunc.add(new TAC(TAC.Op.CONST, result, "0"));
      currentfunc.add(new TAC(TAC.Op.LABEL, end_l));
     }      
     //emit equivalent logic with goto instructions
    else if(ctx.op.getType() == SimpleCParser.NEQ){
      currentfunc.add(new TAC(TAC.Op.GOTONE,f_l,lhs,rhs));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"1"));
      currentfunc.add(new TAC(TAC.Op.GOTO, end_l));
      currentfunc.add(new TAC(TAC.Op.LABEL, f_l));
      currentfunc.add(new TAC(TAC.Op.CONST, result, "0"));
      currentfunc.add(new TAC(TAC.Op.LABEL, end_l));
    }   

    
    return result;
    }
    @Override
    public String visitAndOr(SimpleCParser.AndOrContext ctx) {
     //Recursively generate the code for the left and right operand while storing destinations in variables
     String lhs = visit(ctx.expr(0));
     String rhs = visit(ctx.expr(1));
     //Make temp labels and variables
     String result = currentfunc.freshTemp();
     String end_l = currentfunc.freshLabel();
     //Emit equivalent op code
     if(ctx.op.getType() == SimpleCParser.AND){
       String f_l = currentfunc.freshLabel();

      currentfunc.add(new TAC(TAC.Op.GOTOZE,f_l,lhs));
      currentfunc.add(new TAC(TAC.Op.GOTOZE,f_l,rhs));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"1"));
      currentfunc.add(new TAC(TAC.Op.GOTO, end_l));
      currentfunc.add(new TAC(TAC.Op.LABEL, f_l));
      currentfunc.add(new TAC(TAC.Op.CONST, result, "0"));
      currentfunc.add(new TAC(TAC.Op.LABEL, end_l));
     }      
     //also emit equivalent op code
    else if(ctx.op.getType() == SimpleCParser.OR){
       String t_l = currentfunc.freshLabel();

      currentfunc.add(new TAC(TAC.Op.GOTONZ,t_l,lhs));
      currentfunc.add(new TAC(TAC.Op.GOTONZ,t_l,rhs));
      currentfunc.add(new TAC(TAC.Op.CONST,result,"0"));
      currentfunc.add(new TAC(TAC.Op.GOTO, end_l));
      currentfunc.add(new TAC(TAC.Op.LABEL, t_l));
      currentfunc.add(new TAC(TAC.Op.CONST, result, "1"));
      currentfunc.add(new TAC(TAC.Op.LABEL, end_l));
    }   


     return result;
    }
    //visit variable and return text
    @Override
    public String visitVar(SimpleCParser.VarContext ctx) {
      return ctx.ID().getText();
    }
    //Say hi to num and remember that this was a given
    @Override
    public String visitNum(SimpleCParser.NumContext ctx) {
      String dest = currentfunc.freshTemp();
      currentfunc.add(new TAC(TAC.Op.CONST, dest, ctx.NUM().getText()));
      return dest;
    }

    @Override
    public String visitParens(SimpleCParser.ParensContext ctx) {
      return visit(ctx.expr());
      }
    }

  @Override
  public Void visitProgram(SimpleCParser.ProgramContext ctx) {
    visitChildren(ctx);
    return null;
  }
  
  @Override
  public Void visitFunction(SimpleCParser.FunctionContext ctx) {
    List<String> formals = new LinkedList<String>();
    List<String> locals = new LinkedList<String>();
    if (null != ctx.formalParams()) {
      for (TerminalNode token : ctx.formalParams().ID()) {
        formals.add(token.getText());
      }
    }
    locals.add("true");
    locals.add("false");
    for (SimpleCParser.DeclContext dctx : ctx.decl()) {
      locals.add(dctx.ID().getText());
    }
    
    currentfunc = new TACFunction(ctx.ID().getText(), formals, locals);
    String dest;
    dest = currentfunc.freshTemp();
    currentfunc.add(new TAC(TAC.Op.CONST, dest, "1"));
    currentfunc.add(new TAC(TAC.Op.ASSIGN, "true", dest));
    dest = currentfunc.freshTemp();
    currentfunc.add(new TAC(TAC.Op.CONST, dest, "0"));
    currentfunc.add(new TAC(TAC.Op.ASSIGN, "false", dest));
    for (SimpleCParser.StmtContext sctx : ctx.stmt()) visit(sctx);
    functionlist.add(currentfunc);
    System.err.println("["+currentfunc.toString() + "]");
    
    return null;
  }
  
  @Override
  public Void visitReturn(SimpleCParser.ReturnContext ctx) {
    String temp = exprVisitor.visit(ctx.expr());
    currentfunc.add(new TAC(TAC.Op.RETURN, temp));
    return null;
  }
  @Override
  public Void visitSkip(SimpleCParser.SkipContext ctx) {
  //Emit the NOPE.... i mean NOP
    currentfunc.add(new TAC(TAC.Op.NOP));
    return null;
  }

  @Override
  public Void visitCompound(SimpleCParser.CompoundContext ctx) {
    for (SimpleCParser.StmtContext sctx : ctx.stmt()) visit(sctx);

    return null;
  }
  //Emit Input
  @Override
  public Void visitInput(SimpleCParser.InputContext ctx)
  {
    String in = ctx.ID().getText();
    currentfunc.add(new TAC(TAC.Op.INPUT, in));
    return null;
  }
  //Recursively generate code for the expression
  @Override
  public Void visitOutput(SimpleCParser.OutputContext ctx)
  {
    String temp = exprVisitor.visit(ctx.expr());
    currentfunc.add(new TAC(TAC.Op.OUTPUT, temp));
    return null;
  }
  @Override
  public Void visitAssignment(SimpleCParser.AssignmentContext ctx) {
  //Recursively generate code for the expression
    String reg = exprVisitor.visit(ctx.expr());
    String var = ctx.ID().getText();
    //Emit an assign
    currentfunc.add(new TAC(TAC.Op.ASSIGN,var,reg));

    return null;
  }
  
  @Override
  public Void visitWhile(SimpleCParser.WhileContext ctx) {
    //Create head and false label
    String head_l = currentfunc.freshLabel();
    String false_l = currentfunc.freshLabel();
    //Emit said head label
    currentfunc.add(new TAC(TAC.Op.LABEL, head_l));
    //Recursively generate code for the conditional and say the temp variable
    String temp = exprVisitor.visit(ctx.expr());
    //Emit branch for the false condition
    currentfunc.add(new TAC(TAC.Op.GOTOZE, false_l,temp));
    //Visit the body statement
    visit(ctx.stmt());
    //Emit head label branch
    currentfunc.add(new TAC(TAC.Op.GOTO, head_l));
    //Emit the end label which happens to also be our false label
    currentfunc.add(new TAC(TAC.Op.LABEL, false_l));

    return null;
  }
   @Override
  public Void visitIfThenElse(SimpleCParser.IfThenElseContext ctx) {
    //Recursively generate code for the condition.. and save it while we are here 
    String cond = exprVisitor.visit(ctx.expr());
    //Make out else and end labels
    String else_l = currentfunc.freshLabel();
    String end_l = currentfunc.freshLabel();
    //Emit the else branch
    currentfunc.add(new TAC(TAC.Op.GOTOZE, else_l, cond));
    //Visit the body
    visit(ctx.stmt(0));
    //Emit the unconditional to end label
    currentfunc.add(new TAC(TAC.Op.GOTO, end_l));
    //Emit the else label and visit the else body
    currentfunc.add(new TAC(TAC.Op.LABEL,else_l));
    visit(ctx.stmt(1));    
    //Emit the end label
    currentfunc.add(new TAC(TAC.Op.LABEL,end_l));




    return null;
  }
  // TODO: fill out the rest of the visitors
}
