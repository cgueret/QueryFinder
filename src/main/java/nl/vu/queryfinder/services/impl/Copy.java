/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.model.Triple;
import nl.vu.queryfinder.services.Service;

/**
 * This service creates a verbatim copy of the content of the input query
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Copy extends Service {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.Service#process(nl.vu.queryfinder.model.Query)
	 */
	@Override
	public Query process(Query inputQuery) {
		// Create the query
		Query outputQuery = new Query();

		// Copy the value of the description
		outputQuery.setDescription(inputQuery.getDescription());

		// Copy the triples
		for (Triple triple : inputQuery.getTriples())
			outputQuery.addTriple(triple);

		return outputQuery;
	}

}
