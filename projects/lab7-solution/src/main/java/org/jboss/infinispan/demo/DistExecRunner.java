package org.jboss.infinispan.demo;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.infinispan.AdvancedCache;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutionCompletionService;
import org.infinispan.distexec.DistributedExecutorService;
import org.jboss.infinispan.demo.distexec.LoadTransactionsDistributedCallable;

import com.redhat.waw.ose.model.CustomerTransaction;

@Singleton
@Startup
public class DistExecRunner {

	private static final int BATCH_SIZE = 1000;
	
	@Inject
	AdvancedCache<String, CustomerTransaction> transactionCache;

	@Inject
	RemoteCacheLoader remoteCacheLoader;
	
	public void execLoadTransactions(Set<String> transactions) {
		remoteCacheLoader.clear();
		
		System.out.println("Starting Distributed loading task...");
		long start = System.currentTimeMillis();
		DistributedExecutorService des = new DefaultExecutorService(transactionCache);
		DistributedExecutionCompletionService<Long> decs = new DistributedExecutionCompletionService<Long>(des);
		LoadTransactionsDistributedCallable loaderCallable = new LoadTransactionsDistributedCallable(BATCH_SIZE);
	
		//DistributedTaskBuilder<Long> taskBuilder = des.createDistributedTaskBuilder(loaderCallable);
		//taskBuilder.failoverPolicy(DefaultExecutorService.RANDOM_NODE_FAILOVER);
		//DistributedTask<Long> distributedTask = taskBuilder.build();
		
		long i = 0;
		int size = transactions.size();
		
		try {
			List<Future<Long>> results = decs.submitEverywhere(loaderCallable, transactions.toArray(new String[size]));
			System.out.println("Started " + results.size() + " remote tasks...");
			Future<Long> f = null;
			for(int j=0;j<results.size();j++) {
				try {
					f = decs.take(); //decs.poll(10, TimeUnit.SECONDS);
					System.out.println("Task " + (j+1) + " has been taken.");
				} catch (Exception e) {
					e.printStackTrace();
				}	
				
				try {
					if (f != null) {
						i += f.get().longValue();
					} 
				} catch (Exception e) {
					e.printStackTrace();
				}	 
			}
			System.out.println("Finished remote task processing.");
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
		
		/*List<Future<Long>> results = decs.submitEverywhere(loaderCallable, transactions.keySet().toArray(new String[transactions.keySet().size()]));
		for (Future<Long> f : results) {
			try {
				i += f.get(5L, TimeUnit.MINUTES).longValue();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}*/
	
		if (i > 0) {
			long end = System.currentTimeMillis();
			System.out.println("Distributed task finished with status: " + i + " transactions loaded to remote cache in " + (end-start) + " milliseconds.");
		} else {
			System.err.println("Please check if all transactions will be loaded to remote cache!");
		}
	}
}
