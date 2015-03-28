package com.redhat.waw.ose.model;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class CustomerMarshaller implements MessageMarshaller<Customer> {

	/*
   required string customerid = 1;

   optional string firstname = 2;

   optional string lastname = 3;

   optional string city = 4;

   optional string country = 5;

   optional string middlename = 6;

   optional string phonenumber = 7;

   optional string postalcode = 8;

   optional string stateprovince = 9;

   optional string streetaddress = 10;

   optional string streetaddress2 = 11;
	 */
	
	@Override
	public Class<? extends Customer> getJavaClass() {
		return Customer.class;
	}

	@Override
	public String getTypeName() {
		return "protony.Customer";
	}

	@Override
	public Customer readFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
		Customer c = new Customer();
		c.setCustomerid(reader.readString("customerid"));
		c.setFirstname(reader.readString("firstname"));
		c.setLastname(reader.readString("lastname"));
		c.setCity(reader.readString("city"));
		c.setCountry(reader.readString("country"));
		c.setMiddlename(reader.readString("middlename"));
		c.setPhonenumber(reader.readString("phonenumber"));
		c.setPostalcode(reader.readString("postalcode"));
		c.setStateprovince(reader.readString("stateprovince"));
		c.setStreetaddress(reader.readString("streetaddress"));
		c.setStreetaddress2(reader.readString("streetaddress2"));
		return c;
	}

	@Override
	public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, Customer c) throws IOException {
		writer.writeString("customerid", c.getCustomerid());
		writer.writeString("firstname", c.getFirstname());
		writer.writeString("lastname", c.getLastname());
		writer.writeString("city", c.getCity());
		writer.writeString("country", c.getCountry());
		writer.writeString("middlename", c.getMiddlename());
		writer.writeString("phonenumber", c.getPhonenumber());
		writer.writeString("postalcode", c.getPostalcode());
		writer.writeString("stateprovince", c.getStateprovince());
		writer.writeString("streetaddress", c.getStreetaddress());
		writer.writeString("streetaddress2", c.getStreetaddress2());
		
	}

}
