package io.litterat.pep;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PepMapMapper {

	private final PepContext context;

	// private final MethodHandle constructor;

	public PepMapMapper(PepContext context) {
		this.context = context;

	}

	public Map<String, Object> toMap(Object object) throws Throwable {

		Objects.requireNonNull(object);

		PepDataClass dataClass = context.getDescriptor(object.getClass());

		Object data = dataClass.toData().invoke(object);

		Map<String, Object> map = new HashMap<>();

		PepDataComponent[] fields = dataClass.dataComponents();
		for (int x = 0; x < dataClass.dataComponents().length; x++) {
			PepDataComponent field = fields[x];

			Object v = field.accessor().invoke(data);
			// TODO need to check correctly if this should be recursive.
			// if (v != null && !field.getClass().isPrimitive()) {
			// v = toMap(v);
			// }
			map.put(field.name(), v);
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	public Object toObject(Class<?> clss, Map<String, Object> map) throws Throwable {

		Objects.requireNonNull(clss);
		Objects.requireNonNull(map);

		PepDataClass dataClass = context.getDescriptor(clss);

		PepDataComponent[] fields = dataClass.dataComponents();
		Object[] construct = new Object[fields.length];
		for (int x = 0; x < dataClass.dataComponents().length; x++) {
			PepDataComponent field = fields[x];

			Object v = map.get(field.name());

			// TODO correctly check field informatoin instead.
			if (v instanceof Map) {
				v = toObject(field.getClass(), (Map<String, Object>) v);
			}
			construct[x] = v;
		}

		// Convert constructor to take Object[]
		MethodHandle constructor = dataClass.constructor().asSpreader(Object[].class,
				dataClass.dataComponents().length);

		Object data = constructor.invoke(construct);

		return dataClass.toObject().invoke(data);

	}
}
