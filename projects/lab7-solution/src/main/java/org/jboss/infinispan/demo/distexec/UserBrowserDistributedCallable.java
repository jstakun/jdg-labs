package org.jboss.infinispan.demo.distexec;

import java.io.Serializable;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;

public class UserBrowserDistributedCallable implements DistributedCallable<Long, String, Long>, Serializable {

	private static final long serialVersionUID = 1L;
	private Cache<Long,String> requestCache;
	//private Set<Long> inputKeys;
	
	@Override
	public Long call() throws Exception {
		int size = requestCache.keySet().size();
		int primary = 0;
		for (long key : requestCache.keySet()) {
			if (checkIfCacheIsPrimaryFor(key)) {
				primary++;
			}
		}	
		System.out.println("Found total " + size + " User Browser logs on this node and " + primary + " are primary ...");
		return new Long(primary);
	}

	@Override
	public void setEnvironment(Cache<Long, String> cache, Set<Long> inputKeys) {
		this.requestCache = cache;
		//this.inputKeys = inputKeys;		
	}
	
	private boolean checkIfCacheIsPrimaryFor(long key) {
		return requestCache.getAdvancedCache().getDistributionManager().getPrimaryLocation(key).equals(requestCache.getCacheManager().getAddress());
	}
}
