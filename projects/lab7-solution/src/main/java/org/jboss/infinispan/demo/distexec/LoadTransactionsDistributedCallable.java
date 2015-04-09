package org.jboss.infinispan.demo.distexec;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import javax.naming.InitialContext;

import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.distexec.DistributedTaskFailoverPolicy;
import org.infinispan.distexec.FailoverContext;
import org.infinispan.remoting.transport.Address;
import org.jboss.infinispan.demo.RemoteCacheLoader;

import com.redhat.waw.ose.model.CustomerTransaction;

public class LoadTransactionsDistributedCallable implements DistributedCallable<String, CustomerTransaction, Long>, Serializable, DistributedTaskFailoverPolicy {

	private static final long serialVersionUID = 1L;
	//private Cache<String, CustomerTransaction> transactionCache;
	private Set<String> inputKeys;
	
	private int batchSize;
	
	public LoadTransactionsDistributedCallable(int batchSize) {
		this.batchSize = batchSize;
	}
	
	@Override
	public Long call() throws Exception {
			
		InitialContext ctx = new InitialContext();
		RemoteCacheLoader loader = (RemoteCacheLoader)ctx.lookup("java:global/mytodo/RemoteCacheLoader");
		
		if (loader == null) {
			throw new Exception("Remote Cache Loader is not available!");
		}
		
		int i = 0;
		
		try {
			i = loader.loadTransactionBatchesToRemoteCache(inputKeys, batchSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		/*for (String key : inputKeys) {
			CustomerTransaction ct = transactionCache.get(key);
			if (ct != null) {
				tService.loadTransactionToRemoteCache(key);
				i++;
				System.out.println("Transaction " + key + " saved to remote cache");
			}
		}*/
		
		return new Long(i);
	}
	
	@Override
	public void setEnvironment(Cache<String, CustomerTransaction> transactionCache, Set<String> inputKeys) {
		//this.transactionCache = transactionCache;		
		this.inputKeys = Collections.synchronizedSet(inputKeys);
	}

	@Override
	public Address failover(FailoverContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int maxFailoverAttempts() {
		return 2;
	}
}
