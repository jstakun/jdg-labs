package com.redhat.waw.ose.model;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

@ProtoMessage(name = "CustomerTransaction")
@ProtoDoc("@Indexed")
public class CustomerTransaction implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	@ProtoField(number = 1, required = true)
	@ProtoDoc("@IndexedField(index = true, store = false)")
	public String transactionid;
	
	@ProtoField(number = 2, required = true)
	@ProtoDoc("@IndexedField")
	public String customerid;
	
	@ProtoField(number = 3, required = true)
	public double amount;
	
	@ProtoField(number = 4, required = true)
	public long transactionDate;
	
	public String getTransactionid() {
		return transactionid;
	}

	public void setTransactionid(String transactionid) {
		this.transactionid = transactionid;
	}

	public String getCustomerid() {
		return customerid;
	}

	public void setCustomerid(String customerid) {
		this.customerid = customerid;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public long getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(long transactionDate) {
		this.transactionDate = transactionDate;
	}

}
