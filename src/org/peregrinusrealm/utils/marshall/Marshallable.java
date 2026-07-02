package org.peregrinusrealm.utils.marshall;

public interface Marshallable {
	
	public void marshall(MarshallBuilder builder);

	public void unmarshall(MarshallDestructor destructor);

}
