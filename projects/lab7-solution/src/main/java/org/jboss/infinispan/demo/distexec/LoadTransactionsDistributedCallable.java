package org.jboss.infinispan.demo.distexec;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import javax.naming.InitialContext;

import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;
import org.jboss.infinispan.demo.TransactionService;

import com.redhat.waw.ose.model.CustomerTransaction;

public class LoadTransactionsDistributedCallable implements DistributedCallable<String, CustomerTransaction, Long>, Serializable {

	private static final long serialVersionUID = 1L;
	//private Cache<String, CustomerTransaction> transactionCache;
	private Set<String> inputKeys;
	
	@Override
	public Long call() throws Exception {
			
		InitialContext ctx = new InitialContext();
		TransactionService tService = (TransactionService)ctx.lookup("java:global/mytodo/TransactionService");
		
		if (tService == null) {
			throw new Exception("TransactionService is not available!");
		}
		
		int i = 0;
		
		try {
			i = tService.loadTransactionBatchToRemoteCache(inputKeys, 1000);
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
}
