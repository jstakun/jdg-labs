package org.jboss.infinispan.demo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Transport;
import org.jboss.infinispan.demo.model.Task;

import com.redhat.waw.ose.model.CustomerTransaction;

@Singleton
@Startup
public class Config {

	DefaultCacheManager cacheManager = null;
	/**
	 * 
	 * @return org.infinispan.client.hotrod.RemoteCache<Long, Task>
	 */
	@Produces
	public RemoteCache<Long, Task> getRemoteCache() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.addServer()
			.host("localhost").port(11222).port(11322)
			.security()
	        .authentication()
	            .enable()
	            .serverName("tasks")
	            .saslMechanism("DIGEST-MD5")
	            .callbackHandler(new LoginHandler("thomas", "thomas-123".toCharArray(), "ApplicationRealm"));
		return new RemoteCacheManager(builder.build(), true).getCache("tasks");
	}
	
	@Produces
	public RemoteCache<String, CustomerTransaction> getRemoteTransactionCache() {
		/*ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.addServer().
		    host("localhost").
			port(11522).port(11622).//port(11722).
			marshaller(new ProtoStreamMarshaller());
		
		RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());*/
		
		//hotrod-client.properties
		RemoteCacheManager cacheManager = new RemoteCacheManager(true);
		
		try {
			System.out.println("Registering protobuf schemas...");
			//ProtobufSchemaRegister.registerJmx("localhost", 10099, "clustered");
			new ProtobufSchemaRegister().register(cacheManager);
		    System.out.println("Done");
		} catch (Exception e) {
			e.printStackTrace();					
		}
				
		return cacheManager.getCache("default");
	}
	/**
	 * NOTE: We need an Advanced Cache since we are going to run map reduce functions against it later.
	 * 
	 * @return org.infinispan.AdvancedCache<Long, String>
	 */
	@Produces
	@ApplicationScoped
	public org.infinispan.AdvancedCache<Long, String> getLocalRequestCache() {
		//Cache<Long,String> basicCache = getLocalCacheManager().getCache("client-request-cache",true);
		org.infinispan.Cache<Long,String> basicCache = getLocalCacheManager().getCache("stats", true);
		return basicCache.getAdvancedCache();
	}
	
	@Produces
	@ApplicationScoped
	public org.infinispan.AdvancedCache<String, CustomerTransaction> getLocalTransactionCache() {
		System.out.println("Transactions cache exists: " + getLocalCacheManager().cacheExists("transactions"));
		org.infinispan.Cache<String, CustomerTransaction> basicCache = getLocalCacheManager().getCache("transactions", true);
		return basicCache.getAdvancedCache();
	}
	
	
	/**
	 * DONE: Use org.infinispan.configuration.global.GlobalConfiguration and
	 *  org.infinispan.configuration.cache.Configuration to create a
	 * 	org.infinispan.manager.DefaultCacheManager (which is an implementation of EmbeddedCacheManager)
	 * 
	 * NOTE: You will have to use full namespace for the org.inifinispan.configuration.cache.ConfigurationBuilder
	 *  since we already import org.infinispan.client.hotrod.configuration.ConfigurationBuilder
	 *  
	 * @return org.infinispan.manager.EmbeddedCacheManager
	 * @throws IOException 
	 */
	private synchronized EmbeddedCacheManager getLocalCacheManager() {
		if (cacheManager == null) {
			/*GlobalConfiguration glob = new GlobalConfigurationBuilder()
			.globalJmxStatistics().enable()
			.allowDuplicateDomains(true)
			.build();
	
			org.infinispan.configuration.cache.Configuration loc = new org.infinispan.configuration.cache.ConfigurationBuilder()
			.expiration().lifespan(1,TimeUnit.DAYS)
			.build();
		
			return new DefaultCacheManager(glob, loc, true);*/
		
			System.out.println("Creating new cache manager...");
			
			GlobalConfiguration glob = new GlobalConfigurationBuilder().clusteredDefault() // Builds a default clustered
				// configuration
				.transport().addProperty("configurationFile", "default-configs/default-jgroups-tcp.xml") //"jgroups.xml") // // provide a specific JGroups configuration
				.globalJmxStatistics().allowDuplicateDomains(true).enable() // This method enables the jmx statistics of
				// the global configuration and allows for duplicate JMX domains
				.build(); // Builds the GlobalConfiguration object
		
			org.infinispan.configuration.cache.Configuration loc = new org.infinispan.configuration.cache.ConfigurationBuilder()
		        .jmxStatistics().enable() // Enable JMX statistics
				.clustering().cacheMode(CacheMode.DIST_SYNC)// Set Cache mode to DISTRIBUTED with SYNCHRONOUS replication
				.sync().replTimeout(1, TimeUnit.MINUTES) //dist exec timout increased
				.hash().numOwners(2) // Keeps two copies of each key/value pair
				.expiration().lifespan(1,TimeUnit.DAYS) // Set expiration - cache entries expire after some time (given by
				// the lifespan parameter) and are removed from the cache (cluster-wide).
				.build();
			
			System.out.println("Done.");
			
        	cacheManager = new DefaultCacheManager(glob, loc, true);
		}
		
		return cacheManager;
	}
	
	@PreDestroy
	public void cleanUp() {
		System.out.println("Cleaning up config...");
		//delete BRMS Kie sessions pool
		CustomerTransactionsKieManager.deleteSessions();
		//leave JGroups cluster
		Transport t = getLocalTransactionCache().getRpcManager().getTransport();
		t.stop();
		System.out.println("Done.");
	}
}
