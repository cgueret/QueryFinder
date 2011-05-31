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
public interface ResourceMatcher {
	/**
	 * Find a set of resources matching a particular keyword
	 * 
	 * @param keyword
	 *            the input keyword
	 * @return a set of resources
	 */
	public Set<Resource> getResources(String keyword);
}
