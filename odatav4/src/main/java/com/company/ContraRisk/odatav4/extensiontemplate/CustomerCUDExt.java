/*
package com.company.ContraRisk.odatav4.extensiontemplate;

// * <h1>Extension Templates!</h1>
// * CDS supports only read & query operations and does not support DML operations 
// * i.e we cannot insert, update, delete data using CDS data model.  
// * To support DML operations, one can write custom code as shown below
// * <p>
// * <b>Note:</b> This code is only for reference


// * This sample code is an extension for the sample model available in the SampleCDS folder.
// * Take the contents in the CDS folder and paste it in the DB module.

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.sap.db.jdbc.trace.Connection;
import com.sap.gateway.v4.rt.api.extensions.DataProviderExtensionContext;
import com.sap.gateway.v4.rt.api.extensions.ExtendDataProvider;
import com.sap.gateway.v4.rt.api.extensions.ExtensionContext;
import com.sap.gateway.v4.rt.api.extensions.ExtensionException;
import com.sap.gateway.v4.rt.api.extensions.RequestType;
import com.sap.gateway.v4.rt.cds.api.CDSDSParams;

public class CustomerCUDExt {
	final static Logger logr = LoggerFactory.getLogger("CustomerCUDExt");

	// * This method encapsulates CREATE functionality for Customer entity.
	@ExtendDataProvider(entitySet = { "Customer" }, requestTypes = RequestType.CREATE)
	public void createCustomer(ExtensionContext ecx) throws ODataApplicationException, ExtensionException {
		String Street = null, Area = null, City = null, State = null, Country = null;
		Connection conn = ((CDSDSParams) ecx.getDSParams()).getConnection();

		PreparedStatement ps = null;

		DataProviderExtensionContext extCtx = ecx.asDataProviderContext();
		DeserializerResult payload = extCtx.getDeserializerResult();
		// Get the entity
		Entity ent = payload.getEntity();

		
		// Entity contains a complex type 'CustAddress'. Read the complex type
		// properties as following
		List<Property> custAddress = ent.getProperty("CustAddress").asComplex().getValue();

		for (Property prop : custAddress) {
			String propName = prop.getName();
			if (propName.equals("Street")) {
				Street = prop.getValue().toString();
			} else if (propName.equals("Area")) {
				Area = prop.getValue().toString();
			} else if (propName.equals("City")) {
				City = prop.getValue().toString();
			} else if (propName.equals("State")) {
				State = prop.getValue().toString();
			} else if (propName.equals("Country")) {
				Country = prop.getValue().toString();
			}
		}

		// Get the value of other properties
		int CustomerId = (Integer) ent.getProperty("CustomerID").getValue();
		String CustType = (String) ent.getProperty("Type").getValue();
		String CustomerName = (String) ent.getProperty("CustomerName").getValue();
		boolean Premium = (Boolean) ent.getProperty("Premium").getValue();

		// Insert statement for Customer table
		String psSQL = "INSERT INTO \"SampleService.Customer\" VALUES (?,?,?,?,?,?,?,?,?)";
		try {
			ps = conn.prepareStatement(psSQL);

			ps.setInt(1, CustomerId);
			ps.setString(2, CustType);
			ps.setString(3, CustomerName);
			ps.setBoolean(4, Premium);
			ps.setString(5, Street);
			ps.setString(6, Area);
			ps.setString(7, City);
			ps.setString(8, State);
			ps.setString(9, Country);
			ps.executeUpdate();

			extCtx.setEntityToBeRead();

		} catch (SQLException e) {
			e.printStackTrace();
			
			// Check if the SQL Exception was due to duplicate entry, based on
			// the error message thrown by HANA DB for unique constraint violation
			if (e.getLocalizedMessage().contains("unique constraint violated")) {
				throw new ODataApplicationException("Duplicate Resource", 400, Locale.US);
			} else {
				throw new ODataApplicationException("Some error occurred while creating customer", 400, Locale.US);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Handling other generic exceptions
			throw new ODataApplicationException(
					"Some unknown error occurred while creating Customer.Please contact admin", 500, Locale.US);
		} finally {
			// Release all resources
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logr.error("!!Some problem occurred while closing the DB connection");
					e.printStackTrace();
				}
			}
		}

	}// End of method createCustomer

	@ExtendDataProvider(entitySet = { "Customer" }, requestTypes = RequestType.UPDATE)
	public void updateCustomer(ExtensionContext ecx) throws ODataApplicationException {
		String Street = null, Area = null, City = null, State = null, Country = null, custType = null,
				customerName = null;
		boolean prio;
		int customerId = 0;
		// Obtain the DB connection
		Connection conn = ((CDSDSParams) ecx.getDSParams()).getConnection();

		PreparedStatement ps = null;
		DataProviderExtensionContext extCtx = ecx.asDataProviderContext();
		// Get the request payload
		DeserializerResult payload = extCtx.getDeserializerResult();

		// Obtain the URI Info to get key predicates for Update operation
		UriInfo uri = extCtx.getUriInfo();
		UriResourceEntitySet eset = (UriResourceEntitySet) uri.getUriResourceParts().get(0);
		// Obtain the key values
		List<UriParameter> keys = eset.getKeyPredicates();

		Entity ent = payload.getEntity();
		customerName = (String) ent.getProperty("CustomerName").getValue();
		prio = Boolean.parseBoolean(ent.getProperty("Premium").getValue().toString());

		//  For update operation, the key values will be taken from URI, not from the payload
		for (UriParameter key : keys) {
			if (key.getName().equals("CustomerID")) {
				customerId = Integer.parseInt(key.getText());
			}
			if (key.getName().equals("Type")) {
				String temp = key.getText();
				custType = temp.substring(1, temp.length() - 1);
			}
		}

		//  Get the complex property values from the entity. Name of complex
		//  property is 'CustAddress'
		List<Property> custAddress = ent.getProperty("CustAddress").asComplex().getValue();

		for (Property prop : custAddress) {
			String propName = prop.getName();
			if (propName.equals("Street")) {
				Street = prop.getValue().toString();
			} else if (propName.equals("Area")) {
				Area = prop.getValue().toString();
			} else if (propName.equals("City")) {
				City = prop.getValue().toString();
			} else if (propName.equals("State")) {
				State = prop.getValue().toString();
			} else if (propName.equals("Country")) {
				Country = prop.getValue().toString();
			}
		}

		// Create SQL Statement for prepareStatement
		String sql = "UPDATE \"SampleService.Customer\" SET \"CustomerName\"=?,"
				+ "\"Premium\"=?,\"CustAddress.Street\"=?,\"CustAddress.Area\"=?,\"CustAddress.City\"=?,"
				+ "\"CustAddress.State\"=?,\"CustAddress.Country\"=? WHERE " + "\"CustomerID\"=? AND \"Type\"=?";
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, customerName);
			ps.setBoolean(2, prio);
			ps.setString(3, Street);
			ps.setString(4, Area);
			ps.setString(5, City);
			ps.setString(6, State);
			ps.setString(7, Country);
			ps.setInt(8, customerId);
			ps.setString(9, custType);
			int i = ps.executeUpdate();

			// If i==0, no rows were affected. This means there was no entity.
			// So throw 404 'Entity Not Found' exception
			if (i == 0) {
				throw new ODataApplicationException("Entity Not Found!", 404, Locale.US);
			}

			extCtx.setEntityToBeRead();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new ODataApplicationException("Some error occurred while updating customer", 400, Locale.US);
		} catch (Exception e) {
			// Handling generic exceptions
			e.printStackTrace();
			throw new ODataApplicationException(
					"Some unknown error occurred while updating Customer.Please contact admin", 500, Locale.US);
		} finally {
			// Releasing all resources
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logr.error("!!Some problem occurred while closing the DB connection");
					e.printStackTrace();
				}
			}
		}
	} // End of updateCustomer method

	@ExtendDataProvider(entitySet = { "Customer" }, requestTypes = RequestType.DELETE)
	public void deleteCustomer(ExtensionContext ectx) throws ODataApplicationException {
		// Get DB connection
		Connection conn = ((CDSDSParams) ectx.getDSParams()).getConnection();
		String custType = null;
		int custID = 0;
		PreparedStatement ps = null;
		DataProviderExtensionContext extCtx = ectx.asDataProviderContext();

		// Get URI info to obtain key predicates. No payload for delete
		UriInfo uri = extCtx.getUriInfo();
		UriResourceEntitySet entSet = (UriResourceEntitySet) uri.getUriResourceParts().get(0);
		List<UriParameter> keys = entSet.getKeyPredicates();
		for (UriParameter key : keys) {
			if (key.getName().equals("CustomerID")) {
				custID = Integer.parseInt(key.getText());
			}
			if (key.getName().equals("Type")) {
				String temp = key.getText();
				custType = temp.substring(1, temp.length() - 1);
			}
		}
		// Prepare the SQL statement for prepareStatement
		String sql = "DELETE FROM \"SampleService.Customer\" WHERE \"CustomerID\"=? AND \"Type\"=?";
		try {
			ps = conn.prepareStatement(sql);
			ps.setInt(1, custID);
			ps.setString(2, custType);
			int i = ps.executeUpdate();
			
			//  If i==0, no rows were affected. This means there was no entity.
			//  So throw 404 'Entity Not Found' exception
			if (i == 0) {
				throw new ODataApplicationException("Entity not found!", 404, Locale.US);
			}

		} catch (SQLException e) {
			// Handle other SQL Exceptions
			e.printStackTrace();
			throw new ODataApplicationException("An error occurred while deleting Customer.", 400, Locale.US);
		} catch (Exception e) {
			//Handling for other generic exceptions
			e.printStackTrace();
			throw new ODataApplicationException(
					"Some unknown error occurred while deleting Customer.Please contact admin.", 500, Locale.US);
		} finally {
			// release all resources
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logr.error("Some problem occurred while closing the DB connection!");
					e.printStackTrace();
				}
			}
		}

	} // End of method deleteCustomer

} // End of Class
*/
