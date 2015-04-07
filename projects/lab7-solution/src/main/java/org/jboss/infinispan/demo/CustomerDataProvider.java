package org.jboss.infinispan.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.redhat.waw.ose.model.Customer;

public class CustomerDataProvider {

	private EntityManager entityManager;
	
	private static final String[] customerids = { "CST01010",
		"CST01011",	"CST01012",	"CST01013",	"CST01014",
		"CST01016", "CST01017", "CST01018", "CST01023",
		"CST01024", "CST01025", "CST01026", "CST01028",
		"CST01029", "CST01030", "CST01031", "CST01032",
		"CST01033"};
	
	public CustomerDataProvider(EntityManager entityManager) {
		this.entityManager = entityManager;
	}
	
	public List<String> getCustomerIds() {
		if (entityManager != null) {
			TypedQuery<Customer> query = entityManager.createQuery("select c from Customer c", Customer.class);
			List<Customer> customers = query.getResultList(); 
			List<String> customerids = new ArrayList<String>(customers.size());
			for (Customer c : customers) {
				customerids.add(c.getCustomerid());
			}
			System.out.println("Loaded customer data from database.");
			return customerids;
		} else {
			System.err.println("Loaded customer data from cache.");
			return Arrays.asList(customerids);
		}
	}
}
