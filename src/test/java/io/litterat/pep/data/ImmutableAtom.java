package io.litterat.pep.data;

import io.litterat.pep.Data;

public class ImmutableAtom {

	private final SimpleEnum enumCount;

	private final String str;

	private final boolean bool;

	@Data
	public ImmutableAtom(SimpleEnum enumCount, String str, boolean bool) {
		this.enumCount = enumCount;
		this.str = str;
		this.bool = bool;
	}

	public SimpleEnum enumCount() {
		return enumCount;
	}

	public String str() {
		return str;
	}

	public boolean bool() {
		return bool;
	}

}
