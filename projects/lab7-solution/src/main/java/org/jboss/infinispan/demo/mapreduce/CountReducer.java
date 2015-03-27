package org.jboss.infinispan.demo.mapreduce;

import java.io.Serializable;
import java.util.Iterator;

import org.infinispan.distexec.mapreduce.Reducer;

public class CountReducer implements Reducer<String, Integer>, Serializable {

	private static final long serialVersionUID = 5918721993899089700L;
	
	public enum MODE {LOCAL, GLOBAL};
	
	private MODE mode;
	
	public CountReducer(MODE mode) {
		this.mode = mode;
	}

	@Override
	public Integer reduce(String reducedKey, Iterator<Integer> iter) {
	    if (mode == MODE.LOCAL) {
	    	return reduceLocal(reducedKey, iter);
	    } else if (mode == MODE.GLOBAL) {
	    	return reduceGlobal(reducedKey, iter);
	    } else {
	    	return -1;
	    }
	}
	
	private Integer reduceGlobal(String reducedKey, Iterator<Integer> iter) {
		int sum = 0;
		while (iter.hasNext()) {
			Integer i = (Integer) iter.next();
			sum += i;
		}
		System.out.println("Running reduce for global collector key " + reducedKey + ": " + sum);
		return sum;
	}
	
	private Integer reduceLocal(String reducedKey, Iterator<Integer> iter) {
		int sum = 0;
		while (iter.hasNext()) {
		    Integer i = (Integer) iter.next();
		    sum += i;
		}
		System.out.println("Running combine for node collector key " + reducedKey + ": " + sum);
		return sum;
	}

}
