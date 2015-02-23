package nez.vm;

import nez.SourceContext;
import nez.ast.Tag;
import nez.expr.ByteChar;
import nez.expr.ByteMap;
import nez.expr.DefIndent;
import nez.expr.DefSymbol;
import nez.expr.Expression;
import nez.expr.IsIndent;
import nez.expr.IsSymbol;
import nez.expr.Link;
import nez.expr.Replace;
import nez.expr.Rule;
import nez.expr.Sequence;
import nez.expr.Tagging;
import nez.util.StringUtils;

public abstract class Instruction {
	protected Expression  e;
	public Instruction next;
	int id;
	boolean label = false;
	public Instruction(Expression e, Instruction next) {
		this.e = e;
		this.id = -1;
		this.next = next;
	}
	
	Instruction branch() {
		return null;
	}
	Instruction branch2() {
		return null;
	}
	
	protected static Instruction labeling(Instruction inst) {
		if(inst != null) {
			inst.label = true;
		}
		return inst;
	}
	
	protected static String label(Instruction inst) {
		return "L"+inst.id;
	}

	protected abstract void stringfy(StringBuilder sb);
	
	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		stringfy(sb);
		return sb.toString();
	}
	
	public final String getName() {
		return this.getClass().getSimpleName();
	}
	
	boolean debug() {
		return false;
	}
	
	abstract Instruction exec(Context sc) throws TerminationException;
	
	private static boolean isDebug(Instruction inst) {
		return inst instanceof StackOperation;
	}
	
	public static boolean run(Instruction code, SourceContext sc) {
		boolean result = false;
		try {
			while(true) {
//				if(isDebug(code)) {
//					Instruction prev = code;
//					sc.dumpStack("Before " + prev);
//					code = code.exec(sc);
//					sc.dumpStack("After  " + prev);
//				}
//				else {
				//System.out.println("" + code.id + " " + code);
				code = code.exec(sc);
//				}
			}
		}
		catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}
}

class Fail extends Instruction implements StackOperation {
	Fail(Expression e) {
		super(e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opFail();
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("fail");
	}
}

class FailPush extends Instruction implements StackOperation {
	public final Instruction failjump;
	FailPush(Expression e, Instruction failjump, Instruction next) {
		super(e, next);
		this.failjump = labeling(failjump);
	}
	@Override
	Instruction branch() {
		return this.failjump;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opFailPush(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("failpush ");
		sb.append(label(this.failjump));
		sb.append("  ## " + e);
	}
}

class FailPop extends Instruction implements StackOperation {
	public FailPop(Compiler optimizer, Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opFailPop(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("failpop");
	}
}

class FailSkip extends Instruction {
	FailSkip(Compiler optimizer, Expression e) {
		super(e, null);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("skip");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opFailSkip(this);
	}
}

class CallPush extends Instruction implements StackOperation {
	Rule rule;
	public Instruction jump = null;
	CallPush(Compiler optimizer, Rule rule, Instruction next) {
		super(rule, next);
		this.rule = rule;
	}

