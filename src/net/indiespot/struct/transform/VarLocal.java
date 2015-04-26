package net.indiespot.struct.transform;

import java.util.Arrays;
import java.util.EnumSet;

public class VarLocal {
	private final int slots;
	private int slotBoundary = -1;
	private int expandedSlotBoundary;
	private final VarType[] local;
	private int[] oldSlot2newSlot;
	private int[] local2pntrBase;

	public VarLocal(int slots) {
		this.slots = slots;
		this.local = new VarType[slots * 10 + 10];
		this.local2pntrBase = new int[slots * 10 + 10];
		Arrays.fill(local2pntrBase, -1);
	}

	public int setupParamsRemapTable(int paramSlots, int usedLocalvarSlots) {
		if (StructEnv.PRINT_LOG)
			System.out.println("setupParamsRemapTable:" + paramSlots + ":" + usedLocalvarSlots);

		if (StructEnv.PRINT_LOG)
			this.dump("setup0", false);

		this.oldSlot2newSlot = new int[this.local.length];
		int offset = 0;
		int result = 0;
		for (int i = 0; i < local.length; i++) {
			oldSlot2newSlot[i] = offset++;
			if (local[i] == VarType.STRUCT_PLACEHOLDER)
				offset++; // make room for expansion
			if (i < paramSlots)
				result = offset;
			if (StructEnv.PRINT_LOG)
				if (local[i] != null)
					System.out.println("\t\t\tlocal.map[" + local[i] + "]: " + i + " -> " + oldSlot2newSlot[i]);
		}
		this.slotBoundary = usedLocalvarSlots + (oldSlot2newSlot[paramSlots] - paramSlots);
		if (StructEnv.PRINT_LOG)
			System.out.println("slotBoundary=" + usedLocalvarSlots + " -> " + this.slotBoundary);

		if (StructEnv.PRINT_LOG)
			this.dump("setup1", false);

		for (int src = local.length - 1; src >= 0; src--)
			if (oldSlot2newSlot[src] < local.length)
				local[oldSlot2newSlot[src]] = local[src];

		for (int i = 0; i < local.length; i++) {
			if (local[i] == VarType.STRUCT_PLACEHOLDER) {
				local2pntrBase[i] = i;
				// expand reference to struct-pointer
				local[i + 0] = VarType.STRUCT_HI;
				local[i + 1] = VarType.STRUCT_LO;
			}
		}

		if (StructEnv.PRINT_LOG)
			this.dump("setup2", true);
		return result;
	}

	public void dump(String desc, boolean withMapping) {
		System.out.println("local.dump." + desc);
		for (int i = 0; i < local.length; i++) {
			if (i == slotBoundary)
				System.out.println("\t\t\tboundary: " + i);

			if (local[i] == VarType.STRUCT_PLACEHOLDER && withMapping)
				System.out.println("\t\t\tlocal: " + i + " -> " + local[i] + " refering to (" + local2pntrBase[i] + ")");
			else if (local[i] != null)
				System.out.println("\t\t\tlocal: " + i + " -> " + local[i]);
		}

		if (withMapping)
			for (int i = 0; i < local.length; i++)
				if (oldSlot2newSlot[i] < local.length)
					if (local[oldSlot2newSlot[i]] != null)
						System.out.println("\t\t\tmapped: " + i + " -> " + oldSlot2newSlot[i] + ": " + local[oldSlot2newSlot[i]]);

	}

	public int remap(int index) {
		if (oldSlot2newSlot != null)
			index = oldSlot2newSlot[index];
		return index;
	}

