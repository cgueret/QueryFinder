/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import nl.vu.queryfinder.model.Quad;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;

import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class ModelExpander extends Service {
	// Logger
	protected static final Logger logger = LoggerFactory.getLogger(ModelExpander.class);

	// Value factory
	protected final ValueFactory f = new ValueFactoryImpl();

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

		// Iterate over the data
		for (Quad quad : inputQuery.getTriples()) {
			// Keep the original quad
			outputQuery.addQuad(quad);

			// Turn S,P,O into O,P,S
			Quad inverseQuad = new Quad(quad.getObject(), quad.getPredicate(), quad.getSubject(), quad.getContext());
			outputQuery.addQuad(inverseQuad);

			// Turn a S,P,"blah" into S,P,?v + ?v,label,"blah"
			if (!quad.getObject().stringValue().startsWith("?")) {
				Literal v = f.createLiteral("?" + Integer.toHexString(quad.getObject().stringValue().hashCode()));
				Quad spv = new Quad(quad.getSubject(), quad.getPredicate(), v, quad.getContext());
				outputQuery.addQuad(spv);
				Quad vls = new Quad(v, RDFS.LABEL, quad.getObject(), quad.getContext());
				outputQuery.addQuad(vls);
			}
		}

		return outputQuery;
	}
}
