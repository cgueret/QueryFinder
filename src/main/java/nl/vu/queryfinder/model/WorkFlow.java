/**
 * 
 */
package nl.vu.queryfinder.model;

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

	/**
	 * @param propertyMatcher
	 *            the propertyMatcher to set
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
	 *            the resourceMatcher to set
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
	 *            the classMatcher to set
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

}
