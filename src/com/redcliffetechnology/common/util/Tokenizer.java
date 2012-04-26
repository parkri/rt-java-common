package com.redcliffetechnology.common.util;

public interface Tokenizer<T> {
	Tokenized<T> getTokenized(T instance);

}
