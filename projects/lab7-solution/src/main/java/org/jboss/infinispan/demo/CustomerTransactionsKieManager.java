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
	private static List<KieSession> sessionsPool;
	private static List<Long> activeSessionIds;
	private static final int SESSION_POOL_SIZE = 10;
	
	private CustomerTransactionsKieManager() {
		initialize();
	}
	
	public static CustomerTransactionsKieManager getInstance() {
		return instance;
	}

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
		
		System.out.println("Created Knowledge Base " + kieBase.toString());
	}
	
	public KieSession getKieSession() {
		while (true) {
			for (KieSession session : sessionsPool) {
				synchronized (activeSessionIds) {
					if (!activeSessionIds.contains(session.getIdentifier())) {
						activeSessionIds.add(session.getIdentifier());
						return session;
					}
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void releaseKieSession(KieSession session, List<FactHandle> fhs) {
		synchronized (activeSessionIds) {
			for (FactHandle fh : fhs) {
				session.delete(fh);
			}
			activeSessionIds.remove(session.getIdentifier());
		}
	}
	
	public void deleteSessions() {
		//TODO not yet implemented
	}
}
