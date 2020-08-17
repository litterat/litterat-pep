package io.litterat.pep;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

public class PepMapMapper<T> {

	private final PepClassDescriptor<T> classDescriptor;

	private final MethodHandle constructor;

	public PepMapMapper(PepClassDescriptor<T> classDescriptor) {
		this.classDescriptor = classDescriptor;

		// Convert constructor to take Object[]
		this.constructor = classDescriptor.constructor().asSpreader(Object[].class, classDescriptor.fields().length);
	}

	public Map<String, Object> toMap(Object object) throws Throwable {
		Object data = classDescriptor.toData().invoke(object);

		Map<String, Object> map = new HashMap<>();

		PepFieldDescriptor[] fields = classDescriptor.fields();
		for (int x = 0; x < classDescriptor.fields().length; x++) {
			PepFieldDescriptor field = fields[x];

			map.put(field.name(), field.accessor().invoke(data));
		}
		return map;
	}

	public Object toObject(Map<String, Object> map) throws Throwable {

		PepFieldDescriptor[] fields = classDescriptor.fields();
		Object[] construct = new Object[fields.length];
		for (int x = 0; x < classDescriptor.fields().length; x++) {
			PepFieldDescriptor field = fields[x];

			construct[x] = map.get(field.name());
		}

		Object data = constructor.invoke(construct);

		return classDescriptor.toObject().invoke(data);

	}
}
