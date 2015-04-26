package test.net.indiespot.demo.softlylit.structs.support;

import test.net.indiespot.demo.softlylit.structs.Triangle;

import net.indiespot.struct.cp.Struct;
import net.indiespot.struct.cp.TakeStruct;
import net.indiespot.struct.transform.StructEnv;

public class TriangleBlock {
	private final int cap;
	private final Triangle base;

	private int size;

	public TriangleBlock(int cap) {
		if (cap <= 0)
			throw new IllegalArgumentException();
		this.cap = cap;
		this.base = Struct.mallocArrayBase(Triangle.class, cap);
		this.size = 0;
	}

	public void clear() {
		size = 0;
	}

	public int size() {
		return size;
	}

	@TakeStruct
	public Triangle add() {
		return this.get(size++);
	}

	@TakeStruct
	public Triangle add(Triangle src) {
		Triangle dst = this.add();
		Struct.copy(Triangle.class, src, dst);
		return dst;
	}

	public void addRange(TriangleBlock src, int off, int len) {
		if (StructEnv.SAFETY_FIRST)
			if (off < 0 || len < 0 || off + len > src.size)
				throw new IllegalStateException();
		if (StructEnv.SAFETY_FIRST)
			if (len > this.cap - this.size)
				throw new IllegalStateException();
		int offset = this.size;
		this.size += len;
		Struct.copy(Triangle.class, src.get(off), this.get(offset), len);
	}

	public void addAll(TriangleBlock src) {
		if (StructEnv.SAFETY_FIRST)
			if (src.size > this.cap - this.size)
				throw new IllegalStateException();
		int offset = this.size;
		this.size += src.size;
		Struct.copy(Triangle.class, src.base, this.get(offset), src.size);
	}

	@TakeStruct
	public Triangle get(int index) {
		if (StructEnv.SAFETY_FIRST)
			if (index < 0 || index >= size)
				throw new IllegalStateException();
		return Struct.index(base, Triangle.class, index);
	}

	public void set(int index, Triangle value) {
		Struct.copy(Triangle.class, value, this.get(index));
	}

	public void free() {
		Struct.free(base);
	}
}