package nl.vu.queryfinder.services;

import java.util.Set;

import nl.vu.queryfinder.model.MappedQuery;

import com.hp.hpl.jena.query.Query;

public interface QueryGenerator {
	public Set<Query> getQuery(MappedQuery mappedQuery);
	
}
