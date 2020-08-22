package io.litterat.pep.describe;

import java.lang.invoke.MethodHandle;
import java.util.Collection;

import io.litterat.pep.DataObjectBridge;

/**
 * 
 * Default Collection to array bridge.
 *
 */
public class CollectionBridge implements DataObjectBridge<Object[], Collection<?>> {

	MethodHandle collectionConstructor;

	public CollectionBridge(MethodHandle collectionConstructor) {
		this.collectionConstructor = collectionConstructor;
	}

	@Override
	public Object[] toData(Collection<?> b) {
		return b.toArray();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Collection<?> toObject(Object[] s) {
		try {

			Collection collection = (Collection) collectionConstructor.invoke();

			for (int x = 0; x < s.length; x++) {
				collection.add(s[x]);
			}
		} catch (Throwable e) {
			throw new RuntimeException("Failed to convert to Collection");
		}
		return null;
	}

}
