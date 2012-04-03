package nl.vu.queryfinder.services;

import nl.vu.queryfinder.model.MappedQuery;
import nl.vu.queryfinder.model.StructuredQuery;

/**
 * @author cgueret
 *
 */
public interface MappedQueryGenerator {
	/**
	 * @param structuredQuery
	 * @param propertyMatcher
	 * @param resourceMatcher
	 * @param classMatcher
	 * @return
	 */
	public MappedQuery getMappedQuery(StructuredQuery structuredQuery, PropertyMatcher propertyMatcher,
			ResourceMatcher resourceMatcher, ClassMatcher classMatcher);
}
