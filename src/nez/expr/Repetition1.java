package nez.expr;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class Repetition1 extends Repetition {
	Repetition1(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newRepetition1(this.s, e) : this;
	}
	@Override
	public String getPredicate() { 
		return "+";
	}
	@Override
	public String getInterningKey() { 
		return "+";
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		super.checkAlwaysConsumed(checker, startNonTerminal, stack);
		return this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}
	@Override public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return Accept;
		}
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		long ppos = -1;
		long pos = context.getPosition();
		int c = 0;
		while(ppos < pos) {
			Node left = context.left;
			if(!this.inner.optimized.match(context)) {
				context.left = left;
				left = null;
				break;
			}
			ppos = pos;
			pos = context.getPosition();
			c++;
			left = null;
		}
		if(c == 0) {
			return context.failure2(this);
		}
		return true;
	}
	
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeRepetition1(this, next);
	}

	@Override
	protected int pattern(GEP gep) {
		return 2;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		if(p > 0) {
			int p2 = this.inner.pattern(gep);
			for(int i = 0; i < p2; i++) {
				this.inner.examplfy(gep, sb, p2);
			}
		}
	}
}
