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

/**
 * 
 * This is analagous to the Java reflection RecordComponent class but for Data classes.
 *
 */
public class PepDataComponent {

	// name of the field
	private final String name;

	// type of the field
	private final Class<?> type;

	private final PepDataClass dataClass;

	// is the field optional
	private final boolean isOptional;

	// accessor read handle. signature: type t = object.getT();
	private final MethodHandle accessor;

	public PepDataComponent(String name, Class<?> type, PepDataClass dataClass, boolean isOptional,
			MethodHandle readHandle, MethodHandle writeHandle, int ctorArg) {
		this.name = name;
		this.type = type;
		this.dataClass = dataClass;
		this.isOptional = isOptional;
		this.accessor = readHandle;
	}

	public String name() {
		return name;
	}

	public Class<?> type() {
		return type;
	}

	public PepDataClass dataClass() {
		return dataClass;
	}

	public boolean isOptional() {
		return isOptional;
	}

	public MethodHandle accessor() {
		return accessor;
	}

}
