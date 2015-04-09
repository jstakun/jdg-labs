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

4. Go to http://jdv:8080/mytodo/rest/transactions/filter/amount/G/500 to execute M/R and distributed tasks. You could change G with G, GE, E, LE, L, and 500 with any number between 1 and 1000.

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

1. TODO Connect with JBoss Data Virtualization to remote Data Grid to query for transactions.
