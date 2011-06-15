/**
 * 
 */
package nl.vu.queryfinder.services;

import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import nl.vu.queryfinder.model.Mappings;
import nl.vu.queryfinder.model.StructuredQuery;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public interface RedundancySetGenerator {
	/**
	 * Generate a set of triples from the combination of the query patterns and
	 * the mappings for their keywords
	 * 
	 * @param query
	 *            the set of query patterns
	 * @param mappings
	 *            the mappings for the keywords
	 * @return a set of triples
	 */
	public Set<Triple> getRedundancySet(StructuredQuery query, Mappings mappings);
}
