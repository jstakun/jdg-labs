package org.jboss.infinispan.demo.mapreduce;

import java.io.Serializable;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;

import com.redhat.waw.ose.model.CustomerTransaction;

public class TransactionMapper implements Mapper<String, CustomerTransaction, String, Integer>, Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public void map(String key, CustomerTransaction ct, Collector<String, Integer> collector) {
		if (ct.getAmount() > 500d) {
			System.out.println("Transaction " + key + " meets criteria with amount " + ct.getAmount());
			collector.emit(key, 1);
		} else {
			System.out.println("Transaction " + key + " doesn't meet criteria with amount " + ct.getAmount());
		}
	}

}
