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

public class MasterDataCUDExt {
	final static Logger logr = LoggerFactory.getLogger("MasterDataCUDExt");

	// * This method encapsulates CREATE functionality for Customer entity.
	@ExtendDataProvider(entitySet = { "MODULE" }, requestTypes = RequestType.CREATE)
	public void createModule(ExtensionContext ecx) throws ODataApplicationException, ExtensionException {
		String MODULE_ID = null, MODULE_NAME = null;
		Connection conn = ((CDSDSParams) ecx.getDSParams()).getConnection();

		PreparedStatement ps = null;

		DataProviderExtensionContext extCtx = ecx.asDataProviderContext();
		DeserializerResult payload = extCtx.getDeserializerResult();
		// Get the entity
		Entity ent = payload.getEntity();

/*		// Entity contains a complex type 'CustAddress'. Read the complex type
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
*/
		// Get the value of other properties
		//int CustomerId = (Integer) ent.getProperty("CustomerID").getValue();
		String ModuleId = (String) ent.getProperty("MODULE_ID").getValue();
		String ModuleName = (String) ent.getProperty("MODULE_NAME").getValue();
		//boolean Premium = (Boolean) ent.getProperty("Premium").getValue();

		// Insert statement for Module table
		String psSQL = "INSERT INTO \"masterdata.MODULE\" VALUES (?,?)";
		try {
			ps = conn.prepareStatement(psSQL);

			ps.setString(1, ModuleId);
			ps.setString(2, ModuleName);
			ps.executeUpdate();

			extCtx.setEntityToBeRead();

		} catch (SQLException e) {
			e.printStackTrace();
			
			// Check if the SQL Exception was due to duplicate entry, based on
			// the error message thrown by HANA DB for unique constraint violation
			if (e.getLocalizedMessage().contains("unique constraint violated")) {
				throw new ODataApplicationException("Duplicate Resource", 400, Locale.US);
			} else {
				throw new ODataApplicationException("Some error occurred while creating module", 400, Locale.US);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Handling other generic exceptions
			throw new ODataApplicationException(
					"Some unknown error occurred while creating Module.Please contact admin", 500, Locale.US);
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

	}// End of method createModule
	
	@ExtendDataProvider(entitySet = { "MODULE" }, requestTypes = RequestType.UPDATE)
	public void updateModule(ExtensionContext ecx) throws ODataApplicationException {
		String moduleId = null, moduleName = null;
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
		moduleName = (String) ent.getProperty("MODULE_NAME").getValue();
		//prio = Boolean.parseBoolean(ent.getProperty("Premium").getValue().toString());

		//  For update operation, the key values will be taken from URI, not from the payload
		for (UriParameter key : keys) {
			if (key.getName().equals("MODULE_ID")) {
				String temp = key.getText();
				moduleId = temp.substring(1, temp.length() - 1);
			}
		}

		// Create SQL Statement for prepareStatement
		String sql = "UPDATE \"masterdata.MODULE\" SET \"MODULE_NAME\"=? WHERE " + "\"MODULE_ID\"=?";
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, moduleName);
			ps.setString(2, moduleId);
			int i = ps.executeUpdate();

			// If i==0, no rows were affected. This means there was no entity.
			// So throw 404 'Entity Not Found' exception
			if (i == 0) {
				throw new ODataApplicationException("Entity Not Found!", 404, Locale.US);
			}

			extCtx.setEntityToBeRead();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new ODataApplicationException("Some error occurred while updating module", 400, Locale.US);
		} catch (Exception e) {
			// Handling generic exceptions
			e.printStackTrace();
			throw new ODataApplicationException(
					"Some unknown error occurred while updating Module.Please contact admin", 500, Locale.US);
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
	} // End of updateModule method	
	
	@ExtendDataProvider(entitySet = { "MODULE" }, requestTypes = RequestType.DELETE)
	public void deleteModule(ExtensionContext ectx) throws ODataApplicationException {
		// Get DB connection
		Connection conn = ((CDSDSParams) ectx.getDSParams()).getConnection();
		String moduleId = null;
		PreparedStatement ps = null;
		DataProviderExtensionContext extCtx = ectx.asDataProviderContext();

		// Get URI info to obtain key predicates. No payload for delete
		UriInfo uri = extCtx.getUriInfo();
		UriResourceEntitySet entSet = (UriResourceEntitySet) uri.getUriResourceParts().get(0);
		List<UriParameter> keys = entSet.getKeyPredicates();
		for (UriParameter key : keys) {
			if (key.getName().equals("MODULE_ID")) {
				String temp = key.getText();
				moduleId = temp.substring(1, temp.length() - 1);
			}
		}
		// Prepare the SQL statement for prepareStatement
		String sql = "DELETE FROM \"masterdata.MODULE\" WHERE \"MODULE_ID\"=?";
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, moduleId);
			int i = ps.executeUpdate();
			
			//  If i==0, no rows were affected. This means there was no entity.
			//  So throw 404 'Entity Not Found' exception
			if (i == 0) {
				throw new ODataApplicationException("Entity not found!", 404, Locale.US);
			}

		} catch (SQLException e) {
			// Handle other SQL Exceptions
			e.printStackTrace();
			throw new ODataApplicationException("An error occurred while deleting Module.", 400, Locale.US);
		} catch (Exception e) {
			//Handling for other generic exceptions
			e.printStackTrace();
			throw new ODataApplicationException(
					"Some unknown error occurred while deleting Module.Please contact admin.", 500, Locale.US);
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

	} // End of method deleteModule
}