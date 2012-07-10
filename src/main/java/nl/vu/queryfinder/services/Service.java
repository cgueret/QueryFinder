/**
 * 
 */
package nl.vu.queryfinder.services;

import java.util.HashMap;
import java.util.Map;

import nl.erdf.datalayer.DataLayer;
import nl.erdf.datalayer.hbase.SeverHBaseDataLayer;
import nl.erdf.datalayer.hbase.SpyrosHBaseDataLayer;
import nl.erdf.datalayer.sparql.SPARQLDataLayer;
import nl.erdf.model.Directory;
import nl.vu.datalayer.hbase.connection.HBaseConnection;
import nl.vu.queryfinder.model.Query;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public abstract class Service {
	/**
	 * List of parameters
	 */
	protected final Map<String, String> parameters = new HashMap<String, String>();

	/**
	 * The datalayer to be used to fetch RDF data
	 */
	protected DataLayer dataLayer = null;

	/**
	 * The SPARQL end points that may be queried for resources
	 */
	private Directory directory = null;

	/**
	 * @param dataLayer
	 */
	public void setDataLayer(DataLayer dataLayer) {
		this.dataLayer = dataLayer;
	}

	/**
	 * Set a parameter
	 * 
	 * @param key
	 *            the name of the parameter
	 * @param value
	 *            the value of the parameter
	 */
	public void setParameter(String key, String value) {
		parameters.put(key, value);
	}

	/**
	 * Get a parameter
	 * 
	 * @param key
	 *            the name of the parameter
	 * @return the value of the parameter
	 */
	public String getParameter(String key) {
		return parameters.get(key);
	}

	/**
	 * Process the statements of an input query
	 * 
	 * @param inputQuery
	 *            the input query to process
	 * @return a new query
	 */
	public abstract Query process(Query inputQuery);

	/**
	 * 
	 */
	public void configure() {
		String dataLayerName = this.getParameter("datalayer");
		if (dataLayerName.equals("spyros"))
			dataLayer = SpyrosHBaseDataLayer.getInstance("default");
		else if (dataLayerName.equals("sever_remote"))
			dataLayer = new SeverHBaseDataLayer(HBaseConnection.REST);
		else if (dataLayerName.equals("sever_local"))
			dataLayer = new SeverHBaseDataLayer(HBaseConnection.NATIVE_JAVA);
		else if (dataLayerName.equals("sparql"))
			dataLayer = new SPARQLDataLayer(null);
		else
			dataLayer = null;
	}

	/**
	 * @return the directory
	 */
	public Directory getDirectory() {
		return directory;
	}
}
