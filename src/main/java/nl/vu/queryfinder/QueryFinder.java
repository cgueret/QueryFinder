package nl.vu.queryfinder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.model.QueryPattern;
import nl.vu.queryfinder.services.impl.KeywordMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Hello world!
 * 
 */
// Note : http://jena.sourceforge.net/ARQ/lucene-arq.html
// http://tech.groups.yahoo.com/group/jena-dev/message/39347
// work well : population, city, birth
public class QueryFinder {
	static final Logger logger = LoggerFactory.getLogger(QueryFinder.class);

	public static void main(String[] args) throws IOException {
		// Create all the components
		Query query = new Query();
		KeywordMatcher keywordMatcher = new KeywordMatcher("http://dbpedia.org/sparql");

		// We want to find someone who is an artist
		query.add(QueryPattern.create("?person", QueryPattern.IS_A, "artist"));

		// that works in some field
		query.add(QueryPattern.create("?person", "field", "?field"));

		// who is born in the Netherlands
		query.add(QueryPattern.create("?person", "birth", "Netherlands"));

		// Use the keyword matcher to expand the query keywords
		for (QueryPattern pattern : query) {
			// If the predicate is IS_A, the property is rdf:type and the object
			// must be a class
			if (pattern.getPredicate().equals(QueryPattern.IS_A)) {
				// Bind rdf:type
				query.addBinding(pattern.getPredicate(),
						ResourceFactory.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));

				// Find classes
				Set<Resource> types = keywordMatcher.getClasses(pattern.getObject());
				for (Resource type : types)
					query.addBinding(pattern.getObject(), type);
			}
			// Otherwise, find suitable properties and resources
			else {
				// Find predicates
				Set<Resource> predicates = keywordMatcher.getProperties(pattern.getPredicate());
				for (Resource predicate : predicates)
					query.addBinding(pattern.getPredicate(), predicate);

				// Find resources
				if (!pattern.getObject().startsWith("?")) {
					Set<Resource> resources = keywordMatcher.getResources(pattern.getObject());
					for (Resource resource : resources)
						query.addBinding(pattern.getObject(), resource);
				}
			}
		}
		;

		// Get the final statements
		Set<String> statements = query.getStatements();
		logger.info(statements.size() + " statements");

		// Check if they work
		Set<String> toRemove = new HashSet<String>();
		for (String stmt : statements)
			if (!keywordMatcher.validates(stmt))
				toRemove.add(stmt);
		statements.removeAll(toRemove);

		// Print the statements
		for (String stmt : statements)
			System.out.println(stmt);
	}
}
