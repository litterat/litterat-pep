package io.litterat.pep;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.litterat.pep.data.SimpleUUIDImmutable;
import io.litterat.pep.data.UUIDBridge;

public class UUIDBridgeTest {

	final static UUID FIRST_UUID = UUID.randomUUID();
	final static UUID SECOND_UUID = UUID.randomUUID();

	SimpleUUIDImmutable test = new SimpleUUIDImmutable(FIRST_UUID, SECOND_UUID);

	PepContext context;

	@BeforeEach
	public void setup() {
		context = PepContext.builder().build();
	}

	@Test
	public void checkDescriptor() throws Throwable {
		context.registerAtom(SimpleUUIDImmutable.class, new UUIDBridge());

	}
}
