SOURCE := \
	SimpleCParser.java \
	Compiler.java \
  TypeChecker.java \
	 TAC.java \
   TACFunction.java \
	 CodeGen.java \
	 ASMGen.java \
	 export CLASSPATH=/usr/share/java/antlr4-runtime.jar:$CLASSPATH
CLASSES := $(SOURCE:%.java=%.class)

.PHONY: all ast clean clobber

all: $(CLASSES)

SimpleCParser.java: SimpleC.g4
	antlr4 -visitor $<

%.class: %.java
	javac -cp $(CLASSPATH) $<

clean:
	rm -f $(CLASSES)
	rm -f 'SimpleC.interp'
	rm -f 'SimpleC.tokens'
	rm -f 'SimpleCBaseListener.java'
	rm -f 'SimpleCBaseVisitor.java'
	rm -f 'SimpleCLexer.interp'
	rm -f 'SimpleCLexer.class'
	rm -f 'SimpleCLexer.java'
	rm -f 'SimpleCLexer.tokens'
	rm -f 'SimpleCListener.class'
	rm -f 'SimpleCListener.java'
	rm -f 'SimpleCParser$$ActualParamsContext.class'
	rm -f 'SimpleCParser$$AddSubContext.class'
	rm -f 'SimpleCParser$$AndOrContext.class'
	rm -f 'SimpleCParser$$AssignmentContext.class'
	rm -f 'SimpleCParser$$CallContext.class'
	rm -f 'SimpleCParser$$CompoundContext.class'
	rm -f 'SimpleCParser$$DeclContext.class'
	rm -f 'SimpleCParser$$EqNeqContext.class'
	rm -f 'SimpleCParser$$ExprContext.class'
	rm -f 'SimpleCParser$$FormalParamsContext.class'
	rm -f 'SimpleCParser$$FunctionContext.class'
	rm -f 'SimpleCParser$$IfThenElseContext.class'
	rm -f 'SimpleCParser$$InputContext.class'
	rm -f 'SimpleCParser$$MultDivContext.class'
	rm -f 'SimpleCParser$$NegateContext.class'
	rm -f 'SimpleCParser$$NotContext.class'
	rm -f 'SimpleCParser$$NumContext.class'
	rm -f 'SimpleCParser$$OutputContext.class'
	rm -f 'SimpleCParser$$ParensContext.class'
	rm -f 'SimpleCParser$$ProgramContext.class'
	rm -f 'SimpleCParser$$RelationalContext.class'
	rm -f 'SimpleCParser$$ReturnContext.class'
	rm -f 'SimpleCParser$$SkipContext.class'
	rm -f 'SimpleCParser$$StmtContext.class'
	rm -f 'SimpleCParser$$TypeContext.class'
	rm -f 'SimpleCParser$$VarContext.class'
	rm -f 'SimpleCParser$$WhileContext.class'
	rm -f 'SimpleCParser.class'
	rm -f 'SimpleCParser.java'
	rm -f 'SimpleCVisitor.class'
	rm -f 'SimpleCVisitor.java'
