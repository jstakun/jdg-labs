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
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.distexec.DistributedTask;
import org.infinispan.distexec.DistributedTaskBuilder;
import org.jboss.infinispan.demo.distexec.LoadTransactionsDistributedCallable;

import com.redhat.waw.ose.model.CustomerTransaction;

@Singleton
@Startup
public class DistExecRunner {

	private static final int BATCH_SIZE = 1000;
	
	private static boolean isRunning = false;
	
	@Inject
	AdvancedCache<String, CustomerTransaction> transactionCache;

	@Inject
	RemoteCacheLoader remoteCacheLoader;
	
	public void execLoadTransactions(Set<String> transactions) {
		isRunning = true;
		remoteCacheLoader.clear();
		
		System.out.println("Starting Distributed loading task...");
		long start = System.currentTimeMillis();
		DistributedExecutorService des = new DefaultExecutorService(transactionCache);
		//DistributedExecutionCompletionService<Long> decs = new DistributedExecutionCompletionService<Long>(des);
		
		LoadTransactionsDistributedCallable loaderCallable = new LoadTransactionsDistributedCallable(BATCH_SIZE);
	
		//long timeout = transactionCache.getRpcManager().getDefaultRpcOptions(true).timeout();
		//String timeunit = transactionCache.getRpcManager().getDefaultRpcOptions(true).timeUnit().name();		
		//System.out.println("RpcManager timeout is " + timeout + " " + timeunit);
		
		DistributedTaskBuilder<Long> taskBuilder = des.createDistributedTaskBuilder(loaderCallable);
		DistributedTask<Long> distributedTask = 
				taskBuilder.failoverPolicy(DefaultExecutorService.NO_FAILOVER).
							timeout(1L, TimeUnit.MINUTES).
							build();
		
		long i = 0;
		int size = transactions.size();
		
		/*try {
			List<Future<Long>> results = decs.submitEverywhere(loaderCallable, transactions.toArray(new String[size]));
			System.out.println("Started " + results.size() + " remote tasks...");
			Future<Long> f = null;
			for(int j=0;j<results.size();j++) {
				try {
					f = decs.take(); //decs.poll(10, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				}	
				
				try {
					if (f != null) {
						i += f.get().longValue();
						System.out.println("Task " + (j+1) + " has been finished.");
					} else {
						System.err.println("Task " + (j+1) + " might have failed!");
					}
				} catch (Exception e) {
					System.err.println("Task " + (j+1) + " might have failed! Check error log:");
					e.printStackTrace();
				}	 
			}
			System.out.println("Finished remote tasks processing.");
		} finally {
			des.shutdownNow();
		}*/
		
		try {
			List<Future<Long>> results = des.submitEverywhere(distributedTask, transactions.toArray(new String[size]));
			System.out.println("Started " + results.size() + " remote tasks...");
			for(int j=0;j<results.size();j++) {
				try {
					Future<Long> f = results.get(j);
					if (f != null) {
						i += f.get().longValue();
						System.out.println("Task " + (j+1) + " has been finished.");
					} else {
						System.err.println("Task " + (j+1) + " might have failed!");
					}
				} catch (Exception e) {
					System.err.println("Task " + (j+1) + " might have failed! Check error log:");
					e.printStackTrace();
				}
			}
			System.out.println("Finished remote tasks processing.");
		} finally {
			des.shutdownNow();
		}
	
		if (i > 0) {
			long end = System.currentTimeMillis();
			System.out.println("Distributed task finished with status: " + i + " transactions loaded to remote cache in " + (end-start) + " milliseconds.");
		} else {
			System.err.println("Please check if all transactions will be loaded to remote cache!");
		}
		
		isRunning = false;
	}
	
	
	public boolean isRunning() {
		return isRunning;
	}
	
}
