package org.jboss.infinispan.demo;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.infinispan.Cache;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.jboss.infinispan.demo.distexec.UserBrowserDistributedCallable;
import org.jboss.infinispan.demo.distexec.UserOSCallable;
import org.jboss.infinispan.demo.mapreduce.CountReducer;
import org.jboss.infinispan.demo.mapreduce.UserBrowserVendorCountMapper;
import org.jboss.infinispan.demo.mapreduce.UserOSCountMapper;

@Stateless
public class BIService {
	
	private String[] fakeUserAgents = new String[] { 
				"Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_4) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/7.0.6 Safari/537.78.2",
				"Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543 Safari/419.3",
				"Mozilla/5.0 (compatible; MSIE 10.6; Windows NT 6.1; Trident/5.0; InfoPath.2; SLCC1; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; .NET CLR 2.0.50727) 3gpp-gba UNTRUSTED/1.0",
	            "Unknown"		
	};
	
	
	@Inject
	Cache<Long,String> requestCache;
	
	Logger log = Logger.getLogger(this.getClass().getName());

	public Map<String,Integer> getRequestStatiscsPerOs() {
		
		DistributedExecutorService des = new DefaultExecutorService(requestCache.getAdvancedCache());
		
		UserOSCallable osCallable = new UserOSCallable(requestCache.getAdvancedCache());
		List<Future<Long>> results = des.submitEverywhere(osCallable);
		long counter = 0;
		for (Future<Long> f : results) {
			try {
				counter += f.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Found " + counter + " User OS logs!");
		
		return new MapReduceTask<Long, String, String, Integer>(requestCache.getAdvancedCache())
				.mappedWith(new UserOSCountMapper())
				.combinedWith(new CountReducer(CountReducer.MODE.LOCAL))
				.reducedWith(new CountReducer(CountReducer.MODE.GLOBAL))
				.execute();	
	}
	
	public Map<String,Integer> getRequestStatiscsPerBrowser() {
		
		DistributedExecutorService des = new DefaultExecutorService(requestCache.getAdvancedCache());
		
		UserBrowserDistributedCallable browserCallable = new UserBrowserDistributedCallable();
		List<Future<Long>> results = des.submitEverywhere(browserCallable);
		long counter = 0;
		for (Future<Long> f : results) {
			try {
				counter += f.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Found " + counter + " User Browser logs!");
		
		return new MapReduceTask<Long, String, String, Integer>(requestCache.getAdvancedCache())
				.mappedWith(new UserBrowserVendorCountMapper())
				.combinedWith(new CountReducer(CountReducer.MODE.LOCAL))
				.reducedWith(new CountReducer(CountReducer.MODE.GLOBAL))
				.execute();	
	}
	
	public void generateTestData() {
		generateTestData(5000,0);
	}
	
	public void generateTestData(int count, int first) {
		Random random = new Random(System.currentTimeMillis());
		System.out.println("Starting loading batch data...");
		for(int i=first;i<(first+count);i++) {
			int agent = random.nextInt(fakeUserAgents.length);
			requestCache.put(new Long(i), fakeUserAgents[agent]);
			if (i % 1000 == 0) {
				System.out.println("Loaded " + i + " key/value pairs");
			}
		}
		System.out.println("Batch data loaded.");
	}
}
