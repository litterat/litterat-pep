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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * A PepClassDescriptor provides a descriptor for a classes projected/embedded pair for use in
 * serialization libraries.
 *
 */
public class PepDataClass {

	// The class to be projected.
	private final Class<?> typeClass;

	// The embedded class type.
	private final Class<?> dataClass;

	// Constructor for the data object.
	private final MethodHandle constructor;

	// Method handle to convert object to data object.
	private final MethodHandle toData;

	// Method handle to convert data object to target object.
	private final MethodHandle toObject;

	// All fields in the projected class.
	private final PepDataComponent[] dataComponents;

	// Target class is data. No extract/inject required.
	private final boolean isData;

	// An atom is any value that is passed through as is.
	private final boolean isAtom;

	public PepDataClass(Class<?> targetType, Class<?> serialType, MethodHandle constructor, MethodHandle toData,
			MethodHandle toObject, PepDataComponent[] fields, boolean isAtom) {
		this.typeClass = targetType;
		this.dataClass = serialType;
		this.dataComponents = fields;
		this.constructor = constructor;
		this.toData = toData;
		this.toObject = toObject;
		this.isData = (targetType == serialType);
		this.isAtom = isAtom;
	}

	public PepDataClass(Class<?> targetType, Class<?> serialType, MethodHandle constructor, MethodHandle toData,
			MethodHandle toObject, PepDataComponent[] fields) {
		this(targetType, serialType, constructor, toData, toObject, fields, false);
	}

	// An Atom uses identity function for toData/toObject and construct.
	public PepDataClass(Class<?> targetType) {
		this(targetType, targetType, identity(targetType), identity(targetType), identity(targetType),
				new PepDataComponent[0], true);
	}

	// An Atom with conversion functions. e.g. String <--> UUID
	public PepDataClass(Class<?> targetType, MethodHandle toData, MethodHandle toObject) {
		this(targetType, targetType, identity(targetType), toData, toObject, new PepDataComponent[0], true);
	}

	private static MethodHandle identity(Class<?> targetType) {
		return MethodHandles.identity(targetType);
	}

	/**
	 * @return The class this descriptor is for.
	 */
	public Class<?> typeClass() {
		return typeClass;
	}

	/**
	 * @return The embedded class. This may be equal to the target class.
	 */
	public Class<?> dataClass() {
		return dataClass;
	}

	public boolean isData() {
		return isData;
	}

	public boolean isAtom() {
		return isAtom;
	}

	/**
	 * @return A MethodHandle that has the signature T embed(Object[] values).
	 */
	public MethodHandle constructor() {
		return constructor;
	}

	/**
	 * @return A MethodHandle that has the signature T embed(Object[] values).
	 */
	public MethodHandle toObject() {
		return toObject;
	}

	/**
	 * @return A MethodHandle that has the signature Object[] project(T object)
	 */
	public MethodHandle toData() {
		return toData;
	}

	/**
	 * @return The list of fields and their types returned by the embed function.
	 */
	public PepDataComponent[] dataComponents() {
		return dataComponents;
	}

}
