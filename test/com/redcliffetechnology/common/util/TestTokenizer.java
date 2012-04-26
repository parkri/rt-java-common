package com.redcliffetechnology.common.util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import com.redcliffetechnology.common.util.Tokenized;
import com.redcliffetechnology.common.util.Tokenizer;



public class TestTokenizer implements Runnable {

	private static int MAX_THREADS=8;

	private static final String PREFIX="0000000000";

	private static final long ITERATIONS=500000;

	private static final long LOOPS=4;

	private static final Random random=new Random();

	private static Tokenizer<String> tokenizer;
	private static ArrayList<String> strings;
	private static int[][] randoms;

	private final int[] indexes;
	private final boolean rand;
	private long iterationsPerSecond;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean rand=false;
		randoms=new int[MAX_THREADS][(int)ITERATIONS];
		for(int th=1;th<=MAX_THREADS;th++) {
			for(int i=0;i<ITERATIONS;i++) {
				randoms[th-1][i]=random.nextInt((int)ITERATIONS);
			}
		}		
		strings=new ArrayList<String>((int)ITERATIONS);

		for(int i=0;i<ITERATIONS;i++) {
			String value=""+i;
			value=PREFIX.substring(value.length()-1)+value;
			strings.add(value);
		}
		System.out.println("************** SETUP TESTS");
		for(int th=1;th<=MAX_THREADS;th*=2) {
			System.out.println("************** THREADS="+th);
			for(int randIter=0;randIter<2;randIter++) {
				if(randIter==1) rand=true;
				else rand=false;
				tokenizer=new Tokenizer<String>(String.CASE_INSENSITIVE_ORDER,true);

				TestTokenizer[] tests=new TestTokenizer[th];
				Thread[] threads=new Thread[th];
				for(int loop=0;loop<LOOPS;loop++) {
					for(int t=0;t<th;t++) {
						tests[t]=new TestTokenizer(rand,randoms[th-1]);
						threads[t]=new Thread(tests[t]);
					}
					System.out.println(Thread.currentThread().getName()+" Loop "+(loop+1)+(rand?" RANDOM":" NOT random"));
					for(int t=0;t<th;t++) {
						threads[t].start();
					}
					long iterationsPerSecond=0;
					for(int t=0;t<th;t++) {
						try {
							threads[t].join();
							iterationsPerSecond+=tests[t].getIterationsPerSecond();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					System.out.println(Thread.currentThread().getName()+" Iteration per second="+iterationsPerSecond);
					System.out.println(Thread.currentThread().getName()+" Redistributions="+tokenizer.getNumberOfRedistributions()+" Size="+tokenizer.getNumberOfTokens());
				}
			}
			System.out.println("************** SORTING TESTS");
			// Build random list of String and Tokenized<String>
			Tokenized<?>[] tokenized=new Tokenized<?>[(int)ITERATIONS];
			String[] string=new String[(int)ITERATIONS];
			for(int i=0;i<ITERATIONS;i++) {
				string[i]=strings.get(randoms[0][i]);
				tokenized[i]=tokenizer.getTokenized(string[i]);
			}
			System.out.println(Thread.currentThread().getName()+" Sort strings");
			long start=System.nanoTime();
			Arrays.sort(string,String.CASE_INSENSITIVE_ORDER);
			long elapsed=System.nanoTime()-start;
			System.out.println(Thread.currentThread().getName()+" Strings per second="+((ITERATIONS*1000000000)/elapsed));
			System.out.println(Thread.currentThread().getName()+" Sort tokenized");
			start=System.nanoTime();
			Arrays.sort(tokenized);
			elapsed=System.nanoTime()-start;
			System.out.println(Thread.currentThread().getName()+" Strings per second="+((ITERATIONS*1000000000)/elapsed));
			System.out.println(Thread.currentThread().getName()+" Compare strings & tokenized");
			for(int i=0;i<ITERATIONS;i++) {
				if(!string[i].equals(tokenized[i].getObject())) {
					System.out.println(Thread.currentThread().getName()+" Not equal at "+i+" "+string[i]+"!="+tokenized[i]);
					break;
				}
			}
			
		}
	}

	private TestTokenizer(boolean rand,int[] indexes) {
		this.indexes=indexes;
		this.rand=rand;
	}
	
	private long getIterationsPerSecond() {
		return iterationsPerSecond;
	}

	@Override
	public void run() {
		//stubs.clear();
		long start=System.nanoTime();
		for(long i=0;i<ITERATIONS;i++) {
			//String value=""+i;
			//value=PREFIX.substring(value.length()-1)+value;
			final String value=strings.get(rand?indexes[(int)i]:(int)i);
			Tokenized<String> stub=tokenizer.getTokenized(value);
			if(!stub.getObject().equals(value)) {
				System.out.println(value+"!="+stub.getObject());
			}
			//stubs.add(stub);
			//System.out.println(stub);
		}
		long elapsed=System.nanoTime()-start;
		iterationsPerSecond=((ITERATIONS*1000000000)/elapsed);
	}		

}
