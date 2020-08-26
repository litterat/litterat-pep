package io.litterat.pep.data;

import io.litterat.pep.Data;
import io.litterat.pep.Field;

/**
 * 
 * This is complex in the sense that it is modifying the input or output values of X and Y.
 * The byte code invariance verifier will not be able to resolve which parameters are X and Y.
 * Field annotations are used to assist the verifier.
 *
 */
public class ComplexImmutable {

	public static int multiplier = 2;

	private final int x;
	private final int y;

	@Data
	public ComplexImmutable(int x, @Field(name = "y") int y) {
		this.x = x;

		// hopefully this isn't removed.
		int test = y * multiplier;
		this.y = test / multiplier;
	}

	public int x() {
		int test = x * multiplier;
		return test / multiplier;
	}

	public int y() {
		return y;
	}
}
