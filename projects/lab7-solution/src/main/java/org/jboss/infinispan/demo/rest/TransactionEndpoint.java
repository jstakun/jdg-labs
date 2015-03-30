package org.jboss.infinispan.demo.rest;

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
	public Response filterTransactions(@PathParam("operator") TransactionMapper.Operator o, @PathParam("limit") double limit) {
		Integer count = tService.filterTransactionAmount(o, limit);
		return Response.status(200).entity("{filtered:" +count+"}").build();
	}
	
	@GET
	@Path("/gentestdata/{count}")
	@Produces("application/json")
	public Response getTestData(@PathParam("count") Integer count) {
		if (count < 0) {
			count = 1000;
		}
		tService.generateTestTransaction(count);
		return Response.status(200).entity("{generated:"+count+"}").build();
	}
	
	@GET
	@Path("/clear")
	@Produces("application/json")
	public Response clear() {
		Boolean isClear = tService.clear();
		return Response.status(200).entity("{cleared:"+isClear+"}").build();
	}
}
