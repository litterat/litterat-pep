package io.litterat.pep.describe;

import java.lang.reflect.Method;

public class ComponentInfo {

	private String name;

	private final Class<?> type;

	private Method writeMethod;

	private Method readMethod;

	private int constructorArgument;

	public ComponentInfo(String name, Class<?> type) {
		this.name = name;
		this.type = type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Class<?> getType() {
		return type;
	}

	public Method getWriteMethod() {
		return writeMethod;
	}

	public void setWriteMethod(Method writeMethod) {
		this.writeMethod = writeMethod;
	}

	public Method getReadMethod() {
		return readMethod;
	}

	public void setReadMethod(Method readMethod) {
		this.readMethod = readMethod;
	}

	public int getConstructorArgument() {
		return constructorArgument;
	}

	public void setConstructorArgument(int constructorArgument) {
		this.constructorArgument = constructorArgument;
	}
}
