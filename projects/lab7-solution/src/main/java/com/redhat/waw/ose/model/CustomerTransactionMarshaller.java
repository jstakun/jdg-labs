package com.redhat.waw.ose.model;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class CustomerTransactionMarshaller implements MessageMarshaller<CustomerTransaction> {

	/*required string transactionid = 1;
   
   required string customerid = 2;
   
   required double amount = 3;
   
   required int64 transactionDate = 4;*/
	
	@Override
	public Class<? extends CustomerTransaction> getJavaClass() {
		return CustomerTransaction.class;
	}

	@Override
	public String getTypeName() {
		return "protony.CustomerTransaction";
	}

	@Override
	public CustomerTransaction readFrom(
			org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader)
			throws IOException {
		CustomerTransaction t = new CustomerTransaction(); 
		t.setTransactionid(reader.readString("transactionid"));
		t.setCustomerid(reader.readString("customerid"));
		t.setAmount(reader.readDouble("amount"));
		t.setTransactionDate(reader.readLong("transactionDate"));
		return t;
	}

	@Override
	public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, CustomerTransaction t) throws IOException {
		writer.writeString("transactionid", t.getTransactionid());
		writer.writeString("customerid", t.getCustomerid());
		writer.writeDouble("amount", t.getAmount());
		writer.writeLong("transactionDate", t.getTransactionDate());
	}

	/*required string transactionid = 1;
   
   required string customerid = 2;
   
   required double amount = 3;
   
   required int64 date = 4;*/
	
}
