package org.jboss.infinispan.demo.mapreduce;

import java.io.Serializable;
import java.util.Iterator;

import org.infinispan.distexec.mapreduce.Reducer;

public class TransactionReducer implements Reducer<String, Integer>, Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public Integer reduce(String reducedKey, Iterator<Integer> iter) {
		int sum = 0;
		while (iter.hasNext()) {
			Integer i = (Integer) iter.next();
			sum += i;
		}
		return sum;
	}

}
