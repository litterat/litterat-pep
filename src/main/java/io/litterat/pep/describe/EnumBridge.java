package io.litterat.pep.describe;

import io.litterat.pep.DataObjectBridge;

/**
 * 
 * Default bridge for Enums that converts to/from String.
 *
 */
@SuppressWarnings("rawtypes")
public class EnumBridge implements DataObjectBridge<String, Enum> {

	private final Class enumType;

	public EnumBridge(Class enumType) {
		this.enumType = enumType;
	}

	@Override
	public String toData(Enum b) {

		return b.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Enum toObject(String s) {

		return Enum.valueOf(enumType, s);
	}

}
