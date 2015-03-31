package com.redhat.waw.ose.persistence;

import java.util.Arrays;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import com.redhat.waw.ose.model.Customer;

@Stateless
public class CustomerProvider {

	@PersistenceContext(unitName = "eucustomers")
    private EntityManager entityManager;
	
	private static final String[] customerids = { "CST01010",
		"CST01011",	"CST01012",	"CST01013",	"CST01014",
		"CST01016", "CST01017", "CST01018", "CST01023",
		"CST01024", "CST01025", "CST01026", "CST01028",
		"CST01029", "CST01030", "CST01031", "CST01032",
		"CST01033"};
	
	public List<String> getCustomerIds() {
		/*if (entityManager == null) {
			throw new Exception("EntityManager is not available!");
		}
		TypedQuery<Customer> query = entityManager.createQuery("select c from Customer c", Customer.class);
		return query.getResultList();*/
		return Arrays.asList(customerids);
	}
}
