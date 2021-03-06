package nl.vu.queryfinder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
public class QueryFinderSingle {
	static final Logger logger = LoggerFactory.getLogger(QueryFinderSingle.class);

	public static void main(String[] args) throws Exception {
		// Set an end point
		List<EndPoint> endPoints = new ArrayList<EndPoint>();
		EndPoint endPoint = new EndPoint(URI.create("http://dbpedia.org/sparql"), "http://dbpedia.org",
				EndPointType.VIRTUOSO);
		endPoints.add(endPoint);

		// Create the workflow
		WorkFlow workFlow = new WorkFlow();
		SPARQLMatcher matcher = new SPARQLMatcher(endPoints);
		workFlow.setPropertyMatcher(matcher);
		workFlow.setClassMatcher(matcher);
		workFlow.setResourceMatcher(matcher);
		workFlow.setQueryGenerator(new IncrementalBuilder(endPoint));

		// Create a query: We want to find someone who is an artist, that works
		// in some field, who is born in the Netherlands
		StructuredQuery query = new StructuredQuery();
		Node p = Node.createVariable("person");
		// query.add(QueryPattern.create(p, RDF.type.asNode(),
		// Node.createLiteral("artist")));
		// query.add(QueryPattern.create(p, Node.createLiteral("field"),
		// Node.createVariable("field")));
		// query.add(QueryPattern.create(p, Node.createLiteral("birth"),
		// Node.createLiteral("Netherlands")));

		Node a = Node.createVariable("album");
		query.add(QueryPattern.create(p, RDF.type.asNode(), Node.createLiteral("artist")));
		query.add(QueryPattern.create(p, Node.createLiteral("birth"), Node.createLiteral("New York")));
		query.add(QueryPattern.create(a, Node.createLiteral("artist"), p));
		query.add(QueryPattern.create(a, RDF.type.asNode(), Node.createLiteral("album")));
		query.add(QueryPattern.create(a, Node.createLiteral("genre"), Node.createLiteral("hip hop")));

		// Go !
		workFlow.process(query);
	}
}
