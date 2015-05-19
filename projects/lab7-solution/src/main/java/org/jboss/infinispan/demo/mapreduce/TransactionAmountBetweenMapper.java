package org.jboss.infinispan.demo.mapreduce;

import java.io.Serializable;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;

import com.redhat.waw.ose.model.CustomerTransaction;

public class TransactionAmountBetweenMapper implements Mapper<String, CustomerTransaction, String, Integer>, Serializable {

	private double min, max;
	
	private static final long serialVersionUID = 1L;
	
	private boolean echo = false;
	
	public TransactionAmountBetweenMapper(double min, double max, boolean echo) {
		this.min = min;
		this.max = max;
		this.echo = echo;
	}

	@Override
	public void map(String key, CustomerTransaction ct, Collector<String, Integer> collector) {
		//TODO use BRMS here
		double amount = ct.getAmount();
		if (amount >= min && amount <= max) {
			if (echo) {
				System.out.println("Transaction " + key + " meet comparison criteria.");
			}	
			collector.emit(key, 1);
		} else {
			if (echo) {
				System.out.println("Transaction " + key + " doesn't meet comparison criteria.");
			}
		}
	}
}
