package org.jboss.infinispan.demo;

import java.util.ArrayList;
import java.util.List;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

public class CustomerTransactionsKieManager {
	
	private static CustomerTransactionsKieManager instance = new CustomerTransactionsKieManager();
	private static final int SESSION_POOL_SIZE = 16;
	private List<KieSession> sessionsPool;
	private List<Long> activeSessionIds;
	private KieBase kieBase;
	
	private void initialize() {
		System.out.println("Initializing KieManager instance");
		KieServices kieServices = KieServices.Factory.get();
		
		ReleaseId releaseId = kieServices.newReleaseId("com.redhat.waw.financial", "CustomerTransactions", "1.0.1");
		KieContainer kContainer = kieServices.newKieContainer(releaseId);
		KieScanner kScanner = kieServices.newKieScanner(kContainer);
		
		// Start the KieScanner polling the Maven repository every 60 seconds
		kScanner.start( 60 * 1000L );
		
		//kScanner.scanNow();
		
		kieBase = kContainer.getKieBase();
		
		sessionsPool = new ArrayList<KieSession>();
		activeSessionIds = new ArrayList<Long>();
		
		for(int i=0;i<SESSION_POOL_SIZE;i++) {
			sessionsPool.add(kieBase.newKieSession());
		}
		
		System.out.println("Created Customer Transactions knowledge base.");
	}
	
	public static CustomerTransactionsKieManager getInstance() {
		return instance;
	}
	
	public synchronized KieSession getKieSession() {
		if (sessionsPool == null) {
			initialize();
		}
		while (true) {
			for (KieSession session : sessionsPool) {
				if (!activeSessionIds.contains(session.getIdentifier())) {
						activeSessionIds.add(session.getIdentifier());
						return session;
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void releaseKieSession(KieSession session, List<FactHandle> fhs) {
		for (FactHandle fh : fhs) {
				session.delete(fh);
		}
		activeSessionIds.remove(session.getIdentifier());		
	}
	
	public synchronized void deleteSessions() {
		System.out.println("Cleaning up KIE sessions...");
		//deleting BRMS Kie sessions pool
		if (sessionsPool != null) {
			for (KieSession session : sessionsPool) {
				session.dispose();
			}
		}
		activeSessionIds = null;
		sessionsPool = null;
		System.out.println("Done.");
	}
}