	public int set(final int index, VarType var) {
		int mapped = this.remap(index);

		if (StructEnv.PRINT_LOG)
			System.out.println("\t\t\tlocal.set(" + index + " -> " + mapped + ", " + var.name() + ") was " + (local[mapped] == null ? "UNDEFINED" : local[mapped]));
		if (mapped >= local.length)
			throw new ArrayIndexOutOfBoundsException(mapped + "/" + local.length);
		local[mapped] = var;
		if (StructEnv.PRINT_LOG)
			System.out.println("\t\t\tlocal dump: " + this.toString());

		if (var == VarType.STRUCT_PLACEHOLDER && slotBoundary != -1) {
			if (local2pntrBase[mapped] == -1) {
				local2pntrBase[mapped] = slotBoundary + expandedSlotBoundary;
				expandedSlotBoundary += 2;
			}
			local[local2pntrBase[mapped] + 0] = VarType.STRUCT_HI;
			local[local2pntrBase[mapped] + 1] = VarType.STRUCT_LO;
			if (StructEnv.PRINT_LOG)
				this.dump("set(" + index + ")", true);
			return local2pntrBase[mapped];
		}
		return mapped;
	}

	public int getStructBaseIndexMapped(int index) {
		int mapped = oldSlot2newSlot[index];
		if (local[mapped] != VarType.STRUCT_PLACEHOLDER)
			throw new IllegalStateException("index=" + index + ", mapped=" + mapped + ", expected STRUCT_LOCALVAR, found " + local[mapped]);
		if (local[local2pntrBase[mapped]] != VarType.STRUCT_HI)
			throw new IllegalStateException("index=" + index + ", mapped=" + mapped + ", base=" + local2pntrBase[mapped] + ",  expected STRUCT_HI, found " + local[local2pntrBase[mapped]]);
		return local2pntrBase[mapped];
	}

	public VarType getUnmapped(final int index) {
		return local[index];
	}

	public VarType get(final int index) {
		if (StructEnv.PRINT_LOG)
			System.out.println("\t\t\tlocal dump: " + this.toString());

		int mapped = oldSlot2newSlot[index];
		if (local[mapped] == null)
			throw new IllegalStateException();
		VarType var = local[mapped];

		if (var == VarType.STRUCT_PLACEHOLDER && slotBoundary != -1) {
			if (local2pntrBase[mapped] == -1)
				throw new IllegalStateException();

			int off0 = local2pntrBase[mapped] + 0;
			if (local[off0] != VarType.STRUCT_HI)
				throw new IllegalStateException("index=" + index + ", mapped=" + mapped + ", expected STRUCT_HI at " + off0 + ", found: " + local[off0]);

			int off1 = local2pntrBase[mapped] + 1;
			if (local[off1] != VarType.STRUCT_LO)
				throw new IllegalStateException("index=" + index + ", mapped=" + mapped + ", expected STRUCT_LO at " + off1 + ", found: " + local[off1]);
		}
		return var;
	}

	public VarType getEQ(int index, VarType type) {
		if (type == VarType.STRUCT_PLACEHOLDER)
			throw new UnsupportedOperationException();
		if (type != get(index))
			throw new IllegalStateException("expected: " + type + ", found: " + get(index));
		return type;
	}

	public VarType getEQ(int index, EnumSet<VarType> types) {
		VarType got = get(index);
		if (types.contains(VarType.STRUCT_PLACEHOLDER))
			throw new UnsupportedOperationException();
		if (!types.contains(got))
			throw new IllegalStateException("expected: " + types + ", found: " + got);
		return got;
	}

	public VarLocal copy() {
		VarLocal copy = new VarLocal(slots);
		System.arraycopy(this.local, 0, copy.local, 0, this.local.length);
		copy.oldSlot2newSlot = this.oldSlot2newSlot; // can be shared, is
														// read-only
		copy.expandedSlotBoundary = this.expandedSlotBoundary;
		copy.slotBoundary = this.slotBoundary;
		copy.local2pntrBase = this.local2pntrBase.clone();
		return copy;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');

		int maxNonNull = -1;
		for (int i = 0; i < local.length; i++)
			if (local[i] != null)
				maxNonNull = i;
		for (int i = 0; i <= maxNonNull; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(local[i]);
		}
		sb.append(']');
		return sb.toString();
	}
}
