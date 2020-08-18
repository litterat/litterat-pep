package io.litterat.pep;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import io.litterat.pep.describe.ByteCodeDescriber;

public class PepContext {

	// Resolved class information
	private final ConcurrentHashMap<Class<?>, PepDataClass> descriptors = new ConcurrentHashMap<>();

	// Resolver
	private final PepContextResolver resolver;

	public static class Builder {

		PepContextResolver resolver;

		public Builder() {
			this.resolver = null;
		}

		Builder resolver(PepContextResolver resolver) {
			this.resolver = resolver;
			return this;
		}

		PepContext build() {
			return new PepContext(this);
		}
	}

	private PepContext(Builder builder) {

		if (builder.resolver != null) {
			this.resolver = builder.resolver;
		} else {
			this.resolver = new DefaultResolver(this);
		}

		try {
			registerAtom(Boolean.class);
			registerAtom(boolean.class);
			registerAtom(Character.class);
			registerAtom(char.class);
			registerAtom(Byte.class);
			registerAtom(byte.class);
			registerAtom(Short.class);
			registerAtom(short.class);
			registerAtom(Integer.class);
			registerAtom(int.class);
			registerAtom(Long.class);
			registerAtom(long.class);
			registerAtom(Float.class);
			registerAtom(float.class);
			registerAtom(Double.class);
			registerAtom(double.class);
			registerAtom(Void.class);
			registerAtom(void.class);
		} catch (PepException e) {
			throw new IllegalArgumentException();
		}

	}

	public void registerAtom(Class<?> targetClass) throws PepException {
		register(targetClass, new PepDataClass(targetClass));
	}

	public PepDataClass getDescriptor(Class<?> targetClass) throws PepException {

		PepDataClass descriptor = descriptors.get(targetClass);
		if (descriptor == null) {
			descriptor = resolver.resolve(targetClass);
			if (descriptor == null) {
				throw new PepException(
						String.format("Unable to find suitable data descriptor for class: %s", targetClass.getName()));
			}
			descriptors.put(targetClass, descriptor);
		}

		return descriptor;
	}

	private <T> void checkExists(Class<T> targetClass) throws PepException {
		if (descriptors.containsKey(targetClass)) {
			throw new PepException(String.format("Class already registered: %s", targetClass.getName()));
		}
	}

	public <T> void register(Class<T> targetClass, PepDataClass descriptor) throws PepException {
		checkExists(targetClass);

		descriptors.put(targetClass, descriptor);
	}

	private static class DefaultResolver implements PepContextResolver {

		private final PepContext context;

		public DefaultResolver(PepContext context) {
			this.context = context;
		}

		@Override
		public PepDataClass resolve(Class<?> targetClass) throws PepException {
			PepDataClass descriptor = null;

			// Unable to describe interfaces, arrays, etc.
			if (targetClass.isInterface() || targetClass.isArray() || targetClass.isAnonymousClass()
					|| targetClass.isEnum() || targetClass.isAnnotation() || targetClass.isPrimitive()
					|| targetClass.isSynthetic()) {
				throw new IllegalArgumentException(
						String.format("Not able to describe class for serialization: %s", targetClass));
			}

			if (ToData.class.isAssignableFrom(targetClass)) {
				for (Type genericInterface : targetClass.getGenericInterfaces()) {
					if (genericInterface instanceof ParameterizedType) {
						Type[] genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
						for (Type genericType : genericTypes) {
							System.out.println("Generic type: " + genericType);

							Class<?> serialClass = (Class<?>) genericType;

							try {
								MethodHandle embed = MethodHandles.lookup()
										.unreflectConstructor(targetClass.getConstructor(serialClass));

								MethodHandle project = MethodHandles.lookup()
										.unreflect(targetClass.getMethod("toData"));

								PepDataClass serialDescriptor = resolve(serialClass);

								MethodHandle constructor = null;
								try {
									constructor = MethodHandles.lookup()
											.unreflectConstructor(serialClass.getConstructor(int.class, int.class));
								} catch (IllegalAccessException | NoSuchMethodException | SecurityException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								descriptor = new PepDataClass(targetClass, serialClass, constructor, project, embed,
										serialDescriptor.dataComponents());
							} catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {

								e.printStackTrace();
							}
						}
					}
				}

			} else {

				ByteCodeDescriber describer = new ByteCodeDescriber(context);

				MethodHandle constructor = null;
				try {
					constructor = MethodHandles.lookup()
							.unreflectConstructor(targetClass.getConstructor(int.class, int.class));
				} catch (IllegalAccessException | NoSuchMethodException | SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				PepDataComponent[] fields = describer.describe(targetClass);

				MethodHandle toObject = MethodHandles.identity(targetClass);
				MethodHandle toData = MethodHandles.identity(Object.class)
						.asType(MethodType.methodType(targetClass, targetClass));

				descriptor = new PepDataClass(targetClass, targetClass, constructor, toData, toObject, fields);

			}

			return descriptor;
		}

	}
}
