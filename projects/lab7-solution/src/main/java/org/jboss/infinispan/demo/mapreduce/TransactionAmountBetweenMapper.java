package org.jboss.infinispan.demo.mapreduce;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.jboss.infinispan.demo.CustomerTransactionsKieManager;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import com.redhat.waw.ose.model.Constraints;
import com.redhat.waw.ose.model.CustomerTransaction;
import com.redhat.waw.ose.model.Decision;

public class TransactionAmountBetweenMapper implements Mapper<String, CustomerTransaction, String, Integer>, Serializable {

	private static final long serialVersionUID = 1L;
	
	private boolean echo = false;
	
	private Constraints c;
		
	public TransactionAmountBetweenMapper(double min, double max, boolean echo) {
		c = new Constraints();
		c.setBetweenMin(min);
		c.setBetweenMax(max);
		this.echo = echo;
	}

	@Override
	public void map(String key, CustomerTransaction ct, Collector<String, Integer> collector) {
		//using now BRMS
		
		if (executeEngine(ct)) {
			if (echo) {
				System.out.println("Transaction " + key + " meet comparison criteria: " + ct.getAmount() + " is between (" + c.getBetweenMin() + "," + c.getBetweenMax() + ").");
			}	
			collector.emit(key, 1);
		} else {
			if (echo) {
				
				System.out.println("Transaction " + key + " doesn't meet comparison criteria: " + ct.getAmount() + " isn't between (" + c.getBetweenMin() + "," + c.getBetweenMax() + ").");
			}
		}
	}
	
	private boolean executeEngine(CustomerTransaction ct) {
		KieSession kSession = CustomerTransactionsKieManager.getInstance().getKieSession();  
		List<FactHandle> fhs = new ArrayList<FactHandle>();
		
		Decision d = null;
		
		try {
			fhs.add(kSession.insert(c));
			fhs.add(kSession.insert(ct));
		
			kSession.fireAllRules();
		
			Collection<Object> objects = (Collection<Object>)kSession.getObjects();
		
			for (Object o : objects) {
				if (o instanceof Decision) {
					d = (Decision) o;
					if (d.getId().equals(ct.getTransactionid())) {
						fhs.add(kSession.getFactHandle(o));
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally  {
			CustomerTransactionsKieManager.getInstance().releaseKieSession(kSession, fhs);
		}	
		
		if (d != null && d.getValue().equals("ok")) {
			return true;
		} else {
			return false;
		}
	}
}
