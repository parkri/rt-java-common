package com.redcliffetechnology.common.util;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;



public class Tokenizer<T> {

	private static final long RESIZE_FACTOR=0x10000;
	private static final long MIN=Long.MIN_VALUE/2;
	private static final long MAX=Long.MAX_VALUE/2;
	private static final long ADD=0;

	private final boolean retainAll;
	private final ConcurrentHashMap<T, Tokenized<T>> readTokenSpace;
	private final NavigableMap<T, Tokenized<T>> tokenSpace;
	private int tokenSpaceVersion;

	public Tokenizer(Comparator<? super T> comparator) {
		this(comparator,false);
	}

	public Tokenizer(Comparator<? super T> comparator, boolean retainAll) {
		tokenSpaceVersion=0;
		tokenSpace=(comparator==null)?new TreeMap<T,Tokenized<T>>():new TreeMap<T,Tokenized<T>>(comparator);
		//tokenSpace=(comparator==null)?new ConcurrentSkipListMap<T,Tokenized<T>>():new ConcurrentSkipListMap<T,Tokenized<T>>(comparator);
		readTokenSpace=new ConcurrentHashMap<T,Tokenized<T>>();
		this.retainAll=retainAll;
	}

	public Tokenizer() {
		this(null,false);
	}

	public Tokenizer(boolean retainAll) {
		this(null,false);
	}

	public synchronized int getNumberOfRedistributions() {
		return tokenSpaceVersion;
	}

	public long getNumberOfTokens() {
		return tokenSpace.size();
	}

	public Tokenized<T> getTokenized(T instance) {
		Tokenized<T> stub=readTokenSpace.get(instance);
		if(stub==null) {
			stub=tokenize(instance);
		}
		return stub;
	}

	private final void redistribute() {
		//final Comparator<? super T> comparator=tokenSpace.comparator();
		final long size=tokenSpace.size();
		long newValue=MIN/RESIZE_FACTOR;
		final long incrementValue=MAX/(size*(RESIZE_FACTOR/2));
		if(incrementValue==0) {
			throw new RuntimeException("Unable to redistribute");
		}
		final int newVersion=++tokenSpaceVersion;	
		//T oldObject=null;
		for(Tokenized<T> stub: tokenSpace.values()) {
			stub.setToken(newValue,newVersion);
			newValue+=incrementValue;
			/*
			if(oldObject!=null && 
					((comparator==null && ((Comparable<? super T>)oldObject).compareTo(stub.getObject())>=0) || (comparator!=null && comparator.compare(oldObject, stub.getObject())>=0))) {
				System.out.println("Redist out of order.  "+oldObject+" not before "+stub);				
			}
			oldObject=stub.getObject();
			 */
		}
		//System.out.println("End diff="+((newValue)-(MAX/RESIZE_FACTOR)));
	}


	private final synchronized Tokenized<T> tokenize(T instance) {
		//Tokenized<T> stub=tokenSpace.put(instance, ourStub);

		// Always true when we synchronize

		// Go ahead and do the work of figuring out the token value
		final Map.Entry<T, Tokenized<T>> beforeEntry=tokenSpace.floorEntry(instance);
		final Tokenized<T> before=(beforeEntry==null)?null:beforeEntry.getValue();
		if(before!=null && compare(before.getObject(),instance)==0) {
			// Same
			if(retainAll) {
				final Tokenized<T> stub=new Tokenized<T>(instance, before);
				readTokenSpace.put(instance, stub);
				return stub;
			}
			else {
				return before;
			}
		}
		final Map.Entry<T, Tokenized<T>> afterEntry=tokenSpace.ceilingEntry(instance);
		final Tokenized<T> after=(afterEntry==null)?null:afterEntry.getValue();
		if(after!=null && compare(after.getObject(),instance)==0) {
			// Same
			if(retainAll) {
				final Tokenized<T> stub=new Tokenized<T>(instance, after);
				readTokenSpace.put(instance, stub);
				return stub;
			}
			else {
				return after;
			}
		}
		// before and after will always be valid if not null
		// because synchronized
		long value=0;
		if(before==null) {
			if(after==null) {
				// before==null && after==null
				// value=0;
			}
			else {
				// before==null && after!=null
				do {
					final long afterValue=after.getValue()-ADD;
					value=((MIN-afterValue)/RESIZE_FACTOR);
					if(value>=0) {
						// Out of capacity
						//System.out.println("Redist start instance="+instance+" value="+value+" after="+after);
						redistribute();
					}
					else {
						value+=afterValue;
						break;
					}
				} while(true);
			}
		}
		else {
			if(after==null) {
				// before!=null && after==null
				do {
					final long beforeValue=before.getValue()-ADD;
					value=((MAX-beforeValue)/RESIZE_FACTOR);
					if(value<=0) {
						// Out of capacity
						//System.out.println("Redist end instance="+instance+" value="+value+" before="+before);
						redistribute();
					}
					else {
						value+=beforeValue;
						break;
					}
				} while(true);
			}
			else {
				// before!=null && after!=null
				do {
					final long beforeValue=before.getValue()-ADD;
					final long afterValue=after.getValue()-ADD;
					value=(afterValue-beforeValue)/2;
					if(value<=0) {
						// Out of capacity
						//System.out.println("Redist middle instance="+instance+" value="+value+" before="+before+" after="+after);
						redistribute();
					}
					else {
						value+=beforeValue;
						break;
					}
				} while(true);
			}
		}
		final Tokenized<T> stub=new Tokenized<T>(instance,value+ADD,tokenSpaceVersion);
		tokenSpace.put(instance, stub);
		readTokenSpace.put(instance, stub);
		return stub;

	}

