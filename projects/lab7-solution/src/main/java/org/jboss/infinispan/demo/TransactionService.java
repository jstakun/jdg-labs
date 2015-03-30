package org.jboss.infinispan.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.jboss.infinispan.demo.distexec.LoadTransactionsDistributedCallable;
import org.jboss.infinispan.demo.mapreduce.TransactionMapper;
import org.jboss.infinispan.demo.mapreduce.TransactionReducer;

import com.google.common.collect.Iterables;
import com.redhat.waw.ose.model.CustomerTransaction;

@Stateless
public class TransactionService {

	@Inject
	Cache<String, CustomerTransaction> transactionCache;
	
	@Inject
	RemoteCache<String, CustomerTransaction> remoteCache;
	
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
		System.out.println("Transaction loading task finished with status: " + count + " transactions loaded.");
	}
	
	public int filterTransactionAmount(TransactionMapper.Operator o, double limit) {
		remoteCache.clear();
		
		System.out.println("Remote cache prepared.");
		
		Map<String, Integer> transactions = new MapReduceTask<String, CustomerTransaction, String, Integer>(transactionCache.getAdvancedCache())
				.mappedWith(new TransactionMapper(o, limit))
				.combinedWith(new TransactionReducer(TransactionReducer.Mode.COMBINE))
				.reducedWith(new TransactionReducer(TransactionReducer.Mode.REDUCE))
				.execute();	
		
		System.out.println("MapReduce task finished with status: " + transactions.size() + " transactions filtered.");
				
		DistributedExecutorService des = new DefaultExecutorService(transactionCache);
		LoadTransactionsDistributedCallable loaderCallable = new LoadTransactionsDistributedCallable();
		
		List<Future<Long>> results = des.submitEverywhere(loaderCallable, transactions.keySet().toArray(new String[transactions.keySet().size()]));
		
		long i = 0;
		for (Future<Long> f : results) {
			try {
				i += f.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Distributed task finished with status: " + i + " transactions loaded to remote cache.");
		
		return transactions.size();
	}
	
	public boolean clear() {
		transactionCache.clear();
		System.out.println("Transaction cache cleared.");
		return transactionCache.isEmpty();
	}
	
	public void loadTransactionToRemoteCache(String key) {
		CustomerTransaction ct = transactionCache.get(key);
		if (ct != null) {
			remoteCache.putAsync(key, ct); //.put(key, ct);
			//System.out.println("Transaction " + key + " saved to remote cache");
		}
	}
	
	public int loadTransactionBatchToRemoteCache(Set<String> keys, int size) {
		int i = 0;
		for (List<String> partition : Iterables.partition(keys, size)) {
			int batchSize = loadTransactionBatchToRemoteCache(size, partition);
			System.out.println("Loaded " + batchSize + " transactions to remote cache.");
			i += batchSize;
		}
		return i;
	}
	
	private int loadTransactionBatchToRemoteCache(int count, List<String> keys) {
		Map<String, CustomerTransaction> transactionBatch = new HashMap<String, CustomerTransaction>();
		
		for (String key : keys) {
			CustomerTransaction ct = transactionCache.get(key);
		    if (ct != null) {
		    	transactionBatch.put(key, ct);
		    }
		    if (transactionBatch.size() >= count) {
		    	break;
		    }
		    
		}
		
		if (!transactionBatch.isEmpty()) {
			remoteCache.putAllAsync(transactionBatch);
		}
		
		return transactionBatch.size();
	}
}
