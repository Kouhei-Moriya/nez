package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class WithoutFlag extends Unary {
	String flagName;
	WithoutFlag(SourcePosition s, String flagName, Expression inner) {
		super(s, inner);
		this.flagName = flagName;
		this.optimized = inner.optimized;
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newWithoutFlag(this.s, this.flagName, e) : this;
	}
	@Override
	public String getPredicate() {
		return "without " + this.flagName;
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return this.inner.inferTypestate(visited);
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		this.inner = this.inner.checkTypestate(checker, c);
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		boolean addWithout = false;
		if(undefedFlags != null && !undefedFlags.containsKey(flagName)) {
			undefedFlags.put(flagName, flagName);
			addWithout = true;
		}
		Expression e = inner.removeFlag(undefedFlags);
		if(addWithout) {
			undefedFlags.remove(flagName);
		}
		return e;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}
	@Override
	public boolean predict(int option, int ch, boolean k) {
		return Prediction.predictUnary(this, option, ch, k);
	}
	@Override
	public void predict(int option, boolean[] dfa) {
		Prediction.predictUnary(this, option, dfa);
	}
	@Override
	public boolean match(SourceContext context) {
		return this.inner.optimized.match(context);
	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return this.inner.encode(bc, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return inner.pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.inner.examplfy(gep, sb, p);
	}

}