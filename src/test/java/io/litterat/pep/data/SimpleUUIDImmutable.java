package io.litterat.pep.data;

import java.util.UUID;

public class SimpleUUIDImmutable {

	private final UUID first;
	private final UUID second;

	public SimpleUUIDImmutable(UUID first, UUID second) {
		this.first = first;
		this.second = second;
	}

	public UUID first() {
		return this.first;
	}

	public UUID second() {
		return second;
	}

}
