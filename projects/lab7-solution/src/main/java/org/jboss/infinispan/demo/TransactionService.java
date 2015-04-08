package org.jboss.infinispan.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.infinispan.AdvancedCache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutionCompletionService;
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
	
	@PersistenceContext(unitName = "eucustomers")
    private EntityManager entityManager;
	
	private static int batchCounter = 0;
	
	public void generateTestTransaction(int count) {
		long start = System.currentTimeMillis();
		Random r = new Random(System.currentTimeMillis());
		System.out.println("Starting loading transaction batch...");
		Map<String, CustomerTransaction> ctbatch = new HashMap<String, CustomerTransaction>();
		
		if (entityManager == null) {
			System.err.println("Entity Manager is not available!");
		}
		
		List<String> customerids = new CustomerDataProvider(entityManager).getCustomerIds();
		System.out.println("Found " + customerids.size() + " customer profiles.");
		
		for(int i=0;i<count;i++) {
			String customerid = customerids.get(r.nextInt(customerids.size()));
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
		
		System.out.println("Started MapReduce task...");
		Map<String, Integer> transactions = new MapReduceTask<String, CustomerTransaction, String, Integer>(transactionCache.getAdvancedCache(), true)
				.mappedWith(new TransactionMapper(o, limit))
				.combinedWith(new TransactionReducer(TransactionReducer.Mode.COMBINE))
				.reducedWith(new TransactionReducer(TransactionReducer.Mode.REDUCE))
				.execute();	
		
		long end = System.currentTimeMillis();
		System.out.println("MapReduce task finished with status: " + transactions.size() + " transactions filtered out in " + (end-start) + " milliseconds.");
		start = end;
		
		System.out.println("Starting Distributed loading task...");
		batchCounter = 0;
		DistributedExecutorService des = new DefaultExecutorService(transactionCache);
		DistributedExecutionCompletionService<Long> decs = new DistributedExecutionCompletionService<Long>(des);
		LoadTransactionsDistributedCallable loaderCallable = new LoadTransactionsDistributedCallable();
		
		long i = 0;
		
		try {
			decs.submitEverywhere(loaderCallable, transactions.keySet().toArray(new String[transactions.keySet().size()]));
			Future<Long> f = null;
			//int counter = 0;
			try {
				while ((f = decs.poll(30, TimeUnit.SECONDS)) != null) {
					i += f.get().longValue();
					//counter++;
				} 
			} catch (Exception e) {
				e.printStackTrace();
				if (f != null) {
					f.cancel(true);
				}
			}		
		} finally {
			des.shutdownNow();
		}
		
		/*List<Future<Long>> results = des.submitEverywhere(loaderCallable, transactions.keySet().toArray(new String[transactions.keySet().size()]));
		for (Future<Long> f : results) {
			try {
				i += f.get(5L, TimeUnit.MINUTES).longValue();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}*/
		
		if (i > 0) {
			end = System.currentTimeMillis();
			System.out.println("Distributed task finished with status: " + i + " transactions loaded to remote cache in " + (end-start) + " milliseconds.");
		} else {
			System.err.println("Please check if all transactions will be loaded to remote cache!");
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
				ct = response.get(1L, TimeUnit.MINUTES);
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
				response.get(5L, TimeUnit.MINUTES);
				System.out.println("Loading done: " + response.isDone());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return transactionBatch.size();
	}
	
	public void join() {
		transactionCache.start();
	}
}
