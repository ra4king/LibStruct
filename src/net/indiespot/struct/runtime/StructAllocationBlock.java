package net.indiespot.struct.runtime;

import net.indiespot.struct.transform.StructEnv;

public class StructAllocationBlock {
	final long base;
	final int sizeof;
	protected long next;

	StructAllocationBlock(long base, int sizeof) {
		this.base = base;
		this.sizeof = sizeof;
		this.next = base;
	}

	public void reset() {
		this.next = base;
	}

	public long allocate(int sizeof, int alignment) {
		if (StructEnv.SAFETY_FIRST)
			if (sizeof <= 0)
				throw new IllegalArgumentException();

		next = StructMemory.alignAddress(next, alignment);

		if (StructEnv.SAFETY_FIRST)
			if (!this.canAllocate(sizeof))
				throw new StructAllocationBlockOverflowError();

		long addr = next;
		next += sizeof;
		return addr;
	}

	public int getFreeSpace() {
		return (int) (base + sizeof - next);
	}

	public boolean canAllocate(int sizeof) {
		return (sizeof > 0) && (next + sizeof) <= (base + this.sizeof);
	}

	public boolean isOnBlock(long handle) {
		return (handle >= base && handle < base + sizeof);
	}
}
