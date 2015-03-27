package org.jboss.infinispan.demo.distexec;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.infinispan.Cache;

public class UserOSCallable implements Callable<Long>, Serializable { 

	private static final long serialVersionUID = 1L;
	
	private Cache<Long,String> requestCache;
	
	public UserOSCallable(Cache<Long,String> requestCache) {
		this.requestCache = requestCache;
	}
	
	@Override
	public Long call() throws Exception {
		int size = requestCache.keySet().size();
		int primary = 0;
		for (long key : requestCache.keySet()) {
			if (checkIfCacheIsPrimaryFor(key)) {
				primary++;
			}
		}	
		System.out.println("Found total " + size + " User OS logs on this node and " + primary + " are primary ...");
		return new Long(primary);
	}
	
	private boolean checkIfCacheIsPrimaryFor(long key) {
		return requestCache.getAdvancedCache().getDistributionManager().getPrimaryLocation(key).equals(requestCache.getCacheManager().getAddress());
	}
}