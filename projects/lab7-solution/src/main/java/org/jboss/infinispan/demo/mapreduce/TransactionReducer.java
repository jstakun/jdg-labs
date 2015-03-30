package org.jboss.infinispan.demo.mapreduce;

import java.io.Serializable;
import java.util.Iterator;

import org.infinispan.distexec.mapreduce.Reducer;

public class TransactionReducer implements Reducer<String, Integer>, Serializable {

	public enum Mode {COMBINE, REDUCE};
	
	private static final long serialVersionUID = 1L;
	private Mode mode;
	
	public TransactionReducer(Mode mode) {
		this.mode = mode;
	}
	
	@Override
	public Integer reduce(String reducedKey, Iterator<Integer> iter) {
		int sum = 0;
		while (iter.hasNext()) {
			Integer i = (Integer) iter.next();
			sum += i;
		}
		
		if (mode == Mode.COMBINE) {
			System.out.println("Reduce task executed for key " + reducedKey + " on the child node.");
		} else if (mode == Mode.REDUCE) {
			System.out.println("Reduce task executed for key " + reducedKey + " on the master node.");
		}
		
		return sum;
	}

}
