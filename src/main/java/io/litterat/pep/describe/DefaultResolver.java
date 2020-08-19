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
package io.litterat.pep.describe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.litterat.pep.PepContext;
import io.litterat.pep.PepContextResolver;
import io.litterat.pep.PepDataClass;
import io.litterat.pep.PepDataComponent;
import io.litterat.pep.PepException;
import io.litterat.pep.ToData;

public class DefaultResolver implements PepContextResolver {

	@Override
	public PepDataClass resolve(PepContext context, Class<?> targetClass) throws PepException {
		PepDataClass descriptor = null;

		// Unable to describe interfaces, arrays, etc.
		if (targetClass.isInterface() || targetClass.isArray() || targetClass.isAnonymousClass() || targetClass.isEnum() || targetClass.isAnnotation()
				|| targetClass.isPrimitive() || targetClass.isSynthetic()) {
			throw new IllegalArgumentException(String.format("Not able to describe class for serialization: %s", targetClass));
		}

		if (ToData.class.isAssignableFrom(targetClass)) {
			for (Type genericInterface : targetClass.getGenericInterfaces()) {
				if (genericInterface instanceof ParameterizedType) {
					Type[] genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
					for (Type genericType : genericTypes) {

						Class<?> serialClass = (Class<?>) genericType;

						try {
							MethodHandle embed = MethodHandles.lookup().unreflectConstructor(targetClass.getConstructor(serialClass));

							MethodHandle project = MethodHandles.lookup().unreflect(targetClass.getMethod("toData"));

							PepDataClass serialDescriptor = resolve(context, serialClass);

							MethodHandle constructor = null;
							try {
								constructor = MethodHandles.lookup().unreflectConstructor(serialClass.getConstructor(int.class, int.class));
							} catch (IllegalAccessException | NoSuchMethodException | SecurityException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							descriptor = new PepDataClass(targetClass, serialClass, constructor, project, embed, serialDescriptor.dataComponents());
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
				constructor = MethodHandles.lookup().unreflectConstructor(targetClass.getConstructor(int.class, int.class));
			} catch (IllegalAccessException | NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			PepDataComponent[] fields = describer.describe(targetClass);

			MethodHandle toObject = MethodHandles.identity(targetClass);
			MethodHandle toData = MethodHandles.identity(Object.class).asType(MethodType.methodType(targetClass, targetClass));

			descriptor = new PepDataClass(targetClass, targetClass, constructor, toData, toObject, fields);

		}

		return descriptor;
	}

}