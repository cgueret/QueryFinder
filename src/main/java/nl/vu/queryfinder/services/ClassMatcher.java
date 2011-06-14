/**
 * 
 */
package nl.vu.queryfinder.services;

import java.util.Set;

import com.hp.hpl.jena.graph.Node;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public interface ClassMatcher {
	/**
	 * Find a set of properties matching a particular keyword
	 * 
	 * @param keyword
	 *            the input keyword
	 * @return a set of classes
	 */
	public Set<Node> getClasses(String keyword);
}
