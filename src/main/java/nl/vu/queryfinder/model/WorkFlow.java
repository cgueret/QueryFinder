/**
 * 
 */
package nl.vu.queryfinder.model;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import nl.vu.queryfinder.services.ClassMatcher;
import nl.vu.queryfinder.services.PropertyMatcher;
import nl.vu.queryfinder.services.ResourceMatcher;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class WorkFlow {
	private ResourceMatcher resourceMatcher;
	private PropertyMatcher propertyMatcher;
	private ClassMatcher classMatcher;

	private enum Position {
		S, P, O
	};

	/**
	 * @param propertyMatcher
	 *           the propertyMatcher to set
	 */
	public void setPropertyMatcher(PropertyMatcher propertyMatcher) {
		this.propertyMatcher = propertyMatcher;
	}

	/**
	 * @return the propertyMatcher
	 */
	public PropertyMatcher getPropertyMatcher() {
		return propertyMatcher;
	}

	/**
	 * @param resourceMatcher
	 *           the resourceMatcher to set
	 */
	public void setResourceMatcher(ResourceMatcher resourceMatcher) {
		this.resourceMatcher = resourceMatcher;
	}

	/**
	 * @return the resourceMatcher
	 */
	public ResourceMatcher getResourceMatcher() {
		return resourceMatcher;
	}

	/**
	 * @param classMatcher
	 *           the classMatcher to set
	 */
	public void setClassMatcher(ClassMatcher classMatcher) {
		this.classMatcher = classMatcher;
	}

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
			Set<Node> subjects = getMappingsFor(Position.S, pattern.getSubject());
			Set<Node> predicates = getMappingsFor(Position.P, pattern.getPredicate());
			Set<Node> objects = getMappingsFor(Position.O, pattern.getObject());

			// Compose all the triples
			for (Node s : subjects)
				for (Node p : predicates)
					for (Node o : objects)
						System.out.println(Triple.create(s, p, o));
		}

		return mappedQuery;
	}

	/**
	 * @param position
	 * @param subject
	 * @return
	 */
	private Set<Node> getMappingsFor(Position position, Node node) {
		Set<Node> results = new HashSet<Node>();

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
				// TODO choice depend on predicate
				results.addAll(resourceMatcher.getResources(node.getLiteralLexicalForm()));
				results.addAll(classMatcher.getClasses(node.getLiteralLexicalForm()));
			}
		}

		return results;
	}

	/**
	 * @param query
	 */
	public void process(StructuredQuery query) {
		MappedQuery mappedQuery = getMappedQuery(query);
	}

}
