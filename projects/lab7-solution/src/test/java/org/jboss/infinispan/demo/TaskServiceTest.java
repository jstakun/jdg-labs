package org.jboss.infinispan.demo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.infinispan.AdvancedCache;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.infinispan.demo.mapreduce.CountReducer;
import org.jboss.infinispan.demo.mapreduce.UserOSCountMapper;
import org.jboss.infinispan.demo.model.Task;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("deprecation")
@RunWith(Arquillian.class)
public class TaskServiceTest {

	Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	private TaskService taskservice;
	
	@Inject
	private BIService biservice;

	@Inject
	private AdvancedCache<Long, String> requestCache;

	@Deployment
	public static WebArchive createDeployment() {
		File[] jars = Maven.resolver()
				.loadPomFromFile("pom.xml")
				.importRuntimeDependencies()
				.resolve().withTransitivity()
				.asFile();

		return ShrinkWrap
				.create(WebArchive.class, "todo-test.war")
				.addClass(Config.class)
				.addClass(Task.class)
				.addClass(TaskService.class)
				.addClass(BIService.class)
				.addClass(LoginHandler.class)
				.addClass(UserOSCountMapper.class)
				.addClass(CountReducer.class)
				.addAsLibraries(jars)
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@Test
	@InSequence(1)
	public void should_be_deployed() {
		Assert.assertNotNull(taskservice);
	}

	@Test
	@InSequence(2)
	public void testRetrivingTasks() {
		Collection<Task> tasks = taskservice.findAll();
		Assert.assertNotNull(tasks);
	}

	@Test
	@InSequence(3)
	public void testInsertTask() {
		int orgsize = taskservice.findAll().size();
		Task task = new Task();
		task.setTitle("This is a test task");
		task.setCreatedOn(new Date());
		taskservice.insert(task);
		Assert.assertEquals(orgsize + 1, taskservice.findAll().size());

		taskservice.delete(task);
		Assert.assertEquals(orgsize, taskservice.findAll().size());
	}

	@Test
	@InSequence(4)
	public void testUpdateTask() {
		int orgsize = taskservice.findAll().size();
		Task task = new Task();
		task.setTitle("This is the second test task");
		task.setCreatedOn(new Date());
		taskservice.insert(task);

		log.info("###### Inserted task with id " + task.getId());
		task.setDone(true);
		task.setCompletedOn(new Date());
		taskservice.update(task);
		Assert.assertEquals(orgsize + 1, taskservice.findAll().size());

		for (Task listTask : taskservice.findAll()) {
			if ("This is the second test task".equals(listTask.getTitle())) {
				log.info("### task =" + listTask.getTitle());
				Assert.assertNotNull(listTask.getCompletedOn());
				Assert.assertEquals(true, listTask.isDone());
				taskservice.delete(listTask);
			}
		}
		Assert.assertEquals(orgsize, taskservice.findAll().size());
	}

	@Test
	@InSequence(6)
	public void testRequestCache() throws InterruptedException, ExecutionException {		
		int initialSize = requestCache.size();
		ArrayList<NotifyingFuture<String>> futures = new ArrayList<NotifyingFuture<String>>();
		futures.add(requestCache.putAsync(System.nanoTime(), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_4) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/7.0.6 Safari/537.78.2"));
		futures.add(requestCache.putAsync(System.nanoTime(), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_4) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/7.0.6 Safari/537.78.2"));
		futures.add(requestCache.putAsync(System.nanoTime(), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_4) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/7.0.6 Safari/537.78.2"));
		futures.add(requestCache.putAsync(System.nanoTime(), "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19"));
		futures.add(requestCache.putAsync(System.nanoTime(), "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19"));
		futures.add(requestCache.putAsync(System.nanoTime(), "Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543 Safari/419.3"));
		futures.add(requestCache.putAsync(System.nanoTime(), "Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543 Safari/419.3"));
		for (NotifyingFuture<String> notifyingFuture : futures) {
			notifyingFuture.get();
		}
		Assert.assertEquals(initialSize+7, requestCache.size());
		
		Map<String,Integer> userOsCount = biservice.getRequestStatiscsPerOs();
		
		Assert.assertEquals(3, userOsCount.get("Macintosh").intValue());
		Assert.assertEquals(2, userOsCount.get("Android").intValue());
		Assert.assertEquals(2, userOsCount.get("iPhone").intValue());
	}
}
