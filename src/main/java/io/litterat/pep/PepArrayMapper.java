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
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PepArrayMapper {

	private final PepContext context;

	private final Map<Class<?>, ArrayFunctions> functionCache;

	private static class ArrayFunctions {

		// Converts from Object[] to targetClass. Has signature: Object[] project( T object );
		public final MethodHandle toArray;

		// constructs, calls setters and embeds. Has signature: T embed( Object[] values ).
		public final MethodHandle toObject;

		public ArrayFunctions(MethodHandle toArray, MethodHandle toObject) {
			this.toArray = toArray;
			this.toObject = toObject;

		}
	}

	public PepArrayMapper(PepContext context) {
		this.context = context;
		this.functionCache = new HashMap<>();

	}

	private ArrayFunctions getFunctions(Class<?> clss) throws PepException {
		ArrayFunctions af = functionCache.get(clss);
		if (af == null) {
			PepDataClass dataClass = context.getDescriptor(clss);

			MethodHandle toArray = createProjectFunction(dataClass);
			MethodHandle toObject = createEmbedFunction(dataClass);

			af = new ArrayFunctions(toArray, toObject);
			functionCache.put(clss, af);
		}
		return af;
	}

	/**
	 * Convenience function. Takes the target object of this descriptor and return an object array.
	 * 
	 * @param o target object instance to project to object[]
	 * @return values from target object
	 * @throws Throwable any failure from the project function.
	 */
	public Object[] toArray(Object o) throws Throwable {
		Objects.requireNonNull(o);

		ArrayFunctions af = getFunctions(o.getClass());

		// create and fill array with project instance.
		return (Object[]) af.toArray.invoke(o);
	}

	/**
	 * Convenience function. Takes an array of values based on the field types and returns the target
	 * object.
	 * 
	 * @param values object values to embed into target object.
	 * @return recreated target object.
	 * @throws Throwable any failure from the embed function.
	 */
	public <T> T toObject(Class<T> clss, Object[] values) throws Throwable {
		Objects.requireNonNull(clss);
		Objects.requireNonNull(values);

		ArrayFunctions af = getFunctions(clss);

		// create the embedded object.
		return (T) af.toObject.invoke(values);
	}

	/**
	 * Creates the embed method handle. Will create the serial instance, call setters, and class the
	 * embed method handle to create the target object in a single call. This is equivalent to:
	 * 
	 * @formatter:off
	 * 
	 * // fields mapped as required from value array.
	 * T t = new EmbedClass( values[0], values[1], ... ); 
	 * 
	 * // calls the embed function on the object.
	 * return embed( t ); 
	 * 
	 * @formatter:on
	 * 
	 * @param objectConstructor
	 * @param fields
	 * @return a single MethodHandle to generate target object from Object[]
	 * @throws PepException 
	 */
	private MethodHandle createEmbedFunction(PepDataClass dataClass) throws PepException {

		// (Object[]):serialClass -> ctor(Object[])
		MethodHandle create = createEmbedConstructor(dataClass);

		// (Object[]) -> embed( ctor(Object[]).setValues(Object[]) )
		return MethodHandles.collectArguments(dataClass.toObject(), 0, create);
	}

	/**
	 * Builds a constructor that takes Object[] as constructor arguments and return an object instance.
	 *
	 * @param objectConstructor
	 * @param fields
	 * @return
	 * @throws PepException
	 */
	private MethodHandle createEmbedConstructor(PepDataClass dataClass) throws PepException {
		MethodHandle result = dataClass.constructor();

		PepDataComponent[] fields = dataClass.dataComponents();
		for (int x = 0; x < dataClass.dataComponents().length; x++) {
			PepDataComponent field = fields[x];

			int arg = x;
			int inputIndex = x;

			// (values[],int) -> values[int]
			MethodHandle arrayGetter = MethodHandles.arrayElementGetter(Object[].class);

			// () -> inputIndex
			MethodHandle index = MethodHandles.constant(int.class, inputIndex);

			// (values[]) -> values[inputIndex]
			MethodHandle arrayIndexGetter = MethodHandles.collectArguments(arrayGetter, 1, index)
					.asType(MethodType.methodType(field.type(), Object[].class));

			// Pass the object through toObject if it isn't an atom.
			if (!field.dataClass().isAtom()) {
				ArrayFunctions af = this.getFunctions(field.dataClass().typeClass());

				arrayIndexGetter = MethodHandles.collectArguments(af.toObject, 0, arrayIndexGetter);
			}

			// ()-> constructor( ..., values[inputIndex] , ... )
			result = MethodHandles.collectArguments(result, arg, arrayIndexGetter);

		}

		// spread the arguments so ctor(Object[],Object[]...) becomes ctor(Object[])
		int paramCount = dataClass.constructor().type().parameterCount();
		if (paramCount > 0) {
			int[] permuteInput = new int[paramCount];
			result = MethodHandles.permuteArguments(result,
					MethodType.methodType(dataClass.dataClass(), Object[].class), permuteInput);
		}
		return result;
	}

	/**
	 * create project takes a target object and returns an Object[] of values. Not yet complete. Haven't
	 * worked out how to re-use the Object[] in return value.
	 * 
	 * @formatter:off
	 * 
     * // Project the instance to the embedded version.
     * EmbeddedClass e = project.invoke(o)
     * 
     * // Extract the values from the projected object.
     * Object[] values = new Object[fields.length];
     * 
     * // Call the various accessors to fill in the array and return values.
	 * return getter.invoke(e, values );

	 * @formatter:on
	 * 
	 * @param fields
	 * @return
	 * @throws PepException 
	 */
	private MethodHandle createProjectFunction(PepDataClass dataClass) throws PepException {

		// (int):Object[] -> new Object[int]
		MethodHandle createArray = MethodHandles.arrayConstructor(Object[].class);

		// (int):length -> fields.length
		MethodHandle index = MethodHandles.constant(int.class, dataClass.dataComponents().length);

		// ():Object[] -> new Object[fields.length]
		MethodHandle arrayCreate = MethodHandles.collectArguments(createArray, 0, index);

		// (Object[],serialClass):void -> getters(Object[],serialClass)
		MethodHandle getters = createProjectGetters(dataClass);

		// (Object[],targetClass):void -> getters(Object[], project(targetClass))
		MethodHandle projectGetters = MethodHandles.collectArguments(getters, 1, dataClass.toData());

		// ():Object[] -> return new Object[fields.length];
		MethodHandle returnArray = MethodHandles.collectArguments(projectGetters, 0, arrayCreate);

		return returnArray;
	}

	/**
	 * Create the getters
	 * 
	 * @param fields
	 * @return
	 * @throws PepException
	 */
	private MethodHandle createProjectGetters(PepDataClass dataClass) throws PepException {

		// (object[]):object[] -> return object[];
		MethodHandle identity = MethodHandles.identity(Object[].class);

		// (Object[], embedClass):object[] -> return object[];
		MethodHandle result = MethodHandles.dropArguments(identity, 1, dataClass.dataClass());

		PepDataComponent[] fields = dataClass.dataComponents();
		for (int x = 0; x < fields.length; x++) {

			PepDataComponent field = fields[x];
			int outputIndex = x;

			// (value[],x, v) -> value[x] = v
			MethodHandle arraySetter = MethodHandles.arrayElementSetter(Object[].class);

			// () -> outputIndex
			MethodHandle index = MethodHandles.constant(int.class, outputIndex);

			// (value[],v) -> value[inputIndex] = v
			MethodHandle arrayIndexSetter = MethodHandles.collectArguments(arraySetter, 1, index);

			// (object) -> (Object) object.getter()
			MethodHandle fieldBox = field.accessor().asType(MethodType.methodType(Object.class, dataClass.dataClass()));

			// Pass the object through toArray if it isn't an atom.
			if (!field.dataClass().isAtom()) {
				ArrayFunctions af = this.getFunctions(field.dataClass().typeClass());

				fieldBox = MethodHandles.collectArguments(af.toArray, 0, fieldBox);
			}

			// (value[],object) -> value[inputIndex] = object.getter()
			MethodHandle arrayValueSetter = MethodHandles.collectArguments(arrayIndexSetter, 1, fieldBox);

			// add to list of getters.
			result = MethodHandles.foldArguments(result, arrayValueSetter);

		}

		// (object[]):object[] -> ... callGetters(object[]); ... return object[];
		return result;
	}
}
