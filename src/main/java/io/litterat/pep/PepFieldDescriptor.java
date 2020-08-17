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

public class PepFieldDescriptor {

	// name of the field
	private final String name;

	// type of the field
	private final Class<?> type;

	// is the field optional
	private final boolean isOptional;

	// accessor read handle. signature: type t = object.getT();
	private final MethodHandle readHandle;

	public PepFieldDescriptor(String name, Class<?> type, boolean isOptional, MethodHandle readHandle,
			MethodHandle writeHandle, int ctorArg) {
		this.name = name;
		this.type = type;
		this.isOptional = isOptional;
		this.readHandle = readHandle;
	}

	public String name() {
		return name;
	}

	public Class<?> type() {
		return type;
	}

	public boolean isOptional() {
		return isOptional;
	}

	public MethodHandle accessor() {
		return readHandle;
	}

}