	void setResolvedJump(Instruction jump) {
		assert(this.jump == null);
		this.jump = labeling(this.next);
		this.next = labeling(jump);
	}
	
	
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opCallPush(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("callpush " + label(jump) + "   ## " + rule.getLocalName());
	}

}

class Return extends Instruction implements StackOperation {
	public Return(Compiler optimizer, Rule e) {
		super(e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opReturn();
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("ret  ## " + ((Rule)e).getLocalName());
	}

}

class PosPush extends Instruction {
	PosPush(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opPosPush(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("pospush");
	}
}

class PosBack extends Instruction {
	public PosBack(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opPopBack(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("back");
	}

}


class Exit extends Instruction {
	boolean status;
	public Exit(boolean status) {
		super(null, null);
		this.status = status;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("exit");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		throw new TerminationException(status);
	}

}



class MatchAny extends Instruction {
	MatchAny(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMatchAny(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("matchany");
	}
}

class IByteChar extends Instruction {
	public final boolean optional;
	public final int byteChar;
	IByteChar(ByteChar e, boolean optional, Instruction next) {
		super(e, next);
		this.byteChar = e.byteChar;
		this.optional = optional;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIByteChar(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("bytematch ");
		sb.append(StringUtils.stringfyByte(byteChar));
	}
}

class IByteMap extends Instruction {
	public final boolean optional;
	public final boolean[] byteMap;
	IByteMap(ByteMap e, boolean optional, Instruction next) {
		super(e, next);
		this.byteMap = e.charMap;
		this.optional = optional;
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIByteMap(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("mapmatch ");
		sb.append(StringUtils.stringfyByteMap(byteMap));
	}
}

interface Construction {
	
}

class NodePush extends Instruction {
	NodePush(Link e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodePush(this);
	}
	@Override
	boolean debug() {
		return true;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("nodepush");
	}
}

class NodeStore extends Instruction {
	public final int index;
	NodeStore(Link e, Instruction next) {
		super(e, next);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodeStore(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("nodestore " + index);
	}
}


class NodeNew extends Instruction {
	int shift;
	NodeNew(Expression e, Instruction next) {
		super(e, next);
		this.shift = 0;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNew(this);
	}
	@Override
	boolean debug() {
		return true;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("new");
	}
}

class NodeLeftNew extends Instruction {
	int shift;
	NodeLeftNew(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodeLeftLink(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("leftnew");
	}
}


class NodeCapture extends Instruction {
	int shift;
	NodeCapture(Expression e, Instruction next) {
		super(e, next);
		this.shift = 0;
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opCapture(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("capture");
	}
	
}

class NodeReplace extends Instruction {
	public final String value;
	NodeReplace(Replace e, Instruction next) {
		super(e, next);
		this.value = e.value;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opReplace(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("replace " + StringUtils.quoteString('"', value, '"'));
	}
}

class NodeTag extends Instruction {
	public final Tag tag;
	NodeTag(Tagging e, Instruction next) {
		super(e, next);
		this.tag = e.tag;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodeTag(this);
	}	
	@Override
	boolean debug() {
		return true;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("tag " + StringUtils.quoteString('"', tag.name, '"'));
	}
}


interface Memoization {

}

class Lookup extends FailPush implements Memoization {
	final MemoPoint memoPoint;
	final Instruction skip;
	Lookup(Expression e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, failjump, next);
		this.memoPoint = m;
		this.skip = labeling(skip);
	}
	@Override
	Instruction branch2() {
		return this.skip;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookup(this);
	}
}

class Lookup2 extends Lookup {
	Lookup2(Expression e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, m, next, skip, failjump);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookup2(this);
	}
}

class Memoize extends Instruction implements Memoization {
	final MemoPoint memoPoint;
	Memoize(Expression e, MemoPoint m, Instruction next) {
		super(e, next);
		this.memoPoint = m;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoize(this);
	}
}

class Memoize2 extends Memoize {
	Memoize2(Expression e, MemoPoint m, Instruction next) {
		super(e, m, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoize2(this);
	}
}

class MemoizeFail extends Fail implements Memoization {
	MemoPoint memoPoint;
	MemoizeFail(Expression e, MemoPoint m) {
		super(e);
		this.memoPoint = m;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeFail(this);
	}
}

class MemoizeFail2 extends MemoizeFail {
	MemoizeFail2(Expression e, MemoPoint m) {
		super(e, m);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeFail2(this);
	}
}

class LookupNode extends Lookup {
	final int index;
	LookupNode(Link e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, m, next, skip, failjump);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookupNode(this);
	}
}

class LookupNode2 extends LookupNode {
	final int index;
	LookupNode2(Link e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, m, next, skip, failjump);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookupNode2(this);
	}
}

class MemoizeNode extends NodeStore implements Memoization {
	final MemoPoint memoPoint;
	MemoizeNode(Link e, MemoPoint m, Instruction next) {
		super(e, next);
		this.memoPoint = m;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeNode(this);
	}
}

class MemoizeNode2 extends MemoizeNode {
	MemoizeNode2(Link e, MemoPoint m, Instruction next) {
		super(e, m, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeNode2(this);
	}
}

/* Symbol */

class IDefSymbol extends Instruction {
	Tag tableName;
	IDefSymbol(DefSymbol e, Instruction next) {
		super(e, next);
		this.tableName = e.table;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("def ");
		sb.append(tableName.name);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIDefSymbol(this);
	}
}

class IIsSymbol extends Instruction {
	Tag tableName;
	boolean onlyTop;
	IIsSymbol(IsSymbol e, boolean onlyTop, Instruction next) {
		super(e, next);
		this.tableName = e.table;
		this.onlyTop = onlyTop;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("is ");
		sb.append(tableName.name);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIIsSymbol(this);
	}
}

class IDefIndent extends Instruction {
	IDefIndent(DefIndent e, Instruction next) {
		super(e, next);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("defindent");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIDefIndent(this);
	}
}

class IIsIndent extends Instruction {
	IIsIndent(IsIndent e, Instruction next) {
		super(e, next);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("indent");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIIsIndent(this);
	}
}

class ITablePush extends Instruction {
	ITablePush(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("tablepush");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opITablePush(this);
	}
}

class ITablePop extends FailPop {
	public ITablePop(Compiler optimizer, Expression e, Instruction next) {
		super(optimizer, e, next);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("tablepop");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opITablePop(this);
	}
}


/* Specialization */

class NByteMap extends Instruction {
	public final boolean[] byteMap;
	NByteMap(ByteMap e, Instruction next) {
		super(e, next);
		this.byteMap = e.charMap;
	}
	NByteMap(ByteChar e, Instruction next) {
		super(e, next);
		this.byteMap = ByteMap.newMap();
		this.byteMap[e.byteChar] = true;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNByteMap(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("not ");
		sb.append(StringUtils.stringfyByteMap(byteMap));
	}
}

class RByteMap extends Instruction {
	public final boolean[] byteMap;
	RByteMap(ByteMap e, Instruction next) {
		super(e, next);
		this.byteMap = e.charMap;
	}
	RByteMap(ByteChar e, Instruction next) {
		super(e, next);
		this.byteMap = ByteMap.newMap();
		this.byteMap[e.byteChar] = true;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opRByteMap(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("repeat ");
		sb.append(StringUtils.stringfyByteMap(byteMap));
	}
}


class IMultiChar extends Instruction {
	boolean optional = false;
	final byte[] utf8;
	final int    len;
	public IMultiChar(Sequence e, boolean optional, Instruction next) {
		super(e, next);
		this.utf8 = e.extractMultiChar(0, e.size());
		this.len = this.utf8.length;
		this.optional = optional;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMultiChar(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName());
		for(int i = 0; i < utf8.length; i++) {
			sb.append(" ");
			sb.append(StringUtils.stringfyByte(utf8[i] & 0xff));
		}
	}
}

class NMultiChar extends IMultiChar {
	NMultiChar(Sequence e, Instruction next) {
		super(e, false, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNMultiChar(this);
	}
}
