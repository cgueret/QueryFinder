package nl.vu.queryfinder.services.impl;

import java.util.HashSet;
import java.util.Set;

import nl.erdf.datalayer.DataLayer;
import nl.vu.queryfinder.model.Quad;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AskFilter extends Service {
	// Logger
	protected static final Logger logger = LoggerFactory.getLogger(AskFilter.class);
	private DataLayer dataLayer;

	/**
	 * @param dataLayer
	 */
	public AskFilter(DataLayer dataLayer) {
		this.dataLayer = dataLayer;
	}

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
		outputQuery.setDescription(inputQuery.getDescription());

		// Iterate over the triples
		Set<Value> removeContexts = new HashSet<Value>();
		for (Quad quad : inputQuery.getQuads()) {
			Resource s = quad.getSubject().stringValue().startsWith("?") ? null : (Resource) quad.getSubject();
			URI p = quad.getPredicate().stringValue().startsWith("?") ? null : (URI) quad.getPredicate();
			Value o = quad.getObject().stringValue().startsWith("?") ? null : quad.getObject();
			nl.erdf.model.Triple t = new nl.erdf.model.Triple(s, p, o);

			// Test the triples with 0 or 1 variable
			if (t.getNumberNulls() > 1 || dataLayer.isValid(t)) {
				logger.info(t.toString());
				outputQuery.addQuad(quad);
			} else {
				removeContexts.add(quad.getContext());
			}
		}

		// Create the query
		Query outputQuery2 = new Query();
		outputQuery2.setDescription(inputQuery.getDescription());

		// Clean up blocks left alone
		for (Quad quad : outputQuery.getQuads()) {
			if (!removeContexts.contains(quad.getContext()))
				outputQuery2.addQuad(quad);
		}

		return outputQuery2;
	}
}
