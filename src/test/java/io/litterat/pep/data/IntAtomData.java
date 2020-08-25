package io.litterat.pep.data;

import io.litterat.pep.Data;

public class IntAtomData {

	private final IntAtom intAtom;

	@Data
	public IntAtomData(IntAtom intAtom) {
		this.intAtom = intAtom;
	}

	public IntAtom intAtom() {
		return intAtom;
	}
}
