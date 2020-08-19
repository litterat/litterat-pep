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
package io.litterat.pep.mapper;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.litterat.pep.PepContext;
import io.litterat.pep.PepDataClass;
import io.litterat.pep.PepDataComponent;
import io.litterat.pep.PepException;

/**
 * 
 * Sample showing how to use the Pep library to convert an Object to/from Map<String,Object>
 * 
 */
public class PepMapMapper {

	private final PepContext context;

	public PepMapMapper(PepContext context) {
		this.context = context;
	}

	public Map<String, Object> toMap(Object object) throws PepException {

		Objects.requireNonNull(object);

		PepDataClass dataClass = context.getDescriptor(object.getClass());
		int fieldIndex = 0;

		try {
			Object data = dataClass.toData().invoke(object);

			Map<String, Object> map = new HashMap<>();

			PepDataComponent[] fields = dataClass.dataComponents();
			for (fieldIndex = 0; fieldIndex < dataClass.dataComponents().length; fieldIndex++) {
				PepDataComponent field = fields[fieldIndex];

				Object v = field.accessor().invoke(data);

				// Recursively convert object to map.
				if (v != null) {
					PepDataClass fieldDataClass = field.dataClass();

					// TODO array handling.
					if (fieldDataClass.isAtom()) {
						v = fieldDataClass.toData().invoke(v);
					} else {
						v = toMap(v);
					}
				}

				map.put(field.name(), v);
			}
			return map;
		} catch (Throwable t) {
			throw new PepException(String.format("Failed to convert %s to Map. Could not convert field %s",
					dataClass.typeClass(), dataClass.dataComponents()[fieldIndex].name()), t);
		}
	}

	@SuppressWarnings("unchecked")
	public Object toObject(Class<?> clss, Map<String, Object> map) throws PepException {

		Objects.requireNonNull(clss);
		Objects.requireNonNull(map);

		PepDataClass dataClass = context.getDescriptor(clss);
		int fieldIndex = 0;

		try {

			PepDataComponent[] fields = dataClass.dataComponents();
			Object[] construct = new Object[fields.length];
			for (fieldIndex = 0; fieldIndex < dataClass.dataComponents().length; fieldIndex++) {
				PepDataComponent field = fields[fieldIndex];

				Object v = map.get(field.name());

				// Recursively convert maps back to objects.
				if (v != null) {
					PepDataClass fieldDataClass = field.dataClass();

					// TODO array handling.
					if (fieldDataClass.isAtom()) {
						v = fieldDataClass.toObject().invoke(v);
					} else {
						v = toObject(fieldDataClass.typeClass(), (Map<String, Object>) v);
					}
				}
				construct[fieldIndex] = v;
			}

			// Convert constructor to take Object[]
			MethodHandle constructor = dataClass.constructor().asSpreader(Object[].class,
					dataClass.dataComponents().length);

			Object data = constructor.invoke(construct);

			return dataClass.toObject().invoke(data);
		} catch (Throwable t) {
			throw new PepException(String.format("Failed to convert Map to %s. Incorrect value for field %s",
					dataClass.typeClass(), dataClass.dataComponents()[fieldIndex].name()), t);
		}

	}
}
