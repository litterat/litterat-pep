package io.litterat.pep.data;

import io.litterat.pep.Data;

public class SimpleArray {

	private final SimpleImmutable[] arrayImmutable;

	@Data
	public SimpleArray(SimpleImmutable[] array) {
		this.arrayImmutable = array;
	}

	public SimpleImmutable[] arrayImmutable() {
		return arrayImmutable;
	}

}
