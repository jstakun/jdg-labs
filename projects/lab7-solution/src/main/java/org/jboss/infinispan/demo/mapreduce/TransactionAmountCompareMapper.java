package org.jboss.infinispan.demo.mapreduce;

import java.io.Serializable;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;

import com.redhat.waw.ose.model.CustomerTransaction;

public class TransactionAmountCompareMapper implements Mapper<String, CustomerTransaction, String, Integer>, Serializable {

	public enum Operator {G, GE, E, LE, L};
	
	private Operator o;
	
	private double limit;
	
	private static final long serialVersionUID = 1L;
	
	private boolean echo = false;
	
	public TransactionAmountCompareMapper(Operator o, double limit, boolean echo) {
		this.o = o;
		this.limit = limit;
		this.echo = echo;
	}

	@Override
	public void map(String key, CustomerTransaction ct, Collector<String, Integer> collector) {
		if (compare(ct.getAmount())) {
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
	
	private boolean compare(double amount) {
		if (o.equals(Operator.G)) {
			return amount > limit;
		} else if (o.equals(Operator.GE)) {
			return amount >= limit;
		} else if (o.equals(Operator.E)) {
			return amount == limit;
		} else if (o.equals(Operator.LE)) {
			return amount <= limit;
		} else if (o.equals(Operator.L)) {
			return amount < limit;
		} else {
			//default is Greater 
			return amount > limit;
		}
	}

}
