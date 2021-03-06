/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import nl.vu.queryfinder.model.Quad;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;

import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class ModelExpander extends Service {
	// Logger
	protected static final Logger logger = LoggerFactory
			.getLogger(ModelExpander.class);

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
		for (Quad quad : inputQuery.getQuads()) {
			// Keep the original quad
			outputQuery.addQuad(quad);

			// Turn ?S,P,?O into ?O,P,?S
			/*
			 * if (quad.getSubject().stringValue().startsWith("?") &&
			 * quad.getObject().stringValue().startsWith("?")) { Quad
			 * inverseQuad = new Quad(quad.getObject(), quad.getPredicate(),
			 * quad.getSubject(), quad.getContext());
			 * outputQuery.addQuad(inverseQuad); }
			 */

			// Turn a S,P,"blah" into S,P,?v + ?v,label,"blah"
			if (!quad.getObject().stringValue().startsWith("?")) {
				Literal context = f.createLiteral(Integer.toString(quad
						.hashCode()));
				Literal v = f.createLiteral("?"
						+ Integer.toHexString(quad.getObject().stringValue()
								.hashCode()));
				Quad spv = new Quad(quad.getSubject(), quad.getPredicate(), v,
						context);
				outputQuery.addQuad(spv);
				Literal object = f.createLiteral(
						quad.getObject().stringValue(), new URIImpl(
								RDF.NAMESPACE + "PlainLiteral"));
				Quad vls = new Quad(v, RDFS.LABEL, object, context);
				outputQuery.addQuad(vls);
			}

			// Express events
			// S,"birth|death",O -> ?E,a,Event + ?E,involvedAgent,S + ?E,label,O
			/*
			 * if (quad.getPredicate().stringValue().contains("birth") ||
			 * quad.getPredicate().stringValue().contains("death")) { Literal
			 * context = f.createLiteral(Integer.toString(quad.hashCode()));
			 * Literal event = f.createLiteral("?" +
			 * Integer.toHexString(quad.getObject().stringValue().hashCode()));
			 * Quad quadEvent = new Quad(event, RDF.TYPE,
			 * f.createURI("http://linkedevents.org/ontology/Event"), context);
			 * Quad quadAgent = new Quad(event,
			 * f.createURI("http://linkedevents.org/ontology/involvedAgent"),
			 * quad.getSubject(), context); Quad quadLabel = new Quad(event,
			 * RDFS.LABEL, quad.getObject(), context);
			 * outputQuery.addQuad(quadEvent); outputQuery.addQuad(quadAgent);
			 * outputQuery.addQuad(quadLabel); }
			 */
		}

		return outputQuery;
	}
}
