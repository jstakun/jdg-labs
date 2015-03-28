package org.jboss.infinispan.demo;

import java.util.Map;
import java.util.Random;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.jboss.infinispan.demo.mapreduce.TransactionMapper;
import org.jboss.infinispan.demo.mapreduce.TransactionReducer;

import com.redhat.waw.ose.model.CustomerTransaction;

@Stateless
public class TransactionService {

	@Inject
	Cache<String, CustomerTransaction> transactionCache;
	
	@Inject
	RemoteCache<String, CustomerTransaction> cache;
	
	public void generateTestTransaction(int count) {
		Random r = new Random(System.currentTimeMillis());
		System.out.println("Starting loading transaction batch...");
		for(int i=0;i<count;i++) {
			//TODO load customerid from DB
			String customerid = "CST01010";
			CustomerTransaction t = new CustomerTransaction();
    		t.setTransactionDate(System.currentTimeMillis());
    		t.setTransactionid(customerid + "_" + t.getTransactionDate() + "_" + i);
    		t.setCustomerid(customerid);
    		t.setAmount(r.nextDouble() * 1000d);
    		transactionCache.put(t.getTransactionid(), t);
			
			if (i > 0 && i % 1000 == 0) {
				System.out.println("Loaded " + i + " transactions");
			}
		}
		System.out.println("Transaction batch loaded.");
	}
	
	public Map<String, Integer> filterTransactionAmount(TransactionMapper.Operator o, double limit) {
		Map<String, Integer> transactions = new MapReduceTask<String, CustomerTransaction, String, Integer>(transactionCache.getAdvancedCache())
				.mappedWith(new TransactionMapper(o, limit))
				.reducedWith(new TransactionReducer())
				.execute();	
		
		System.out.println("MapReduce executed.");
		
		cache.clear();
		
		System.out.println("Remote cache prepared.");
		
		System.out.println(transactions.keySet().size() + " transactions will be loaded to remote cache.");
		
		int i=0;
		for (String key : transactions.keySet()) {
			i++;
			CustomerTransaction ct = transactionCache.get(key);
			if (ct != null) {
				cache.putAsync(key, ct); //.put(key, ct);
				//System.out.println("Transaction " + key + " saved to remote cache");
			}
			if (i > 0 && i % 1000 == 0) {
				System.out.println(i + " transactions loaded to remote cache.");
			}
		}
		
		System.out.println(transactions.keySet().size() + " transactions loaded to remote cache.");
		
		return transactions;
	}
	
	public boolean clear() {
		transactionCache.clear();
		System.out.println("Transaction cache cleared.");
		return transactionCache.isEmpty();
	}
}
