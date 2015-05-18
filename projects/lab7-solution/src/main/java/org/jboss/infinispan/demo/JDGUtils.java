package org.jboss.infinispan.demo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
public class JDGUtils {
	
	public static String routingTable(Cache<?, ?> cache) {
		return cache.getAdvancedCache().getDistributionManager().getConsistentHash().getRoutingTableAsString();
	}

	public static List<Address> locate(Cache<String, ?> cache, String id) {
		return cache.getAdvancedCache().getDistributionManager().getConsistentHash().locateOwners(id);
	}

	public static Address locatePrimary(Cache<String, ?> cache, String id) {
		return cache.getAdvancedCache().getDistributionManager().getConsistentHash().locatePrimaryOwner(id);
	}

	public static boolean checkIfCacheIsPrimaryFor(Cache<String, ?> cache, String key) {
		return cache.getAdvancedCache().getDistributionManager().getPrimaryLocation(key).equals(cache.getCacheManager().getAddress());
	}

	public static boolean checkIfKeyIsLocalInCache(Cache<String, ?> cache, String key) {
		return cache.getAdvancedCache().getDistributionManager().getLocality(key).isLocal();
	}

	public static boolean checkIfCacheIsSecondaryFor(Cache<String, ?> cache, String key) {
		return !checkIfCacheIsPrimaryFor(cache, key) && checkIfKeyIsLocalInCache(cache, key);
	}

	public static Set<String> valuesFromKeys(Cache<String, ?> cache) {
		return valuesFromKeys(cache, Filter.ALL);
	}

	public static Set<String> localValuesFromKeys(Cache<String, ?> cache) {
		return valuesFromKeys(cache, Filter.LOCAL);
	}

	public static Set<String> primaryValuesFromKeys(Cache<String, ?> cache) {
		return valuesFromKeys(cache, Filter.PRIMARY);
	}

	public static Set<String> replicaValuesFromKeys(Cache<String, ?> cache) {
		return valuesFromKeys(cache, Filter.REPLICA);
	}

	private static Set<String> valuesFromKeys(Cache<String, ?> cache, Filter filter) {
		Set<String> values = new HashSet<String>();
		for (String s : cache.keySet()) {
			switch (filter) {
				case ALL:
					values.add(s + " " + cache.get(s));
					break;
				case LOCAL:
					if (checkIfKeyIsLocalInCache(cache, s)) {
						values.add(s + " " + cache.get(s));
					}
					break;
				case PRIMARY:
					if (checkIfCacheIsPrimaryFor(cache, s)) {
						values.add(s + " " + cache.get(s));
					}
					break;
				case REPLICA:
					if (checkIfCacheIsSecondaryFor(cache, s)) {
						values.add(s + " " + cache.get(s));
					}
					break;
			}
		}
		return values;
	}


	private static enum Filter {ALL, LOCAL, PRIMARY, REPLICA};
}