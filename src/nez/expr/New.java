package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class New extends Unconsumed {
	boolean lefted;
	int shift;
	New(SourcePosition s, boolean lefted, int shift) {
		super(s);
		this.lefted = lefted;
		this.shift  = shift;
	}
	@Override
	public String getPredicate() { 
		return "new";
	}
	@Override
	public String getInterningKey() {
		return lefted ? "{@}" : "{}";
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public Expression removeNodeOperator() {
		return Factory.newEmpty(s);
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.ObjectType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		if(c.required != Typestate.ObjectType) {
			checker.reportWarning(s, "unexpected { .. => removed!");
			return Factory.newEmpty(s);
		}
		c.required = Typestate.OperationType;
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}

	@Override
	public short acceptByte(int ch) {
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		long startIndex = context.getPosition();
//
////		ParsingObject left = context.left;
//		for(int i = 0; i < this.prefetchIndex; i++) {
//			if(!this.get(i).matcher.match(context)) {
//				context.rollback(startIndex);
//				return false;
//			}
//		}
		int mark = context.startConstruction();
		Node newnode = context.newNode();
		context.left = newnode;
		for(int i = 0; i < this.size(); i++) {
			if(!this.get(i).optimized.match(context)) {
				context.abortConstruction(mark);
				context.rollback(startIndex);
				newnode = null;
				return false;
			}
		}
		newnode.setEndingPosition(context.getPosition());
		context.left = newnode;
		return true;
	}
	
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeNew(this, next);
	}

	@Override
	protected int pattern(GEP gep) {
		int max = 0;
		for(Expression p: this) {
			int c = p.pattern(gep);
			if(c > max) {
				max = c;
			}
		}
		return max;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		for(Expression e: this) {
			e.examplfy(gep, sb, p);
		}
	}
}