	/*
	private final synchronized Tokenized<T> tokenize(T instance) {
		final Tokenized<T> ourStub=new Tokenized<T>(instance,-1,-1);
		Tokenized<T> stub=tokenSpace.put(instance, ourStub);

		// Always true when we synchronize
		if(stub==null) {
			stub=ourStub;
			// Go ahead and do the work of figuring out the token value
			final Map.Entry<T, Tokenized<T>> beforeEntry=tokenSpace.lowerEntry(instance);
			final Tokenized<T> before=(beforeEntry==null)?null:beforeEntry.getValue();
			final Map.Entry<T, Tokenized<T>> afterEntry=tokenSpace.higherEntry(instance);
			final Tokenized<T> after=(afterEntry==null)?null:afterEntry.getValue();

			// before and after will always be valid if not null
			// because synchronized
			long value=0;
			if(before==null) {
				if(after==null) {
					// before==null && after==null
					// value=0;
				}
				else {
					// before==null && after!=null
					do {
						final long afterValue=after.getValue()-ADD;
						value=((MIN-afterValue)/RESIZE_FACTOR);
						if(value>=0) {
							// Out of capacity
							//System.out.println("Redist start instance="+instance+" value="+value+" after="+after);
							redistribute();
						}
						else {
							value+=afterValue;
							break;
						}
					} while(true);
				}
			}
			else {
				if(after==null) {
					// before!=null && after==null
					do {
						final long beforeValue=before.getValue()-ADD;
						value=((MAX-beforeValue)/RESIZE_FACTOR);
						if(value<=0) {
							// Out of capacity
							//System.out.println("Redist end instance="+instance+" value="+value+" before="+before);
							redistribute();
						}
						else {
							value+=beforeValue;
							break;
						}
					} while(true);
				}
				else {
					// before!=null && after!=null
					do {
						final long beforeValue=before.getValue()-ADD;
						final long afterValue=after.getValue()-ADD;
						value=(afterValue-beforeValue)/2;
						if(value<=0) {
							// Out of capacity
							//System.out.println("Redist middle instance="+instance+" value="+value+" before="+before+" after="+after);
							redistribute();
						}
						else {
							value+=beforeValue;
							break;
						}
					} while(true);
				}
			}
			stub.setToken(value+ADD,tokenSpaceVersion);
			readTokenSpace.put(instance, stub);
		}
		else {
			// Wait for token space version to be >= 0
			// to indicate the other thread has finished building the Stub
			//while(stub.getVersion()<0);
			tokenSpace.put(instance,stub);
			if(retainAll) {
				readTokenSpace.put(instance, new Tokenized<T>(instance,stub));
			}
		}
		return stub;
	}
	 */

	private int compare(T a, T b) {
		if(tokenSpace.comparator()!=null) {
			return tokenSpace.comparator().compare(a, b);
		}
		return 0;
	}



}
