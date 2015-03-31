package org.jboss.infinispan.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.infinispan.AdvancedCache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
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
	AdvancedCache<String, CustomerTransaction> transactionCache;
	
	@Inject
	RemoteCache<String, CustomerTransaction> remoteCache;
	
	private static int batchCounter = 0;
	
	public void generateTestTransaction(int count) {
		long start = System.currentTimeMillis();
		Random r = new Random(System.currentTimeMillis());
		System.out.println("Starting loading transaction batch...");
		Map<String, CustomerTransaction> ctbatch = new HashMap<String, CustomerTransaction>();
		
		for(int i=0;i<count;i++) {
			//TODO load customerid from DB
			String customerid = "CST01010";
			CustomerTransaction t = new CustomerTransaction();
    		t.setTransactionDate(System.currentTimeMillis());
    		t.setTransactionid(customerid + "_" + t.getTransactionDate() + "_" + i);
    		t.setCustomerid(customerid);
    		t.setAmount(r.nextDouble() * 1000d);
    		//transactionCache.put(t.getTransactionid(), t);
    		ctbatch.put(t.getTransactionid(), t);
			
			if (i > 0 && i % 1000 == 0) {
				putTransactionBatchToLocalCache(ctbatch);
				ctbatch.clear();
			} else if (i == count-1 && !ctbatch.isEmpty()) {
				putTransactionBatchToLocalCache(ctbatch);
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("Transaction loading task finished with status: " + count + " transactions loaded in " + (end-start) + " milliseconds.");
	}
	
	private void putTransactionBatchToLocalCache(Map<String, CustomerTransaction> ctbatch) {
		/*NotifyingFuture<Void> response = transactionCache.putAllAsync(ctbatch, 1, TimeUnit.DAYS);
		try {
			response.get();
			System.out.println("Loaded " + ctbatch.size() + " transactions: " + response.isDone());
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		transactionCache.putAll(ctbatch, 1, TimeUnit.DAYS);
		System.out.println("Loaded " + ctbatch.size() + " transactions.");
	}
	
	public int filterTransactionAmount(TransactionMapper.Operator o, double limit) {
		long start = System.currentTimeMillis();
		NotifyingFuture<Void> response = remoteCache.clearAsync();
		try {
			response.get();
			System.out.println("Remote cache prepared: " + response.isDone());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Starting MapReduce task...");
		Map<String, Integer> transactions = new MapReduceTask<String, CustomerTransaction, String, Integer>(transactionCache.getAdvancedCache())
				.mappedWith(new TransactionMapper(o, limit))
				.combinedWith(new TransactionReducer(TransactionReducer.Mode.COMBINE))
				.reducedWith(new TransactionReducer(TransactionReducer.Mode.REDUCE))
				.execute();	
		
		long end = System.currentTimeMillis();
		System.out.println("MapReduce task finished with status: " + transactions.size() + " transactions filtered in " + (end-start) + " milliseconds.");
		start = end;
		
		System.out.println("Starting Distributed loading task...");
		DistributedExecutorService des = new DefaultExecutorService(transactionCache);
		LoadTransactionsDistributedCallable loaderCallable = new LoadTransactionsDistributedCallable();
		
		List<Future<Long>> results = des.submitEverywhere(loaderCallable, transactions.keySet().toArray(new String[transactions.keySet().size()]));
		
		long i = 0;
		int l = 0;
		for (Future<Long> f : results) {
			l++;
			try {
				i += f.get(5L, TimeUnit.MINUTES);
			} catch (TimeoutException e) {
				System.err.println("Network timeout occured. Please check if all transactions will be loaded to remote cache!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (l == results.size()-1) {
			end = System.currentTimeMillis();
			System.out.println("Distributed task finished with status: " + i + " transactions loaded to remote cache in " + (end-start) + " milliseconds.");
		} 
		
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
			NotifyingFuture<CustomerTransaction> response = remoteCache.putAsync(key, ct, 1, TimeUnit.DAYS); //.put(key, ct);
			try {
				ct = response.get();
				System.out.println("Transaction " + ct.getTransactionid() + " saved to remote cache.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public int loadTransactionBatchToRemoteCache(Set<String> keys, int size) {
		int i = 0;
		for (List<String> partition : Iterables.partition(keys, size)) {
			int batchSize = loadTransactionBatchToRemoteCache(size, partition);
			System.out.println("Loaded " + batchSize + " transactions to remote cache in batch " + (++batchCounter) + ".");
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
			System.out.println("Loading batch of " + transactionBatch.size() + " transactions to remote cache.");
			NotifyingFuture<Void> response = remoteCache.putAllAsync(transactionBatch, 1 , TimeUnit.DAYS);
			try {
				response.get();
				System.out.println("Loading done: " + response.isDone());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return transactionBatch.size();
	}
}
