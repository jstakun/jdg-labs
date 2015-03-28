package org.jboss.infinispan.demo.rest;

import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.jboss.infinispan.demo.TransactionService;

@Stateless
@Path("/transactions")
public class TransactionEndpoint {

	@Inject
	TransactionService tService;
	
	@GET
	@Path("/filter")
	@Produces("application/json")
	public Map<String,Integer> filterTransactions() {
		return tService.filterTransactions();
	}
	
	@GET
	@Path("/gentestdata/{count}")
	@Produces("application/json")
	public Response getTestData(@PathParam("count") int count) {
		if (count < 0) {
			count = 1000;
		}
		tService.generateTestTransaction(count);
		return Response.ok().build();
	}
}
