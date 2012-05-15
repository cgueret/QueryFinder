package nl.vu.queryfinder.services.impl;

import nl.erdf.datalayer.hbase.NativeHBaseDataLayer;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.model.Quad;
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
		for (Quad quad : inputQuery.getTriples()) {
			Resource s = quad.getSubject().stringValue().startsWith("?") ? null : (Resource) quad.getSubject();
			URI p = quad.getPredicate().stringValue().startsWith("?") ? null : (URI) quad.getPredicate();
			Value o = quad.getObject().stringValue().startsWith("?") ? null : quad.getObject();
			nl.erdf.model.Triple t = new nl.erdf.model.Triple(s, p, o);

			// Test the triples with 0 or 1 variable
			if (t.getNumberNulls() > 1 || d.isValid(t)) {
				logger.info(t.toString());
				outputQuery.addQuad(quad);
			}
		}

		return outputQuery;
	}
}
