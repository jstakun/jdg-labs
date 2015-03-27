package org.jboss.infinispan.demo.rest;

import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.PathParam;

import org.jboss.infinispan.demo.BIService;

/**
 * 
 */
@Stateless
@Path("/stats")
public class BiEndpoint {

	@Inject
	BIService biService;

	Logger log = Logger.getLogger(this.getClass().getName());

	@GET
	@Path("/os")
	@Produces("application/json")
	public Map<String,Integer> getRequestStatiscsPerOs() {
		return biService.getRequestStatiscsPerOs();
	}
	
	@GET
	@Path("/browser")
	@Produces("application/json")
	public Map<String,Integer> getRequestStatiscsPerBrowser() {
		return biService.getRequestStatiscsPerBrowser();
	}
	
	@GET
	@Path("/gentestdata")
	@Produces("application/json")
	public Response getTestData() {
		biService.generateTestData();
		return Response.noContent().build();
	}
	
	@GET
	@Path("/gentestdata/{count}/{first}")
	@Produces("application/json")
	public Response getTestData(@PathParam("count") int count,@PathParam("first") int first) {
		if (count < 0) {
			count = 1000;
		}
		if (first < 0) {
			count = 0;
		}
		biService.generateTestData(count, first);
		return Response.noContent().build();
	}
}