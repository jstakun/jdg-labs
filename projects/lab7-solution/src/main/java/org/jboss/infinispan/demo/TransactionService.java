package org.jboss.infinispan.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.StringUtils;
import org.infinispan.AdvancedCache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.remoting.transport.Address;
import org.jboss.infinispan.demo.mapreduce.TransactionAmountBetweenMapper;
import org.jboss.infinispan.demo.mapreduce.TransactionAmountCompareMapper;
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
				putAllTransactions(ctbatch);
				ctbatch.clear();
				System.out.println("Loaded (" + (i+1) + "/" + count + ") transactions.");
			} 
		}
		long end = System.currentTimeMillis();
		System.out.println("Transaction loading task finished with status: " + count + " transactions loaded in " + (end-start) + " milliseconds.");
	}
	
	public void putAllTransactions(Map<String, CustomerTransaction> ctbatch) {
		transactionCache.putAll(ctbatch, 1, TimeUnit.DAYS);
	}
	
	public void putTransaction(CustomerTransaction ct) {
		transactionCache.put(ct.getTransactionid(), ct, 1, TimeUnit.DAYS);
	}
	
	public int filterTransactionAmountCompare(TransactionAmountCompareMapper.Operator o, double limit, boolean echo) {
		Mapper<String, CustomerTransaction, String, Integer> transactionMapper = new TransactionAmountCompareMapper(o, limit, echo);
		return filterTransactions(transactionMapper, echo);
	}
	
	public int filterTransactionAmountBetween(double min, double max, boolean echo) {
		Mapper<String, CustomerTransaction, String, Integer> transactionMapper = new TransactionAmountBetweenMapper(min, max, echo);
		return filterTransactions(transactionMapper, echo);
	}
	
	private int filterTransactions(Mapper<String, CustomerTransaction, String, Integer> transactionMapper, boolean echo) {
		if (echo) {
			System.out.println("Echo is set to true.");
			long timeout = transactionCache.getRpcManager().getDefaultRpcOptions(true).timeout();
			String timeunit = transactionCache.getRpcManager().getDefaultRpcOptions(true).timeUnit().name();
			System.out.println("DistExec timeout is set to " + timeout + " " + timeunit);
		}
		
		System.out.println("Started Map Reduce task...");
		long start = System.currentTimeMillis();
		Map<String, Integer> transactions = new MapReduceTask<String, CustomerTransaction, String, Integer>(transactionCache.getAdvancedCache())
				.mappedWith(transactionMapper)
				//.combinedWith(new TransactionReducer(TransactionReducer.Mode.COMBINE, echo))
				.reducedWith(new TransactionReducer(TransactionReducer.Mode.REDUCE, echo))
				.execute();	
		
		long end = System.currentTimeMillis();
		System.out.println("Map Reduce task finished with status: " + transactions.size() + " transactions filtered out in " + (end-start) + " milliseconds.");
		
		if (!transactions.keySet().isEmpty()) {
			if (!distExecRunner.isRunning()) {
				distExecRunner.execLoadTransactions(transactions.keySet());
			} else {
				System.out.println("DistExec loading task in progress. Please wait and retry.");
			}
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
	
	public int[] getSize() {
		int total = transactionCache.size();
		int primary = 0;
		Address addr = transactionCache.getCacheManager().getAddress();
		for (String key : transactionCache.keySet()) {
			if (transactionCache.getDistributionManager().getPrimaryLocation(key).equals(addr)) {
				primary++;
			}
		}
		return new int[] {total, primary};
	}
	
	public String getKeys(int number) {
		List<String> keys = new ArrayList<String>();
		for (String key : transactionCache.keySet()) {
			if (keys.size() < number) {
				keys.add(key);
			} else {
				break;
			}
		}
		return StringUtils.join(keys, ",");
	}
	
	public Object getTransaction(String key) {
		Object o = transactionCache.getCacheEntry(key).getValue();
		if (o instanceof CustomerTransaction) {
			System.out.println("This object is instance of com.redhat.waw.ose.model.CustomerTransaction.");
		} else {
			System.out.println("This object is not instance of com.redhat.waw.ose.model.CustomerTransaction!");			
		}
		return o;
	}
}
