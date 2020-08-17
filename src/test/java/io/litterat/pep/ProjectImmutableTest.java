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

import io.litterat.pep.projects.ProjectImmutable;

public class ProjectImmutableTest {

	@Test
	public void simpleImmutableTest() throws Throwable {
		PepContext context = new PepContext();
		PepClassDescriptor<ProjectImmutable> descriptor = context.getDescriptor(ProjectImmutable.class);
		Assertions.assertNotNull(descriptor);

		Assertions.assertEquals(ProjectImmutable.class, descriptor.typeClass());
		Assertions.assertEquals(ProjectImmutable.ProjectImmutableData.class, descriptor.dataClass());

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

		ProjectImmutable test = new ProjectImmutable(xValue, yValue);

		// project to an array.
		PepArrayMapper<ProjectImmutable> arrayMap = new PepArrayMapper<>(descriptor);
		Object[] values = arrayMap.toArray(test);
		Assertions.assertNotNull(values);

		// rebuild as an object.
		ProjectImmutable embed = arrayMap.toObject(values);
		Assertions.assertNotNull(embed);
		if (!(embed instanceof ProjectImmutable)) {
			Assertions.fail();
		}

		ProjectImmutable si = embed;
		Assertions.assertEquals(xValue, si.x());
		Assertions.assertEquals(yValue, si.y());

	}
}
