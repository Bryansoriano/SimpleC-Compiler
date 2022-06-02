import java.io.PrintWriter;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

class ASMGen {
  PrintWriter outfile;
  
  public ASMGen(PrintWriter outfile) {
    this.outfile = outfile;
  }
  
  public void gen(List<TACFunction> functionlist) {
    for (TACFunction function : functionlist) {
      outfile.print(String.format("" +
".text\n" +
".globl %s\n" +
".type %s, @function\n" +
"%s:\n" +
                                       "", function.name, function.name, function.name));

      // improvements and optimization opportunities
      // - support types with different bitwidths
      // - machine code optimization to avoid storing immediates in registers first (peephole on machine code or augment intermediate code)
      // - don't allocate space for symbols that are no longer used due to optimization
      
      // create a symbol table to hold the offsets
      int bytewidth = 8;  // assume everything is a 64-bit integer
      List<String> symbols = new LinkedList<String>();
      Map<String, Integer> offsets = new HashMap<String, Integer>();
      int localspace = 0;
      symbols.addAll(function.formals);
      symbols.addAll(function.locals);
      symbols.addAll(function.temps);
      for (String symbol : symbols) {
        localspace = localspace + bytewidth;
        // System.err.println("put " + symbol + " " + localspace);
        offsets.put(symbol, localspace);
      }

      // ABI: align stack on 16-byte boundary (you might get segfaults calling other libraries without this!)
      localspace = ((localspace / 16) + 1) * 16;
      
      // ABI: prologue
      push("%rbp");
      mov("%rsp", "%rbp");
      sub(String.format("$%d", localspace), "%rsp");

      // move actual parameters into their stack variables
      // start with the six register-allocated args
      String param_regs[] = {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"};
      for (int i = 0; i < Math.min(function.formals.size(), param_regs.length); i++) {
        String symbol = function.formals.get(i);
        assert offsets.containsKey(symbol);
        mov(param_regs[i], negoffset("%rbp", offsets.get(symbol)));
      }
      // then do the stack-allocated args
      {
        int param_offset = 16;  // start at first parameter before return address
        for (int i = function.formals.size() - 1; i >= param_regs.length ; i--) {
          String symbol = function.formals.get(i);
          assert offsets.containsKey(symbol);
          mov(posoffset("%rbp", param_offset), "%rax");
          mov("%rax", localvar(offsets.get(symbol)));
          param_offset += bytewidth;
        }
      }

      String returnlabel = String.format("_%s_return", function.name);  

      // improvement register allocation
      
      // emit statements
      int param_count = 0;  // track param number for intel abi
      for (TAC tac : function.body) {
        switch (tac.op) {
        case RETURN:
          // ABI: save return value into %rax
          assert(offsets.containsKey(tac.arg1));
          // mov the contents of the address of the tac.arg1 variable to the rax register, per the ABI
          // localvar() computes the register indirection given an offset, offsets.get() gets the offset from rbp for a given variable name
          mov(localvar(offsets.get(tac.arg1)), "%rax");
          // jmp to the returnlabel, which has already been created and will be emitted before the function epilogue
          jmp(returnlabel);
          break;
        case CONST:
          assert(offsets.containsKey(tac.arg1));
          mov_from_imm("1", localvar(offsets.get(tac.arg1)));
          // mov_from_imm
          break;
        case ASSIGN:
          assert(offsets.containsKey(tac.arg1));
          assert(offsets.containsKey(tac.arg2));
          // mov arg2's localvar offset into a register, e.g., rax
          mov(localvar(offsets.get(tac.arg2)),"%rax");
          // then mov that register's value into arg1
          mov("%rax", localvar(offsets.get(tac.arg1)));

          break;
        case INPUT:
          call("input_int64_t@PLT");
          assert(offsets.containsKey(tac.arg1));
          mov("%rax", localvar(offsets.get(tac.arg1)));
          break;
        case OUTPUT:
          assert(offsets.containsKey(tac.arg1));
          mov(localvar(offsets.get(tac.arg1)), param_regs[0]);  // move to first param
          call("output_int64_t@PLT");
          break;
        case ADD:
          assert(offsets.containsKey(tac.arg1));
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          // move the operands' addresses into two registers, e.g., rax and rcx
          mov(localvar(offsets.get(tac.arg2)),"%rax");
          mov(localvar(offsets.get(tac.arg1)),"%rcx");

          // emit the add instruction
          add("%rcx", "%rax");
          // mov the result from the register into the destination address
          mov("%rax", localvar(offsets.get(tac.arg3)));
          break;
        case SUB:
          assert(offsets.containsKey(tac.arg1));
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          // same as add only use sub
          mov(localvar(offsets.get(tac.arg2)),"%rax");
          mov(localvar(offsets.get(tac.arg1)),"%rcx");
          sub("%rcx", "%rax");
          mov("%rax", localvar(offsets.get(tac.arg3)));
          break;
        case MULT:
           assert(offsets.containsKey(tac.arg1));
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          // same as add only use sub
          mov(localvar(offsets.get(tac.arg2)),"%rax");
          mov(localvar(offsets.get(tac.arg1)),"%rcx");
          mul("%rcx", "%rax");
          mov("%rax", localvar(offsets.get(tac.arg3)));
          break;
        case DIV:
          assert(offsets.containsKey(tac.arg1));
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          mov(localvar(offsets.get(tac.arg2)), "%rax");
          mov(localvar(offsets.get(tac.arg3)), "%rcx");
          cdq();
          div("%rcx");
          mov("%rax", localvar(offsets.get(tac.arg1)));
          break;
        case PARAM:
          if (param_count < 6) {
            // ABI: first 6 params to registers
            assert(offsets.containsKey(tac.arg1));
            mov(localvar(offsets.get(tac.arg1)), param_regs[param_count]);
          } else {
            // ABI: rest of params to stack
            assert(offsets.containsKey(tac.arg1));
            push(localvar(offsets.get(tac.arg1)));
          }
          param_count++;
          break;
        case CALL:
          call(tac.arg2);
          // ABI: return value in %rax
          assert(offsets.containsKey(tac.arg1));
          // ABI: pop stack-passed params
          for (int i = 0; i < param_count - param_regs.length; i++) {
            pop("%rcx");
          }
          mov("%rax", localvar(offsets.get(tac.arg1)));
          param_count = 0;
          break;
        case NOP:
          nop();
          break;
        case LABEL:
          //label(String.format("_%s%s", function.name, tac.arg1));
          label(tac.arg1);
          break;
        case GOTO:
          // emit a jmp
          jmp(tac.arg1);
          break;
        case GOTOZE:
          assert(offsets.containsKey(tac.arg2));
          // mov the operand into a register, e.g., rax
          // compare that register using cmp, e.g., compare 0 to rax
          // use the appropriate jXX instruction, e.g., j("z", ...) to branch to the given label, tac.arg1
          mov(localvar(offsets.get(tac.arg2)), "%rax");
          cmp("$0", "%rax");
          j("z", tac.arg1);
          break;
        case GOTONZ:
          assert(offsets.containsKey(tac.arg2));
          // same as gotoze except use a different jXX instruction
          mov(localvar(offsets.get(tac.arg2)), "%rax");
          cmp("$0", "%rax");
          j("", tac.arg1);
          break;
        case GOTOEQ:
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          // same as gotoze, except mov both operands into two different registers, e.g., rax and rcx, then use cmp to compare those registers
          // be sure to get the right j("...", tac.arg1) with the corresponding jXX kind, e.g., "e", "ne", "l", etc.
          mov(localvar(offsets.get(tac.arg2)), "%rax");
          mov(localvar(offsets.get(tac.arg3)), "%rcx");
          cmp("%rcx", "%rax");
          j("e", tac.arg1);
          break;
        case GOTONE:
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          // same as gotoeq, except use a different jXX instruction
          mov(localvar(offsets.get(tac.arg2)), "%rax");
          mov(localvar(offsets.get(tac.arg3)), "%rcx");
          cmp("%rcx", "%rax");
          j("ne", tac.arg1);
          break;
        case GOTOLT:
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          // same as gotoeq, except use a different jXX instruction
          mov(localvar(offsets.get(tac.arg2)), "%rax");
          mov(localvar(offsets.get(tac.arg3)), "%rcx");
          cmp("%rcx", "%rax");
          j("lt", tac.arg1);
          break;
        case GOTOGT:
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          // same as gotoeq, except use a different jXX instruction
          mov(localvar(offsets.get(tac.arg2)), "%rax");
          mov(localvar(offsets.get(tac.arg3)), "%rcx");
          cmp("%rcx", "%rax");
          j("gt", tac.arg1);
          break;
        case GOTOGE:
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          // same as gotoeq, except use a different jXX instruction
          mov(localvar(offsets.get(tac.arg2)), "%rax");
          mov(localvar(offsets.get(tac.arg3)), "%rcx");
          cmp("%rcx", "%rax");
          j("ge", tac.arg1);
          break;
        case GOTOLE:
          assert(offsets.containsKey(tac.arg2));
          assert(offsets.containsKey(tac.arg3));
          // same as gotoeq, except use a different jXX instruction
          mov(localvar(offsets.get(tac.arg2)), "%rax");
          mov(localvar(offsets.get(tac.arg3)), "%rcx");
          cmp("%rcx", "%rax");
          j("le", tac.arg1);
          break;
        default:
          System.err.println("TODO: make this an assertion");
          break;
        }
      }

      // emit return label
      label(returnlabel);
      
      // ABI: epilogue
      mov("%rbp", "%rsp");
      pop("%rbp");
      ret();
    }
  }

  protected String localvar(int offset) {
    return negoffset("%rbp", offset);
  }

  protected String negoffset(String reg, int offset) {
    return String.format("-%d(%s)", offset, reg);
  }

  protected String posoffset(String reg, int offset) {
    return String.format("%d(%s)", offset, reg);
  }

  protected void push(String arg1) {
    outfile.print(String.format("\tpush\t%s\n", arg1));
  }

  protected void pop(String arg1) {
    outfile.print(String.format("\tpop\t%s\n", arg1));
  }

  protected void mov(String arg1, String arg2) {
    outfile.print(String.format("\tmov\t%s, %s\n", arg1, arg2));
  }

  protected void mov_from_imm(String arg1, String arg2) {
    outfile.print(String.format("\tmovq\t$%s, %s\n", arg1, arg2));
  }

  protected void add(String arg1, String arg2) {
    outfile.print(String.format("\tadd\t%s, %s\n", arg1, arg2));
  }
  
  protected void sub(String arg1, String arg2) {
    outfile.print(String.format("\tsub\t%s, %s\n", arg1, arg2));
  }
  
  protected void mul(String arg1, String arg2) {
    outfile.print(String.format("\timul\t%s, %s\n", arg1, arg2));
  }

  protected void cdq() {
    outfile.print(String.format("\tcdq\n"));
  }
  
  protected void div(String arg1) {
    outfile.print(String.format("\tidiv\t%s\n", arg1));
  }
  
  protected void call(String arg1) {
    outfile.print(String.format("\tcall\t%s\n", arg1));
  }
  
  protected void ret() {
    outfile.print(String.format("\tret\n"));
  }

  protected void label(String labelname) {
    outfile.print(String.format("%s:\n", labelname));
  }

  protected void jmp(String labelname) {
    outfile.print(String.format("\tjmp\t%s\n", labelname));
  }

  protected void nop() {
    outfile.print(String.format("\tnop\n"));
  }

  protected void cmp(String arg1, String arg2) {
    outfile.print(String.format("\tcmp\t%s, %s\n", arg1, arg2));
  }

  protected void j(String suffix, String labelname) {
    outfile.print(String.format("\tj%s\t%s\n", suffix, labelname));
  }
}
