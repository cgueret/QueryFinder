/**
 * 
 */
package nl.vu.queryfinder.services;

import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public interface PropertyMatcher {
	/**
	 * Find a set of properties matching a particular keyword
	 * 
	 * @param keyword
	 *            the input keyword
	 * @return a set of properties
	 */
	public Set<Resource> getProperties(String keyword);
}
