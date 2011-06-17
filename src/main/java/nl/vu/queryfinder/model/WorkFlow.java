/**
 * 
 */
package nl.vu.queryfinder.model;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.vocabulary.RDF;

import nl.vu.queryfinder.services.ClassMatcher;
import nl.vu.queryfinder.services.PropertyMatcher;
import nl.vu.queryfinder.services.QueryGenerator;
import nl.vu.queryfinder.services.ResourceMatcher;
import nl.vu.queryfinder.util.TripleSet;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class WorkFlow {
	private enum Position {
		O, P, S
	}
	static final Logger logger = LoggerFactory.getLogger(WorkFlow.class);
	private ClassMatcher classMatcher;
	private PropertyMatcher propertyMatcher;
	private QueryGenerator queryGenerator;

	private ResourceMatcher resourceMatcher;;

	/**
	 * @return the classMatcher
	 */
	public ClassMatcher getClassMatcher() {
		return classMatcher;
	}

	/**
	 * @param query
	 */
	public MappedQuery getMappedQuery(StructuredQuery query) {
		MappedQuery mappedQuery = new MappedQuery();

		for (QueryPattern pattern : query) {
			// Get all the possible subjects, predicates and objects
			Set<Node> subjects = getMappingsFor(Position.S, pattern);
			Set<Node> predicates = getMappingsFor(Position.P, pattern);
			Set<Node> objects = getMappingsFor(Position.O, pattern);

			// Compose all the triples
			// TODO Check if a triple is valid before pushing it
			TripleSet triples = new TripleSet();
			triples.setPattern(pattern);
			for (Node s : subjects)
				for (Node p : predicates)
					for (Node o : objects)
						triples.add(Triple.create(s, p, o));

			// Add the group to the query
			mappedQuery.addGroup(triples);
		}

		return mappedQuery;
	}

	/**
	 * @param position
	 * @param subject
	 * @return
	 */
	private Set<Node> getMappingsFor(Position position, QueryPattern pattern) {
		Set<Node> results = new HashSet<Node>();
		Node node = null;

		if (position.equals(Position.S))
			node = pattern.getSubject();
		if (position.equals(Position.P))
			node = pattern.getPredicate();
		if (position.equals(Position.O))
			node = pattern.getObject();

		if (node.isVariable()) {
			results.add(node);
		}

		if (node.isURI()) {
			results.add(node);
		}

		if (node.isLiteral()) {
			if (position.equals(Position.S))
				results.addAll(resourceMatcher.getResources(node.getLiteralLexicalForm()));
			if (position.equals(Position.P))
				results.addAll(propertyMatcher.getProperties(node.getLiteralLexicalForm()));
			if (position.equals(Position.O)) {
				if (pattern.getPredicate().equals(RDF.type.asNode()))
					results.addAll(classMatcher.getClasses(node.getLiteralLexicalForm()));
				else
					results.addAll(resourceMatcher.getResources(node.getLiteralLexicalForm()));
			}
		}

		if (results.isEmpty())
			results.add(node);

		return results;
	}

	/**
	 * @return the propertyMatcher
	 */
	public PropertyMatcher getPropertyMatcher() {
		return propertyMatcher;
	}

	/**
	 * 
	 */
	public QueryGenerator getQueryGenerator() {
		return queryGenerator;
	}

	/**
	 * @return the resourceMatcher
	 */
	public ResourceMatcher getResourceMatcher() {
		return resourceMatcher;
	}

	/**
	 * @param query
	 * @throws Exception
	 */
	public void process(StructuredQuery structuredQuery) throws Exception {
		MappedQuery mappedQuery = getMappedQuery(structuredQuery);
		mappedQuery.printContent();
		Set<Query> sparqlQuerySet = queryGenerator.getQuery(mappedQuery);
		for (Query sparqlQuery : sparqlQuerySet)
			logger.info(sparqlQuery.serialize());
	}

	/**
	 * @param classMatcher
	 *            the classMatcher to set
	 */
	public void setClassMatcher(ClassMatcher classMatcher) {
		this.classMatcher = classMatcher;
	}

	/**
	 * @param propertyMatcher
	 *            the propertyMatcher to set
	 */
	public void setPropertyMatcher(PropertyMatcher propertyMatcher) {
		this.propertyMatcher = propertyMatcher;
	}

	/**
	 * @param queryGenerator
	 */
	public void setQueryGenerator(QueryGenerator queryGenerator) {
		this.queryGenerator = queryGenerator;
	}

	/**
	 * @param resourceMatcher
	 *            the resourceMatcher to set
	 */
	public void setResourceMatcher(ResourceMatcher resourceMatcher) {
		this.resourceMatcher = resourceMatcher;
	}
}
