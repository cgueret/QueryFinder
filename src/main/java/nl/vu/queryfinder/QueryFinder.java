package nl.vu.queryfinder;

import java.io.IOException;

import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.EndPoint.EndPointType;
import nl.vu.queryfinder.model.QueryPattern;
import nl.vu.queryfinder.model.StructuredQuery;
import nl.vu.queryfinder.model.WorkFlow;
import nl.vu.queryfinder.services.impl.IncrementalBuilder;
import nl.vu.queryfinder.services.impl.SPARQLMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class QueryFinder {
	static final Logger logger = LoggerFactory.getLogger(QueryFinder.class);

	public static void main(String[] args) throws IOException {
		// Set an end point
		EndPoint endPoint = new EndPoint("http://dbpedia.org/sparql", "http://dbpedia.org", EndPointType.VIRTUOSO);

		// Create the workflow
		WorkFlow workFlow = new WorkFlow();
		SPARQLMatcher matcher = new SPARQLMatcher(endPoint);
		workFlow.setPropertyMatcher(matcher);
		workFlow.setClassMatcher(matcher);
		workFlow.setResourceMatcher(matcher);
		workFlow.setQueryGenerator(new IncrementalBuilder(endPoint));

		// Create a query: We want to find someone who is an artist, that works
		// in some field, who is born in the Netherlands
		StructuredQuery query = new StructuredQuery();
		Node p = Node.createVariable("person");
		query.add(QueryPattern.create(p, RDF.type.asNode(), Node.createLiteral("artist")));
		query.add(QueryPattern.create(p, Node.createLiteral("field"), Node.createVariable("field")));
		query.add(QueryPattern.create(p, Node.createLiteral("birth"), Node.createLiteral("Netherlands")));

		// Go !
		workFlow.process(query);
	}
}
