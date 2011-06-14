/**
 * 
 */
package nl.vu.queryfinder.services;

import nl.vu.queryfinder.model.StructuredQuery;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public interface NLParser {
	/**
	 * Generate a query, which is a set of query patterns, corresponding to the
	 * full text description
	 * 
	 * @param description
	 *            a textual description of the information need
	 * 
	 * @return an instance of Query
	 */
	public StructuredQuery getQuery(String description);
}
