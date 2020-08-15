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

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import io.litterat.pep.PepException;
import io.litterat.pep.PepFieldDescriptor;

public class ByteCodeDescriber implements ClassDescriber {

	// Temporary store for fields information.
	private class FieldInfo {
		String name;
		Class<?> type;
		boolean isOptional;
		Method accessor;
		int arg;

		@Override
		public String toString() {
			return "field: " + name + " type:" + type.getName() + " isOptional: " + isOptional + " arg: " + arg;
		}
	}

	@Override
	public PepFieldDescriptor[] describe(Class<?> clss) throws PepException {

		FieldInfo[] fields = new FieldInfo[clss.getDeclaredFields().length];

		Map<String, FieldInfo> fieldMap = createFieldMap(clss, fields);

		try {
			ClassReader cr = new ClassReader(clss.getName());
			ClassNode classNode = new ClassNode();
			cr.accept(classNode, 0);

			// Perform node instruction inspection to match constructor arguments with accessors.
			for (MethodNode method : classNode.methods) {

				Type methodType = Type.getType(method.desc);

				if (method.name.equals("<init>")) {

					// TODO identify if this is the right constructor.
					identifyArguments(fieldMap, method, methodType);

				} else {

					// Only interested in accessors.
					if (methodType.getArgumentTypes().length != 0) {
						continue;
					}

					// Returns the field name that this accessor is using if it is a simple accessor type.
					String field = examineAccessor(clss, fieldMap, method);
					if (field != null) {
						FieldInfo info = fieldMap.get(field);
						info.accessor = clss.getDeclaredMethod(method.name);
					}
				}
			}

			// Prepare the field descriptors.
			// TODO deal with fields that were not found.
			PepFieldDescriptor[] fieldDescriptors = new PepFieldDescriptor[fields.length];
			for (int x = 0; x < fields.length; x++) {
				FieldInfo info = fields[x];

				MethodHandle accessor = MethodHandles.lookup().unreflect(info.accessor);

				fieldDescriptors[x] = new PepFieldDescriptor(info.name, info.type, info.isOptional, accessor, null,
						info.arg);
			}

			return fieldDescriptors;
		} catch (IOException | NoSuchMethodException | SecurityException | IllegalAccessException e) {

			throw new PepException("Failed to access class", e);
		}
	}

	/**
	 * Find all possible fields for this class.
	 * 
	 * @param clss
	 * @param fields
	 * @return
	 */
	private Map<String, FieldInfo> createFieldMap(Class<?> clss, FieldInfo[] fields) {
		Map<String, FieldInfo> fieldMap = new HashMap<>();

		Field[] declaredFields = clss.getDeclaredFields();
		for (int x = 0; x < declaredFields.length; x++) {
			Field f = declaredFields[x];
			fields[x] = new FieldInfo();
			fields[x].name = f.getName();
			fields[x].type = f.getType();
			if (f.getType().isPrimitive()) {
				fields[x].isOptional = false;
			} else {
				fields[x].isOptional = false;
			}

			fieldMap.put(f.getName(), fields[x]);

		}
		return fieldMap;
	}

	/**
	 * Find all the
	 * 
	 * @param fieldMap
	 * @param method
	 */
	private void identifyArguments(Map<String, FieldInfo> fieldMap, MethodNode method, Type methodType) {
		boolean foundLoadThis = false;
		boolean foundLoadArg = false;
		int arg = 0;

		ListIterator<AbstractInsnNode> it = method.instructions.iterator();
		while (it.hasNext()) {
			AbstractInsnNode insn = it.next();

			switch (insn.getOpcode()) {
			case Opcodes.ALOAD:
				VarInsnNode varInsn = (VarInsnNode) insn;
				if (!foundLoadThis) {
					if (varInsn.var == 0) {
						foundLoadThis = true;
					}
					break;
				}
			case Opcodes.ILOAD:
			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
			case Opcodes.FLOAD:
				VarInsnNode varLoadInsn = (VarInsnNode) insn;

				// Check if this is being loaded from a parameter variable.
				if (foundLoadThis & varLoadInsn.var > 0 && varLoadInsn.var <= methodType.getArgumentTypes().length) {
					foundLoadArg = true;
					arg = varLoadInsn.var - 1;
				}
				break;
			case Opcodes.PUTFIELD:
				FieldInsnNode putFieldInsn = (FieldInsnNode) insn;
				if (foundLoadThis && foundLoadArg) {

					// Invariance identified, so capture the argument number.
					FieldInfo info = fieldMap.get(putFieldInsn.name);
					info.arg = arg;

				}
			default:
				foundLoadThis = false;
				foundLoadArg = false;
			}

		}
	}

	/**
	 * Look through the instructions looking for ALOAD, GETFIELD, RETURN combination.
	 * 
	 * @param clss
	 * @param fieldMap
	 * @param method
	 * @return field name
	 * @throws NoSuchMethodException
	 */
	private String examineAccessor(Class<?> clss, Map<String, FieldInfo> fieldMap, MethodNode method)
			throws NoSuchMethodException {
		boolean foundLoadThis = false;
		String lastField = null;

		ListIterator<AbstractInsnNode> it = method.instructions.iterator();
		while (it.hasNext()) {

			AbstractInsnNode insn = it.next();

			switch (insn.getOpcode()) {
			case Opcodes.ALOAD:
				VarInsnNode varInsn = (VarInsnNode) insn;
				if (varInsn.var == 0) {
					foundLoadThis = true;
				}
				break;
			case Opcodes.GETFIELD:
				FieldInsnNode fieldInsn = (FieldInsnNode) insn;
				if (foundLoadThis) {
					lastField = fieldInsn.name;
				}
				break;
			case Opcodes.IRETURN:
			case Opcodes.ARETURN:
			case Opcodes.LRETURN:
			case Opcodes.DRETURN:
			case Opcodes.RETURN:
			case Opcodes.FRETURN:
				if (foundLoadThis && lastField != null) {
					// accessor matches, break;
					break;
				}
			case -1:
				// ignore these opcodes.
				break;
			default:
				foundLoadThis = false;
				lastField = null;
			}
		}

		return lastField;
	}

}
