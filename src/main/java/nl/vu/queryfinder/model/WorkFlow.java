/**
 * 
 */
package nl.vu.queryfinder.model;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;

import nl.vu.queryfinder.services.ClassMatcher;
import nl.vu.queryfinder.services.MappedQueryGenerator;
import nl.vu.queryfinder.services.PropertyMatcher;
import nl.vu.queryfinder.services.QueryGenerator;
import nl.vu.queryfinder.services.ResourceMatcher;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class WorkFlow {
	static final Logger logger = LoggerFactory.getLogger(WorkFlow.class);
	private QueryGenerator queryGenerator;
	private ClassMatcher classMatcher;
	private PropertyMatcher propertyMatcher;
	private ResourceMatcher resourceMatcher;
	private MappedQueryGenerator mappedQueryGenerator;

	/**
	 * @param query
	 * @throws Exception
	 */
	public void process(StructuredQuery structuredQuery) throws Exception {
		MappedQuery mappedQuery = mappedQueryGenerator.getMappedQuery(structuredQuery, propertyMatcher, resourceMatcher,
				classMatcher);
		mappedQuery.printContent();
		Set<Query> sparqlQuerySet = queryGenerator.getQuery(mappedQuery);
		for (Query sparqlQuery : sparqlQuerySet)
			logger.info(sparqlQuery.serialize());
	}

	/**
	 * @return the classMatcher
	 */
	public ClassMatcher getClassMatcher() {
		return classMatcher;
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
	 * @return
	 */
	public MappedQueryGenerator getMappedQueryGenerator() {
		return mappedQueryGenerator;
	}

	/**
	 * @param classMatcher
	 *           the classMatcher to set
	 */
	public void setClassMatcher(ClassMatcher classMatcher) {
		this.classMatcher = classMatcher;
	}

	/**
	 * @param propertyMatcher
	 *           the propertyMatcher to set
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
	 *           the resourceMatcher to set
	 */
	public void setResourceMatcher(ResourceMatcher resourceMatcher) {
		this.resourceMatcher = resourceMatcher;
	}

	/**
	 * @param mappedQueryGenerator
	 */
	public void setMappedQueryGenerator(MappedQueryGenerator mappedQueryGenerator) {
		this.mappedQueryGenerator = mappedQueryGenerator;
	}
}
