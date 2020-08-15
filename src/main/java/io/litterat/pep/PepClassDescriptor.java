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

/**
 * A PepClassDescriptor provides a descriptor for a classes projected/embedded pair for use in
 * serialization libraries.
 *
 * @param <T> Pep class descriptor for type T
 */
public class PepClassDescriptor<T> {

	// The class to be projected.
	private final Class<T> targetClass;

	// The embedded class type.
	private final Class<?> embedClass;

	// constructs, calls setters and embeds. Has signature: T embed( Object[] values ).
	private final MethodHandle embedFunction;

	// Converts from Object[] to targetClass. Has signature: Object[] project( T object );
	private final MethodHandle projectFunction;

	// All fields in the projected class.
	private final PepFieldDescriptor[] fields;

	public PepClassDescriptor(Class<T> targetType, Class<?> serialType, MethodHandle constructor, MethodHandle project,
			MethodHandle embed, PepFieldDescriptor[] fields) {
		this.targetClass = targetType;
		this.embedClass = serialType;
		this.fields = fields;

		this.projectFunction = createProjectFunction(fields, project);
		this.embedFunction = createEmbedFunction(constructor, fields, embed);
	}

	/**
	 * @return The class this descriptor is for.
	 */
	public Class<T> targetClass() {
		return targetClass;
	}

	/**
	 * @return The embedded class. This may be equal to the target class.
	 */
	public Class<?> serialClass() {
		return embedClass;
	}

	/**
	 * @return A MethodHandle that has the signature T embed(Object[] values).
	 */
	public MethodHandle embed() {
		return embedFunction;
	}

	/**
	 * @return A MethodHandle that has the signature Object[] project(T object)
	 */
	public MethodHandle project() {
		return projectFunction;
	}

	/**
	 * @return The list of fields and their types returned by the embed function.
	 */
	public PepFieldDescriptor[] fields() {
		return fields;
	}

	/**
	 * Convenience function. Takes the target object of this descriptor and return an object array.
	 * 
	 * @param o
	 * @return
	 * @throws Throwable
	 */
	public Object[] project(T o) throws Throwable {

		// create and fill array with project instance.
		return (Object[]) projectFunction.invoke(o);
	}

