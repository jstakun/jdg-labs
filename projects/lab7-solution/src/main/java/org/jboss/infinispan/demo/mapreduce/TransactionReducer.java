package org.jboss.infinispan.demo.mapreduce;

import java.io.Serializable;
import java.util.Iterator;

import org.infinispan.distexec.mapreduce.Reducer;

public class TransactionReducer implements Reducer<String, Integer>, Serializable {

	public enum Mode {COMBINE, REDUCE};
	
	private static final long serialVersionUID = 1L;
	
	private Mode mode;
	
	private boolean echo = false;
	
	public TransactionReducer(Mode mode, boolean echo) {
		this.mode = mode;
		this.echo = echo;
	}
	
	@Override
	public Integer reduce(String reducedKey, Iterator<Integer> iter) {
		int sum = 0;
		while (iter.hasNext()) {
			Integer i = (Integer) iter.next();
			sum += i;
		}
		
		if (echo) {
			if (mode.equals(Mode.COMBINE)) {
				System.out.println("Combine task executed for key " + reducedKey);
			} else if (mode.equals(Mode.REDUCE)) {
				System.out.println("Reduce task executed for key " + reducedKey);
			}
		}
		
		return sum;
	}

}
