package io.litterat.pep.data;

import java.util.UUID;

import io.litterat.pep.DataObjectBridge;

public class UUIDBridge implements DataObjectBridge<String, UUID> {

	@Override
	public String toData(UUID object) {
		return object.toString();
	}

	@Override
	public UUID toObject(String data) {
		return UUID.fromString(data);
	}

}
