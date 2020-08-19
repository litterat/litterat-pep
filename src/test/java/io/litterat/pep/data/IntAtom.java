package io.litterat.pep.data;

import java.util.HashMap;
import java.util.Map;

import io.litterat.pep.PepAtom;

public class IntAtom {

	private final int id;

	private IntAtom(int id) {
		this.id = id;
	}

	public int id() {
		return id;
	}

	private static final Map<Integer, IntAtom> atomList = new HashMap<>();

	@PepAtom
	public static final IntAtom getAtom(int id) {
		IntAtom atom = atomList.get(id);
		if (atom == null) {
			atom = new IntAtom(id);
			atomList.put(id, atom);
		}
		return atom;
	}
}
