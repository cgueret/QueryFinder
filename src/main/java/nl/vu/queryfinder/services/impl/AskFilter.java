package nl.vu.queryfinder.services.impl;

import nl.erdf.datalayer.hbase.NativeHBaseDataLayer;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.model.Triple;
import nl.vu.queryfinder.services.Service;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AskFilter extends Service {
	// Logger
	protected static final Logger logger = LoggerFactory.getLogger(AskFilter.class);

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

		NativeHBaseDataLayer d = new NativeHBaseDataLayer();

		// Iterate over the triples
		for (Triple triple : inputQuery.getTriples()) {
			Resource s = triple.getSubject().stringValue().startsWith("?") ? null : (Resource) triple.getSubject();
			URI p = triple.getPredicate().stringValue().startsWith("?") ? null : (URI) triple.getPredicate();
			Value o = triple.getObject().stringValue().startsWith("?") ? null : triple.getObject();
			nl.erdf.model.Triple t = new nl.erdf.model.Triple(s, p, o);

			// Test the triples with 0 or 1 variable
			if (t.getNumberNulls() > 1 || d.isValid(t)) {
				logger.info(t.toString());
				outputQuery.addTriple(triple);
			}
		}

		return outputQuery;
	}
}
