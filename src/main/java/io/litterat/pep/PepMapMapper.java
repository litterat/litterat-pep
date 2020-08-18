package io.litterat.pep;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

public class PepMapMapper<T> {

	private final PepDataClass classDescriptor;

	private final MethodHandle constructor;

	public PepMapMapper(PepDataClass classDescriptor) {
		this.classDescriptor = classDescriptor;

		// Convert constructor to take Object[]
		this.constructor = classDescriptor.constructor().asSpreader(Object[].class,
				classDescriptor.dataComponents().length);
	}

	public Map<String, Object> toMap(Object object) throws Throwable {
		Object data = classDescriptor.toData().invoke(object);

		Map<String, Object> map = new HashMap<>();

		PepDataComponent[] fields = classDescriptor.dataComponents();
		for (int x = 0; x < classDescriptor.dataComponents().length; x++) {
			PepDataComponent field = fields[x];

			map.put(field.name(), field.accessor().invoke(data));
		}
		return map;
	}

	public Object toObject(Map<String, Object> map) throws Throwable {

		PepDataComponent[] fields = classDescriptor.dataComponents();
		Object[] construct = new Object[fields.length];
		for (int x = 0; x < classDescriptor.dataComponents().length; x++) {
			PepDataComponent field = fields[x];

			construct[x] = map.get(field.name());
		}

		Object data = constructor.invoke(construct);

		return classDescriptor.toObject().invoke(data);

	}
}
