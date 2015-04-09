package org.jboss.infinispan.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.infinispan.AdvancedCache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.jboss.infinispan.demo.mapreduce.TransactionMapper;
import org.jboss.infinispan.demo.mapreduce.TransactionReducer;

import com.redhat.waw.ose.model.CustomerTransaction;

@Stateless
public class TransactionService {

	@Inject
	AdvancedCache<String, CustomerTransaction> transactionCache;
	
	@Inject
	DistExecRunner distExecRunner;
	
	@PersistenceContext(unitName = "eucustomers")
    private EntityManager entityManager;
	
	
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
    		ctbatch.put(t.getTransactionid(), t);
			
			if ((i > 0 && i % 1000 == 0) || (i == count-1 && !ctbatch.isEmpty())) {
				transactionCache.putAll(ctbatch, 1, TimeUnit.DAYS);
				ctbatch.clear();
				System.out.println("Loaded (" + (i+1) + "/" + count + ") transactions.");
			} 
		}
		long end = System.currentTimeMillis();
		System.out.println("Transaction loading task finished with status: " + count + " transactions loaded in " + (end-start) + " milliseconds.");
	}
	
	public int filterTransactionAmount(TransactionMapper.Operator o, double limit) {
		System.out.println("Started MapReduce task...");
		long start = System.currentTimeMillis();
		Map<String, Integer> transactions = new MapReduceTask<String, CustomerTransaction, String, Integer>(transactionCache.getAdvancedCache(), true)
				.mappedWith(new TransactionMapper(o, limit))
				.combinedWith(new TransactionReducer(TransactionReducer.Mode.COMBINE))
				.reducedWith(new TransactionReducer(TransactionReducer.Mode.REDUCE))
				.execute();	
		
		long end = System.currentTimeMillis();
		System.out.println("MapReduce task finished with status: " + transactions.size() + " transactions filtered out in " + (end-start) + " milliseconds.");
		
		if (!transactions.keySet().isEmpty()) {
			distExecRunner.execLoadTransactions(transactions.keySet());
		} else {
			System.out.println("No transaction available to be loaded to distributed cache.");
		}
		
		return transactions.size();
	}
	
	public boolean clear() {
		transactionCache.clear();
		System.out.println("Transaction cache cleared.");
		return transactionCache.isEmpty();
	}
	
	public void join() {
		transactionCache.start();
	}
}
