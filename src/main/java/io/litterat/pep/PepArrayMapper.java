package io.litterat.pep;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class PepArrayMapper<T> {

	private final PepClassDescriptor<T> classDescriptor;

	// constructs, calls setters and embeds. Has signature: T embed( Object[] values ).
	private final MethodHandle embedFunction;

	// Converts from Object[] to targetClass. Has signature: Object[] project( T object );
	private final MethodHandle projectFunction;

	public PepArrayMapper(PepClassDescriptor<T> classDescriptor) {
		this.classDescriptor = classDescriptor;
		this.projectFunction = createProjectFunction(classDescriptor.fields(), classDescriptor.toData());
		this.embedFunction = createEmbedFunction(classDescriptor.constructor(), classDescriptor.fields(),
				classDescriptor.toObject());
	}

	/**
	 * Convenience function. Takes the target object of this descriptor and return an object array.
	 * 
	 * @param o target object instance to project to object[]
	 * @return values from target object
	 * @throws Throwable any failure from the project function.
	 */
	public Object[] toArray(T o) throws Throwable {

		// create and fill array with project instance.
		return (Object[]) projectFunction.invoke(o);
	}

	/**
	 * Convenience function. Takes an array of values based on the field types and returns the target
	 * object.
	 * 
	 * @param values object values to embed into target object.
	 * @return recreated target object.
	 * @throws Throwable any failure from the embed function.
	 */
	public T toObject(Object[] values) throws Throwable {

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

		// (Object[]) -> embed( ctor(Object[]).setValues(Object[]) )
		return MethodHandles.collectArguments(embed, 0, create);
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

			int arg = x;
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

		// spread the arguments so ctor(Object[],Object[]...) becomes ctor(Object[])
		int paramCount = objectConstructor.type().parameterCount();
		if (paramCount > 0) {
			int[] permuteInput = new int[paramCount];
			result = MethodHandles.permuteArguments(result,
					MethodType.methodType(classDescriptor.dataClass(), Object[].class), permuteInput);
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
		MethodHandle result = MethodHandles.dropArguments(identity, 1, classDescriptor.dataClass());

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
			MethodHandle fieldBox = field.accessor()
					.asType(MethodType.methodType(Object.class, classDescriptor.dataClass()));

			// (value[],object) -> value[inputIndex] = object.getter()
			MethodHandle arrayValueSetter = MethodHandles.collectArguments(arrayIndexSetter, 1, fieldBox);

			// add to list of getters.
			result = MethodHandles.foldArguments(result, arrayValueSetter);

		}

		// (object[]):object[] -> ... callGetters(object[]); ... return object[];
		return result;
	}
}
