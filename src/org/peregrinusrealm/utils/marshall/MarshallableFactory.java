package org.peregrinusrealm.utils.marshall;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class MarshallableFactory {

	private final Map<Long, Constructor<? extends Marshallable>> mapper = new HashMap<>();

	public void register(Class<? extends Marshallable> type) {
		try {
			long id = generateClassNameId(type.getName());
			if (mapper.containsKey(id)) {
				Class<?> existingType = mapper.get(id).getDeclaringClass();
				if (!existingType.equals(type)) {
					throw new IllegalStateException(String.format("Hash collision detected! ID %d (0x%016X) is shared by [%s] and [%s]", id, id, existingType.getName(), type.getName()));
				}
			}
			mapper.put(id, type.getConstructor());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private long generateClassNameId(String className) {
		long hash = 0xcbf29ce484222325L; // FNV_offset_basis
		for (int i = 0; i < className.length(); i++) {
			hash ^= className.charAt(i);
			hash *= 0x100000001b3L; // FNV_prime
		}
		return hash;
	}

	public Marshallable unmarshall(byte[] data) {
		try {
			MarshallDestructor destructor = new MarshallDestructor(data);
			long id = destructor.getLong();
			if (!mapper.containsKey(id)) {
				throw new IllegalStateException(String.format("Unknown marshallable type ID: %d (0x%016X). Please ensure it is registered.", id, id));
			}
			Marshallable marshallable = mapper.get(id).newInstance();
			marshallable.unmarshall(destructor);
			return marshallable;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] marshall(Marshallable marshallable) {
		try {
			MarshallBuilder builder = MarshallBuilder.newInstance();
			String className = marshallable.getClass().getName();
			long id = generateClassNameId(className);
			if (!mapper.containsKey(id)) {
				throw new IllegalStateException(String.format("Class [%s] has not been registered in MarshallableFactory.", className));
			}
			builder.add(id);
			marshallable.marshall(builder);
			return builder.build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}