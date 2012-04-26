package com.redcliffetechnology.common.util;

public interface Tokenized<T> {
	
	T getObject();
	
	int hashCode() ;
	
	boolean equals(Tokenized<T> other);

	boolean equals(Object o);

}
