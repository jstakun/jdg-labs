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
import org.jboss.infinispan.demo.mapreduce.TransactionMapper;

@Stateless
@Path("/transactions")
public class TransactionEndpoint {

	@Inject
	TransactionService tService;
	
	@GET
	@Path("/filter/amount/{operator}/{limit}")
	@Produces("application/json")
	public Map<String,Integer> filterTransactions(@PathParam("operator") TransactionMapper.Operator o, @PathParam("limit") double limit) {
		return tService.filterTransactionAmount(o, limit);
	}
	
	@GET
	@Path("/gentestdata/{count}")
	@Produces("application/json")
	public Response getTestData(@PathParam("count") int count) {
		if (count < 0) {
			count = 1000;
		}
		tService.generateTestTransaction(count);
		return Response.ok("{count: " + count + "}").build();
	}
	
	@GET
	@Path("/clear")
	public Response clear() {
		boolean isClear = tService.clear();
		return Response.ok("{isClear: " + isClear + "}").build();
	}
}
