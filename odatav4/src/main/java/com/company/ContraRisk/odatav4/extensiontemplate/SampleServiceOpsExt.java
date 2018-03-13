/*
 
// * This sample code is an extension for the sample model available in the SampleCDS folder.
// * Take the contents in the CDS folder and paste it in the DB module.

package com.company.ContraRisk.odatav4.extensiontemplate;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.gateway.v4.rt.api.extensions.DataProviderExtensionContext;
import com.sap.gateway.v4.rt.api.extensions.ExtendDataProvider;
import com.sap.gateway.v4.rt.api.extensions.ExtensionContext;
import com.sap.gateway.v4.rt.cds.api.CDSDSParams;

public class SampleServiceOpsExt {
	//create logger
	final Logger logger = LoggerFactory.getLogger("SampleServiceOpsExt");

	// This annotation denotes that this function would be called when the particular operation is called.
	// It takes in two parameter.
	// serviceName denotes the namespace of your service. It is optional. If not provided it applies to all service. 
	// operationName denotes what operation triggers the method.
	// The user should be aware of what he is returning as part of his operation and return the value accordingly
		 
	//URL: GET  ~/SampleService/GetPremiumCustomers(IsPremium=true)
	@ExtendDataProvider(serviceName = "SampleService", operationName = "GetPremiumCustomers")
	public void getPremiumCusts(ExtensionContext eCtx) throws ODataApplicationException {
		Connection conn = ((CDSDSParams) eCtx.getDSParams()).getConnection();
		DataProviderExtensionContext dpExt = eCtx.asDataProviderContext();
		// Declaring the Entity Collection which will be returned for the
		// function
		EntityCollection resultSet = new EntityCollection();
		// get the function parameter
		UriInfo uri = dpExt.getUriInfo();
		UriResource firstSeg = uri.getUriResourceParts().get(0);
		UriParameter funcParam = ((UriResourceFunction) firstSeg).getParameters().get(0);
		boolean isPremium = Boolean.parseBoolean(funcParam.getText());

		// SQL Operations
		String sql = "SELECT * FROM \"SampleService.Customer\" WHERE " + "\"Premium\"=?";
		try {
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setBoolean(1, isPremium);
			ResultSet rs = ps.executeQuery();

			// The Return type of the function is Entity Set. So preparing
			// entity set out of the query result set
			while (rs.next()) {
				// Prepare the complex value to be set in the entity
				ComplexValue cv = new ComplexValue();
				List<Property> cplxProp = cv.getValue();
				cplxProp.add(new Property(null, "Street", ValueType.PRIMITIVE, rs.getString("CustAddress.Street")));
				cplxProp.add(new Property(null, "Area", ValueType.PRIMITIVE, rs.getString("CustAddress.Area")));
				cplxProp.add(new Property(null, "City", ValueType.PRIMITIVE, rs.getString("CustAddress.City")));
				cplxProp.add(new Property(null, "State", ValueType.PRIMITIVE, rs.getString("CustAddress.State")));
				cplxProp.add(new Property(null, "Country", ValueType.PRIMITIVE, rs.getString("CustAddress.Country")));
				// Create the entity and add primitive and complex value
				// properties
				Entity ent = new Entity();
				ent.addProperty(new Property(null, "CustomerID", ValueType.PRIMITIVE, rs.getInt("CustomerID")));
				ent.addProperty(new Property(null, "Type", ValueType.PRIMITIVE, rs.getString("Type")));
				ent.addProperty(new Property(null, "CustomerName", ValueType.PRIMITIVE, rs.getString("CustomerName")));
				ent.addProperty(new Property(null, "Premium", ValueType.PRIMITIVE, rs.getBoolean("Premium")));
				ent.addProperty(new Property(null, "CustAddress", ValueType.COMPLEX, cv));
				// Add entity to the entity set
				resultSet.getEntities().add(ent);
			}
			// Set the entity collection
			dpExt.setResultEntityCollection(resultSet);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ODataApplicationException(
					"Some error occurred while fetching Customers. Please contact the admin for details", 500,
					Locale.US);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ODataApplicationException("Some unknown error occurred. Please contact the admin for details",
					500, Locale.US);
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logger.error("Error while closing the connection");
					e.printStackTrace();
				}
			}
		}

	}// End of getPremiumCusts method

	//URL: POST ~/SampleService/Customer(CustomerID=1,Type='Enterprise')/SampleService.SetPremiumStatus
	//Header: Content-Type: application/json
	//Body: {"SetPremium" : false} 
	@ExtendDataProvider(serviceName = "SampleService", operationName = "SetPremiumStatus")
	public void setPremiumCusts(ExtensionContext eCtx) throws ODataApplicationException {
		String custType = null;
		int custId = 0;
		Connection conn = ((CDSDSParams) eCtx.getDSParams()).getConnection();
		DataProviderExtensionContext dpExt = eCtx.asDataProviderContext();

		// Pick up the key predicates for customer from UriInfo as the Action is
		// bound to a customer
		UriInfo uri = dpExt.getUriInfo();
		UriResourceEntitySet firstSeg = (UriResourceEntitySet) uri.getUriResourceParts().get(0);
		List<UriParameter> keyPred = firstSeg.getKeyPredicates();
		for (UriParameter key : keyPred) {
			if (key.getName().equals("CustomerID")) {
				custId = Integer.parseInt(key.getText());
			}
			if (key.getName().equals("Type")) {
				String temp = key.getText();
				custType = temp.substring(1, temp.length() - 1);
			}
		}

		// Read the request payload
		InputStream is = dpExt.getODataRequest().getBody();
		ObjectMapper om = new ObjectMapper();
		try {
			// Get the JSON structure of the request payload
			JsonNode jsonNode = om.readTree(is);
			// Obtain the request parameter
			boolean setPremium = jsonNode.get("SetPremium").asBoolean();
			// SQL Operations
			String sql = "UPDATE \"SampleService.Customer\" SET \"Premium\"=? WHERE \"CustomerID\"=? AND \"Type\"=?";
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setBoolean(1, setPremium);
			ps.setInt(2, custId);
			ps.setString(3, custType);
			int i = ps.executeUpdate();
			ps.clearParameters();
			
			// if no row in the DB is affected, i would be zero. In that throw
			// an application exception
			if (i == 0) {
				logger.error("Error occurred while setting Premium option: Entity not found");
				throw new ODataApplicationException("Entity Not Found!", 404, Locale.US);
			} else {
				// Else, retrieve the entity and set it in the result
				String sqlSelect = "SELECT * FROM \"SampleService.Customer\" WHERE \"CustomerID\"=? AND \"Type\"=?";
				ps = conn.prepareStatement(sqlSelect);
				ps.setInt(1, custId);
				ps.setString(2, custType);
				ResultSet rs = ps.executeQuery();
				Entity ent = new Entity();
				while (rs.next()) {
					// Prepare the complex value to be set in the entity
					ComplexValue cv = new ComplexValue();
					List<Property> cplxProp = cv.getValue();
					cplxProp.add(new Property(null, "Street", ValueType.PRIMITIVE, rs.getString("CustAddress.Street")));
					cplxProp.add(new Property(null, "Area", ValueType.PRIMITIVE, rs.getString("CustAddress.Area")));
					cplxProp.add(new Property(null, "City", ValueType.PRIMITIVE, rs.getString("CustAddress.City")));
					cplxProp.add(new Property(null, "State", ValueType.PRIMITIVE, rs.getString("CustAddress.State")));
					cplxProp.add(
							new Property(null, "Country", ValueType.PRIMITIVE, rs.getString("CustAddress.Country")));
					// Add the complex and primitive type values to the entity
					ent.addProperty(new Property(null, "CustomerID", ValueType.PRIMITIVE, rs.getInt("CustomerID")));
					ent.addProperty(new Property(null, "Type", ValueType.PRIMITIVE, rs.getString("Type")));
					ent.addProperty(
							new Property(null, "CustomerName", ValueType.PRIMITIVE, rs.getString("CustomerName")));
					ent.addProperty(new Property(null, "Premium", ValueType.PRIMITIVE, rs.getBoolean("Premium")));
					ent.addProperty(new Property(null, "CustAddress", ValueType.COMPLEX, cv));
				}
				dpExt.setResultEntity(ent);
			}

		} catch (JsonProcessingException e) {
			logger.error("Error in JSON Parsing");
			e.printStackTrace();
			throw new ODataApplicationException("Error in JSON Parsing. Check the payload or contact admin", 400,
					Locale.US);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ODataApplicationException(
					"Some error occurred while processing the request. Please check the logs or contact admin", 500,
					Locale.US);
		} catch (SQLException e) {
			logger.error("Some SQL Exception occurred while executing the operation SetPremiumStatus");
			e.printStackTrace();
			throw new ODataApplicationException(
					"Some error occurred while processing the request. Please check the logs or contact admin", 500,
					Locale.US);
		} catch(Exception e){
			e.printStackTrace();
			throw new ODataApplicationException(
					"Some unknown error occurred. Please check the logs or contact admin", 500,
					Locale.US);
		}finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logger.error("Error while closing connection");
					e.printStackTrace();
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("Some I/O exception occurred while closing the Input Stream");
					e.printStackTrace();
				}
			}
		}

	}

}// End of class
*/
