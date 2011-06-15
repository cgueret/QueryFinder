package nl.vu.queryfinder;

import java.io.IOException;
import nl.vu.queryfinder.model.StructuredQuery;
import nl.vu.queryfinder.model.QueryPattern;
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
// Note : http://jena.sourceforge.net/ARQ/lucene-arq.html
// http://tech.groups.yahoo.com/group/jena-dev/message/39347
// work well : population, city, birth
public class QueryFinder {
	static final String END_POINT = "http://dbpedia.org/sparql";
	static final Logger logger = LoggerFactory.getLogger(QueryFinder.class);

	public static void main(String[] args) throws IOException {
		// Create the workflow
		WorkFlow workFlow = new WorkFlow();
		SPARQLMatcher matcher = new SPARQLMatcher(END_POINT);
		workFlow.setPropertyMatcher(matcher);
		workFlow.setClassMatcher(matcher);
		workFlow.setResourceMatcher(matcher);
		workFlow.setQueryGenerator(new IncrementalBuilder(END_POINT));
		
		// Create a query: We want to find someone who is an artist, that works in
		// some field, who is born in the Netherlands
		StructuredQuery query = new StructuredQuery();
		Node p = Node.createVariable("person");
		query.add(QueryPattern.create(p, RDF.type.asNode(), Node.createLiteral("artist")));
		query.add(QueryPattern.create(p, Node.createLiteral("field"), Node.createVariable("field")));
		query.add(QueryPattern.create(p, Node.createLiteral("birth"), Node.createLiteral("Netherlands")));

		// Go !
		workFlow.process(query);

		// Use the keyword matcher to expand the query keywords
		/*
		 * for (QueryPattern pattern : query) { // If the predicate is IS_A, the
		 * property is rdf:type and the object // must be a class if
		 * (pattern.getPredicate().equals(QueryPattern.IS_A)) { // Bind rdf:type
		 * query.addBinding(pattern.getPredicate(),
		 * Node.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		 * 
		 * // Find classes Set<Resource> types =
		 * keywordMatcher.getClasses(pattern.getObject()); for (Resource type :
		 * types) query.addBinding(pattern.getObject(), type); } // Otherwise,
		 * find suitable properties and resources else { // Find predicates
		 * Set<Resource> predicates =
		 * keywordMatcher.getProperties(pattern.getPredicate()); for (Resource
		 * predicate : predicates) query.addBinding(pattern.getPredicate(),
		 * predicate);
		 * 
		 * // Find resources if (!pattern.getObject().startsWith("?")) {
		 * Set<Resource> resources =
		 * sindiceSearch.getResources(pattern.getObject()); for (Resource resource
		 * : resources) query.addBinding(pattern.getObject(), resource); } } } ;
		 * 
		 * // Get the final statements Set<String> statements =
		 * query.getStatements(); logger.info(statements.size() + " statements");
		 * 
		 * // Check if they work Set<String> toRemove = new HashSet<String>(); for
		 * (String stmt : statements) if (!keywordMatcher.validates(stmt))
		 * toRemove.add(stmt); statements.removeAll(toRemove);
		 * 
		 * // Print the statements for (String stmt : statements)
		 * System.out.println(stmt);
		 */
	}
}
