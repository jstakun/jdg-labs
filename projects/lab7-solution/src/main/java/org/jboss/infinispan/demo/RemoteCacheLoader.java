package org.jboss.infinispan.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.infinispan.AdvancedCache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;

import com.google.common.collect.Iterables;
import com.redhat.waw.ose.model.CustomerTransaction;

@Stateless
public class RemoteCacheLoader {
	
	private static int batchCounter = 0;
	private static int transactionsCounter = 0;
	
	@Inject
	AdvancedCache<String, CustomerTransaction> transactionCache;
	
	@Inject
	RemoteCache<String, CustomerTransaction> remoteCache;

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
	
	public int loadTransactionBatchesToRemoteCache(Set<String> keys, int size) {
		int i = 0;
		int transactionCount = keys.size();
		int batchCount = (size/transactionCount + 1);
		System.out.println(batchCount + " batches will be loaded to remote cache...");
		for (List<String> partition : Iterables.partition(keys, size)) {
			int batchSize = loadTransactionBatchToRemoteCache(size, partition);
			transactionsCounter += batchSize;
			System.out.println("Loaded (" + transactionsCounter + "/" + transactionCount + ") transactions to remote cache in (" + (++batchCounter) + "/" + batchCount + ") batches.");
			i += batchSize;
		}
		System.out.println("Loaded " + batchCount + " batches to remote cache.");
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
	
	public void clear() {
		NotifyingFuture<Void> response = remoteCache.clearAsync();
		
		try {
			response.get();
			batchCounter = 0;
			transactionsCounter = 0;
			System.out.println("Remote cache prepared: " + response.isDone());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
