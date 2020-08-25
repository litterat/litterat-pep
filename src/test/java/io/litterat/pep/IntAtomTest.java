package io.litterat.pep;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.litterat.pep.data.IntAtom;
import io.litterat.pep.data.IntAtomData;
import io.litterat.pep.mapper.PepArrayMapper;
import io.litterat.pep.mapper.PepMapMapper;

public class IntAtomTest {

	final static IntAtom INT_ATOM_TEST = IntAtom.getAtom(23);

	IntAtomData test = new IntAtomData(INT_ATOM_TEST);

	PepContext context;

	@BeforeEach
	public void setup() {
		context = PepContext.builder().build();
	}

	@Test
	public void testToArray() throws Throwable {

		// project to an array.
		PepArrayMapper arrayMap = new PepArrayMapper(context);
		Object[] values = arrayMap.toArray(test);
		Assertions.assertNotNull(values);

		// rebuild as an object.
		IntAtomData object = arrayMap.toObject(IntAtomData.class, values);

		// Validate
		Assertions.assertNotNull(object);
		Assertions.assertTrue(object instanceof IntAtomData);
		Assertions.assertEquals(INT_ATOM_TEST, test.intAtom());
	}

	@Test
	public void testToMap() throws Throwable {

		PepMapMapper mapMapper = new PepMapMapper(context);
		Map<String, Object> map = mapMapper.toMap(test);

		IntAtomData object = (IntAtomData) mapMapper.toObject(IntAtomData.class, map);

		// validate result.
		Assertions.assertNotNull(object);
		Assertions.assertTrue(object instanceof IntAtomData);
		Assertions.assertEquals(INT_ATOM_TEST, test.intAtom());
	}
}