	/**
	 * Convenience function. Takes an array of values based on the field types and returns the target
	 * object.
	 * 
	 * @param values
	 * @return
	 * @throws Throwable
	 */
	public T embed(Object[] values) throws Throwable {

		// create the embedded object.
		return (T) embedFunction.invoke(values);
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
	 * // calls setters as required from value array. 
	 * embedSetters.invoke( t, values ); 
	 * 
	 * // calls the embed function on the object.
	 * return embed( t ); 
	 * 
	 * @formatter:on
	 * 
	 * @param objectConstructor
	 * @param fields
	 * @return a single MethodHandle to generate target object from Object[]
	 */
	private MethodHandle createEmbedFunction(MethodHandle objectConstructor, PepFieldDescriptor[] fields,
			MethodHandle embed) {

		// (Object[]):serialClass -> ctor(Object[])
		MethodHandle create = createEmbedConstructor(objectConstructor, fields);

		// (serialClass, Object[]) -> serialClass.setValues(Object[x]);
		MethodHandle setters = createEmbedSetters(fields);

		// (Object[], Object[]) -> ctor(Object[]).setvalues(Object[])
		MethodHandle createAndSet = MethodHandles.collectArguments(setters, 1, create);

		// (Object[]) -> ctor(Object[]).setValues(Object[])
		MethodHandle createSet = MethodHandles.permuteArguments(createAndSet,
				MethodType.methodType(embedClass, Object[].class), 0, 0);

		// (Object[]) -> embed( ctor(Object[]).setValues(Object[]) )
		return MethodHandles.collectArguments(embed, 0, createSet);
	}

	/**
	 * Builds a constructor that takes Object[] as constructor arguments and return an object instance.
	 *
	 * @param objectConstructor
	 * @param fields
	 * @return
	 */
	private MethodHandle createEmbedConstructor(MethodHandle objectConstructor, PepFieldDescriptor[] fields) {
		MethodHandle result = objectConstructor;

		for (int x = 0; x < fields.length; x++) {
			PepFieldDescriptor field = fields[x];

			if (field.writeHandle() == null) {

				int arg = field.constructorArgument();
				int inputIndex = x;

				// (values[],int) -> values[int]
				MethodHandle arrayGetter = MethodHandles.arrayElementGetter(Object[].class);

				// () -> inputIndex
				MethodHandle index = MethodHandles.constant(int.class, inputIndex);

				// (values[]) -> values[inputIndex]
				MethodHandle arrayIndexGetter = MethodHandles.collectArguments(arrayGetter, 1, index)
						.asType(MethodType.methodType(field.type(), Object[].class));

				// ()-> constructor( ..., values[inputIndex] , ... )
				result = MethodHandles.collectArguments(result, arg, arrayIndexGetter);
			}
		}

		// spread the arguments so ctor(Object[],Object[]...) becomes ctor(Object[])
		int paramCount = objectConstructor.type().parameterCount();
		if (paramCount > 0) {
			int[] permuteInput = new int[paramCount];
			result = MethodHandles.permuteArguments(result, MethodType.methodType(embedClass, Object[].class),
					permuteInput);
		}
		return result;
	}

	/**
	 * Creates the setters for the projected class.
	 * 
	 * @param fields
	 * @return
	 */
	private MethodHandle createEmbedSetters(PepFieldDescriptor[] fields) {

		// (object):object -> return object;
		MethodHandle result = MethodHandles.identity(embedClass);

		for (int x = 0; x < fields.length; x++) {

			PepFieldDescriptor field = fields[x];
			int inputIndex = x;

			if (field.writeHandle() != null) {

				// (obj, value):void -> obj.setField( value );
				MethodHandle fieldSetter = field.writeHandle();

				// (value[],x):Object -> value[x]
				MethodHandle arrayGetter = MethodHandles.arrayElementGetter(Object[].class);

				// ():int -> inputIndex
				MethodHandle index = MethodHandles.constant(int.class, inputIndex);

				// (value[]):Object -> value[inputIndex]
				MethodHandle arrayIndexGetter = MethodHandles.collectArguments(arrayGetter, 1, index);

				// (obj, value[]):void -> obj.setField( value[inputIndex] );
				MethodHandle arrayFieldSetter = MethodHandles.collectArguments(fieldSetter, 1, arrayIndexGetter);

				// add to list of setters.
				result = MethodHandles.foldArguments(result, arrayFieldSetter);
			}
		}

		// (Object[],serialClass):serialClass -> return serialClass;
		result = MethodHandles.permuteArguments(result, MethodType.methodType(embedClass, Object[].class, embedClass),
				1);
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
	 */
	private MethodHandle createProjectFunction(PepFieldDescriptor[] fields, MethodHandle project) {

		// (int):Object[] -> new Object[int]
		MethodHandle createArray = MethodHandles.arrayConstructor(Object[].class);

		// (int):length -> fields.length
		MethodHandle index = MethodHandles.constant(int.class, fields.length);

		// ():Object[] -> new Object[fields.length]
		MethodHandle arrayCreate = MethodHandles.collectArguments(createArray, 0, index);

		// (Object[],serialClass):void -> getters(Object[],serialClass)
		MethodHandle getters = createProjectGetters(fields);

		// (Object[],targetClass):void -> getters(Object[], project(targetClass))
		MethodHandle projectGetters = MethodHandles.collectArguments(getters, 1, project);

		// ():Object[] -> return new Object[fields.length];
		MethodHandle returnArray = MethodHandles.collectArguments(projectGetters, 0, arrayCreate);

		return returnArray;
	}

	/**
	 * Create the getters
	 * 
	 * @param fields
	 * @return
	 */
	private MethodHandle createProjectGetters(PepFieldDescriptor[] fields) {

		// (object[]):object[] -> return object[];
		MethodHandle identity = MethodHandles.identity(Object[].class);

		// (Object[], embedClass):object[] -> return object[];
		MethodHandle result = MethodHandles.dropArguments(identity, 1, embedClass);

		for (int x = 0; x < fields.length; x++) {

			PepFieldDescriptor field = fields[x];
			int outputIndex = x;

			// (value[],x, v) -> value[x] = v
			MethodHandle arraySetter = MethodHandles.arrayElementSetter(Object[].class);

			// () -> outputIndex
			MethodHandle index = MethodHandles.constant(int.class, outputIndex);

			// (value[],v) -> value[inputIndex] = v
			MethodHandle arrayIndexSetter = MethodHandles.collectArguments(arraySetter, 1, index);

			// (object) -> (Object) object.getter()
			MethodHandle fieldBox = field.readHandle().asType(MethodType.methodType(Object.class, embedClass));

			// (value[],object) -> value[inputIndex] = object.getter()
			MethodHandle arrayValueSetter = MethodHandles.collectArguments(arrayIndexSetter, 1, fieldBox);

			// add to list of getters.
			result = MethodHandles.foldArguments(result, arrayValueSetter);

		}

		// (object[]):object[] -> ... callGetters(object[]); ... return object[];
		return result;
	}

}
