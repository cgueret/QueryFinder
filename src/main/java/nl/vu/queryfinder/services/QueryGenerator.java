package nl.vu.queryfinder.services;

import nl.vu.queryfinder.model.MappedQuery;

import com.hp.hpl.jena.query.Query;

public interface QueryGenerator {
	public Query getQuery(MappedQuery mappedQuery);
	
}
