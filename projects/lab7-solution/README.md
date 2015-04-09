# EAP and JBoss Data Grid Map/Reduce and distributed tasks execution demo 

![Realtime Big Data Analytics](https://raw.githubusercontent.com/jstakun/jdg-labs/master/projects/lab7-solution/bd_arch.jpg "Realtime Big Data Analytics")

## EAP 6 Setup

1. Install EAP 6.3.

2. Install Red Hat JBoss Data Grid 6.4.1 Library Module for JBoss EAP 6.

3. Install Red Hat JBoss Data Grid 6.4.1 Hot Rot Java Client Module for JBoss EAP 6.

4. Install this application.

```
mvn jboss-as:deploy
```

## Remote Distributed cache setup

1. Install Red Hat JBoss Data Grid 6.4.1 Server.

2. Configure distributed cache in $JDG_HOME/standalone/configuration/clustered.xml.

```
<distributed-cache name="default" mode="ASYNC" segments="20" owners="2" remote-timeout="30000" start="EAGER">
       <locking isolation="READ_COMMITTED" acquire-timeout="60000" concurrency-level="1000" striping="false"/>
       <transaction mode="NONE"/>
       <indexing index="ALL">
            <property name="default.directory_provider">infinispan</property>
            <property name="default.indexmanager">near-real-time</property>
       </indexing>
</distributed-cache>
```

## Running this demo

1. Start multiple JBoss Data Grid instances in clustered mode i.e.

```
$JDG_HOME/bin/clustered.sh -Djboss.server.name=node1 -Djboss.default.jgroups.stack=tcp -Djboss.socket.binding.port-offset=300

$JDG_HOME/bin/clustered.sh -Djboss.server.name=node2 -Djboss.default.jgroups.stack=tcp -Djboss.socket.binding.port-offset=400
```

2. Start multiple EAP instances in standalone-ha or domain mode to form cluster i.e.

```
$JBOSS_HOME/bin/standalone.sh -Djboss.server.base.dir=/opt/jboss/machine1/ --server-config=standalone-ha.xml 

$JBOSS_HOME/bin/standalone.sh -Djboss.server.base.dir=/opt/jboss/machine2/ --server-config=standalone-ha.xml -Djboss.node.name=node2 -Djboss.socket.binding.port-offset=100
```

3. Go to http://host:8080/mytodo/rest/transactions/gentestdata/10 to generate 10 test transactions. You could change 10 with any other number.

4. Go to http://host:8080/mytodo/rest/transactions/filter/amount/G/500 to execute M/R and distributed tasks. You could change G with G, GE, E, LE, L, and 500 with any number between 1 and 1000.

5. Check Distributed cache stats to verify if filtered transactions were loaded to remote Data Grid.

```
$JDG_HOME/bin/cli.sh

You are disconnected at the moment. Type 'connect' to connect to the server or 'help' for the list of supported commands.
[disconnected /] connect localhost:10299
[standalone@localhost:10299 cache-container=clustered] cache default
[standalone@localhost:10299 distributed-cache=default] stats
Statistics: {
...
}
```
## JBoss Data Virtualization setup

1. Install JDV 6.1 on top of EAP 6.3

2. Create JBoss module containing CustomerTransaction POJO, CustomerMarschaller class and customer.proto file and install to EAP.

3. Install Red Hat JBoss Data Grid 6.4.1 Hot Rot Java Client Module for JBoss EAP 6.

4. Configure JDG resource adapter at $JDV_HOME/jboss-eap-6.3/standalone/configuration/standalone.xml

```
<resource-adapter id="infinispanRemQSDSL">
   <module slot="main" id="org.jboss.teiid.resource-adapter.infinispan.dsl"/>
   <connection-definitions>
      <connection-definition class-name="org.teiid.resource.adapter.infinispan.dsl.InfinispanManagedConnectionFactory" jndi-name="java:/infinispanRemoteDSL" enabled="true" use-java-context="true" pool-name="infinispanRemoteDSL">
           <config-property name="CacheTypeMap">
                  default:com.redhat.waw.ose.model.CustomerTransaction;transactionid
           </config-property>
           <config-property name="ProtobufDefinitionFile">
                  /protony/customer.proto
           </config-property>
           <config-property name="Module">
                  com.redhat.waw.ose.model
           </config-property>
           <config-property name="MessageDescriptor">
                  protony.CustomerTransaction
           </config-property>
           <config-property name="MessageMarshallers">
                  com.redhat.waw.ose.model.Customer:com.redhat.waw.ose.model.CustomerMarshaller,com.redhat.waw.ose.model.CustomerTransaction:com.redhat.waw.ose.model.CustomerTransactionMarshaller
           </config-property>
           <config-property name="RemoteServerList">
                  127.0.0.1:11522;127.0.0.1:11622
           </config-property>
      </connection-definition>
   </connection-definitions>
</resource-adapter>
```

5. Install CustomersTransactions virtual database located in this project: https://github.com/jstakun/datavirt/tree/master/Customers
   (You'll need to create EUCustomers data source)
   
For more details for this section please have a look a JDV jdg-remote-cache quickstart: 
https://github.com/teiid/teiid-quickstarts/blob/master/jdg-remote-cache/
  
