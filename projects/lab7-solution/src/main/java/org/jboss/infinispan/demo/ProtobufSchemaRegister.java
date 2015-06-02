package org.jboss.infinispan.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import com.redhat.waw.ose.model.Customer;
import com.redhat.waw.ose.model.CustomerMarshaller;
import com.redhat.waw.ose.model.CustomerTransaction;
import com.redhat.waw.ose.model.CustomerTransactionMarshaller;

public class ProtobufSchemaRegister {

	private static final String PROTOBUF_DEFINITION_RESOURCE = "/protony/customer.proto";
	
	public static void registerJmx(String serverHost, int serverJmxPort, String cacheContainerName) throws MalformedURLException, IOException, MalformedObjectNameException, InstanceNotFoundException, MBeanException, ReflectionException {
			String schemaFileName = "customer.proto";     // The name of the schema file
			String schemaFileContents = readResource(PROTOBUF_DEFINITION_RESOURCE); // The Protobuf schema file contents

			JMXConnector jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:remoting-jmx://" + serverHost + ":" + serverJmxPort));
			MBeanServerConnection jmxConnection = jmxConnector.getMBeanServerConnection();

			ObjectName protobufMetadataManagerObjName = new ObjectName("jboss.infinispan:type=RemoteQuery,name=" + 
			ObjectName.quote(cacheContainerName) + ",component=ProtobufMetadataManager");

			jmxConnection.invoke(protobufMetadataManagerObjName, "registerProtofile", new Object[]{schemaFileName, schemaFileContents}, 
				                     new String[]{CustomerTransaction.class.getName(), Customer.class.getName()});
			jmxConnector.close();
	}
	
	public void register(RemoteCacheManager cacheManager) throws DescriptorParserException, IOException {
		//register marshaller to the client
		SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(cacheManager);
		ctx.registerProtoFiles(new FileDescriptorSource().addProtoFile("customer.proto", this.getClass().getResourceAsStream(PROTOBUF_DEFINITION_RESOURCE)));
		ctx.registerMarshaller(new CustomerMarshaller());
		ctx.registerMarshaller(new CustomerTransactionMarshaller());				
		
		/*ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
		String generatedSchema = protoSchemaBuilder
		       .fileName(PROTOBUF_DEFINITION_RESOURCE)
		       .packageName("protony")
		       .addClass(CustomerTransaction.class)
		       .build(ctx);
		System.out.println(generatedSchema);*/
		
		//register marshaller to server
		RemoteCache<String, String> metadataCache = cacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
		if (!metadataCache.containsKey(PROTOBUF_DEFINITION_RESOURCE)) {
			metadataCache.put(PROTOBUF_DEFINITION_RESOURCE, ProtobufSchemaRegister.readResource(PROTOBUF_DEFINITION_RESOURCE));
			String errors = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
			if (errors != null) {
				throw new IllegalStateException("Some Protobuf schema files contain errors:\n" + errors);
			}
		}
	}
	
	private static String readResource(String resourcePath) throws IOException {
		InputStream is = ProtobufSchemaRegister.class.getResourceAsStream(resourcePath);
		try {
			final Reader reader = new InputStreamReader(is, "UTF-8");
			StringWriter writer = new StringWriter();
			char[] buf = new char[1024];
			int len;
			while ((len = reader.read(buf)) != -1) {
				writer.write(buf, 0, len);
			}
			return writer.toString();
		} finally {
			is.close();
		}
	}
}
