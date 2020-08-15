/*
 * Copyright (c) 2020, Live Media Pty. Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.litterat.pep;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.litterat.pep.immutable.SimpleImmutable;

public class ImmutableDescriptorTest {

	@Test
	public void simpleImmutableTest() throws Throwable {
		PepContext context = new PepContext();
		PepClassDescriptor<SimpleImmutable> descriptor = context.getDescriptor(SimpleImmutable.class);
		Assertions.assertNotNull(descriptor);

		Assertions.assertEquals(SimpleImmutable.class, descriptor.targetClass());
		Assertions.assertEquals(SimpleImmutable.class, descriptor.serialClass());

		PepFieldDescriptor[] fields = descriptor.fields();
		Assertions.assertNotNull(fields);
		Assertions.assertEquals(2, fields.length);

		PepFieldDescriptor fieldX = fields[0];
		Assertions.assertEquals("x", fieldX.name());
		Assertions.assertEquals(false, fieldX.isOptional());
		Assertions.assertEquals(int.class, fieldX.type());

		PepFieldDescriptor fieldY = fields[1];
		Assertions.assertEquals("y", fieldY.name());
		Assertions.assertEquals(false, fieldY.isOptional());
		Assertions.assertEquals(int.class, fieldY.type());

		final int xValue = 1;
		final int yValue = 2;

		SimpleImmutable test = new SimpleImmutable(xValue, yValue);

		// project to an array.
		Object[] values = descriptor.project(test);

		// rebuild as an object.
		SimpleImmutable embed = descriptor.embed(values);
		Assertions.assertNotNull(embed);
		if (!(embed instanceof SimpleImmutable)) {
			Assertions.fail();
		}

		SimpleImmutable si = embed;
		Assertions.assertEquals(xValue, si.x());
		Assertions.assertEquals(yValue, si.y());

	}
}
