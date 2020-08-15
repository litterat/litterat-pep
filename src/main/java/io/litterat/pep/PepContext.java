package io.litterat.pep;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import io.litterat.pep.describe.ByteCodeDescriber;

public class PepContext {

	/**
	 * 
	 */
	private final ConcurrentHashMap<Class<?>, PepClassDescriptor<?>> descriptors = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <T> PepClassDescriptor<T> getDescriptor(Class<T> targetClass) throws PepException {

		PepClassDescriptor<?> descriptor = descriptors.get(targetClass);
		if (descriptor == null) {
			descriptor = createDescriptor(targetClass);
			descriptors.put(targetClass, descriptor);
		}

		return (PepClassDescriptor<T>) descriptor;
	}

	@SuppressWarnings("unchecked")
	public <T> PepClassDescriptor<T> getDescriptor(Class<T> targetClass, ProjectEmbedPair<T, ?> pePair)
			throws PepException {

		PepClassDescriptor<?> descriptor = descriptors.get(targetClass);
		if (descriptor == null) {
			descriptor = createDescriptor(targetClass);
			descriptors.put(targetClass, descriptor);
		}

		return (PepClassDescriptor<T>) descriptor;
	}

	@SuppressWarnings("unchecked")
	public <T> PepClassDescriptor<T> getDescriptor(Class<T> targetClass, Embeds<T> embeds) throws PepException {

		PepClassDescriptor<?> descriptor = descriptors.get(targetClass);
		if (descriptor == null) {
			descriptor = createDescriptor(targetClass);
			descriptors.put(targetClass, descriptor);
		}

		return (PepClassDescriptor<T>) descriptor;
	}

	private <T> PepClassDescriptor<T> createDescriptor(Class<T> targetClass) throws PepException {
		PepClassDescriptor<T> descriptor = null;

		// Unable to describe interfaces, arrays, etc.
		if (targetClass.isInterface() || targetClass.isArray() || targetClass.isAnonymousClass() || targetClass.isEnum()
				|| targetClass.isAnnotation() || targetClass.isPrimitive() || targetClass.isSynthetic()) {
			throw new IllegalArgumentException("Not able to describe class for serialization");
		}

		if (Projects.class.isAssignableFrom(targetClass)) {
			for (Type genericInterface : targetClass.getGenericInterfaces()) {
				if (genericInterface instanceof ParameterizedType) {
					Type[] genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
					for (Type genericType : genericTypes) {
						System.out.println("Generic type: " + genericType);

						Class<?> serialClass = (Class<?>) genericType;

						try {
							MethodHandle embed = MethodHandles.lookup()
									.unreflectConstructor(targetClass.getConstructor(serialClass));

							MethodHandle project = MethodHandles.lookup().unreflect(targetClass.getMethod("project"));

							@SuppressWarnings("unchecked")
							PepClassDescriptor<T> serialDescriptor = (PepClassDescriptor<T>) createDescriptor(
									serialClass);

							MethodHandle constructor = null;
							try {
								constructor = MethodHandles.lookup()
										.unreflectConstructor(serialClass.getConstructor(int.class, int.class));
							} catch (IllegalAccessException | NoSuchMethodException | SecurityException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							descriptor = new PepClassDescriptor<T>(targetClass, serialClass, constructor, project,
									embed, serialDescriptor.fields());
						} catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {

							e.printStackTrace();
						}
					}
				}
			}

		} else {

			ByteCodeDescriber describer = new ByteCodeDescriber();

			MethodHandle constructor = null;
			try {
				constructor = MethodHandles.lookup()
						.unreflectConstructor(targetClass.getConstructor(int.class, int.class));
			} catch (IllegalAccessException | NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			PepFieldDescriptor[] fields = describer.describe(targetClass);

			MethodHandle embed = MethodHandles.identity(targetClass);
			// MethodHandle embedEmpty =
			// MethodHandles.empty(MethodType.methodType(targetClass, targetClass));
			// MethodHandle embed = MethodHandles.collectArguments(embedEmpty, 0, ident);
			MethodHandle project = MethodHandles.identity(Object.class)
					.asType(MethodType.methodType(targetClass, targetClass));

			descriptor = new PepClassDescriptor<T>(targetClass, targetClass, constructor, project, embed, fields);
			// ByteCodeDescriptor
		}

		return descriptor;
	}
}
