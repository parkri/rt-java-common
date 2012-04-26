package com.redcliffetechnology.common.util.tokenizer;

import com.redcliffetechnology.common.util.Tokenized;

public final class VolatileComparableTokenized<T> implements Tokenized<T>, Comparable<VolatileComparableTokenized<T>> {

	private static final class Token {
		public volatile long value;
		public volatile int version;

		public Token(long tokenValue, int tokenSpaceVersion) {
			this.value=tokenValue;
			this.version=tokenSpaceVersion;
		}
	}

	private final Token token;

	private final T object;

	//final ReentrantLock lock=new ReentrantLock();

	void setToken(long tokenValue, int tokenVersion) {
		final int localVersion=(tokenVersion<<1)+1;
		token.version=localVersion;
		token.value=tokenValue;
		token.version=localVersion+1;
	}

	VolatileComparableTokenized(T o, long tokenValue, int tokenVersion) {
		token=new Token(tokenValue,(tokenVersion<<1)+2);
		this.object=o;
	}

	VolatileComparableTokenized(T o, VolatileComparableTokenized<T> tokenized) {
		this.token=tokenized.token;
		this.object=o;
	}

	public T getObject() {
		return object;
	}

	public String getDescription() {
		final int beforeVersion=token.version;
		final long localValue=token.value;
		final int afterVersion=token.version;
		return object.toString()+":"+localValue+".(vb="+beforeVersion+",va="+afterVersion+")";				
	}

	public String toString() {
		return object.toString();
	}

	long getVersion() {
		return token.version;
	}

	long getValue() {
		return token.value;
	}

	public final int compareTo(final VolatileComparableTokenized<T> other) {
		if(other.token==this.token) {
			return 0;
		}
		do {
			final int thisBeforeVersion=this.token.version;
			final long thisValue=this.token.value;
			final int thisAfterVersion=this.token.version;
			final int otherBeforeVersion=other.token.version;
			final long otherValue=other.token.value;
			final int otherAfterVersion=other.token.version;
			if((thisAfterVersion&1)==0 && thisAfterVersion==otherAfterVersion && thisBeforeVersion==thisAfterVersion && otherBeforeVersion==otherAfterVersion) {
				final long diff=thisValue-otherValue;
				return (int) ((diff >> 63) | (-diff >>> 63));	
			}
		} while(true);
	}

	@Override
	public final int hashCode() {
		return token.hashCode();
	}

	@Override
	public final boolean equals(Tokenized<T> o) {
		if(o==this) {
			return true;
		}
		return false;
	}
	
	@Override
	public final boolean equals(Object o) {
		if(o==this) {
			return true;
		}
		if(o instanceof Tokenized<?>) {
			return equals((Tokenized<?>)o);
		}
		return false;
	}
}