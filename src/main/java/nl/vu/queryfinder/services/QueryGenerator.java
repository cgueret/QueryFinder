package nl.vu.queryfinder.services;

import java.util.Set;

import nl.vu.queryfinder.model.MappedQuery;

import com.hp.hpl.jena.query.Query;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public interface QueryGenerator {
	/**
	 * @param mappedQuery
	 * @return
	 * @throws Exception
	 */
	public Set<Query> getQuery(MappedQuery mappedQuery) throws Exception;

}
