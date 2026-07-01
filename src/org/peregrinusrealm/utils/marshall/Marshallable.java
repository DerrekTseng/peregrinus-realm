package org.peregrinusrealm.utils.marshall;

public interface Marshallable {

	public byte[] marshall();

	public void unmarshall(byte[] data);

}
