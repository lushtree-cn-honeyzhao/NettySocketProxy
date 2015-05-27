package com.ccompass.netty.proxy;
public class ObjectClone implements Cloneable{
	
	public ObjectClone clone() {
		ObjectClone o = null;
		try {
			o = (ObjectClone) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return o;
	}
}
