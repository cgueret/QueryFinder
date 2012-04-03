/**
 * 
 */
package nl.vu.queryfinder.services;

import java.util.HashMap;
import java.util.Map;

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
}
