package nl.vu.queryfinder.services.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.EndPoint;
import nl.vu.queryfinder.services.EndPoint.EndPointType;
import nl.vu.queryfinder.services.Service;
import nl.vu.queryfinder.util.PaginatedQueryExec;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPARQLMatcher extends Service {
	// Logger
	private static final Logger logger = LoggerFactory.getLogger(SPARQLMatcher.class);

	// Property types ( RDF.PROPERTY )
	private static final Value[] PROP_TYPES = { OWL.DATATYPEPROPERTY, OWL.OBJECTPROPERTY,
			OWL.FUNCTIONALPROPERTY };

	// The end point to query
	private final PaginatedQueryExec exec;
	private final EndPoint endPoint;

	/**
	 * @param endPoint
	 * @throws RepositoryException
	 */
	public SPARQLMatcher(EndPoint endPoint) throws RepositoryException {
		// Connect to the end point
		this.endPoint = endPoint;
		exec = new PaginatedQueryExec(endPoint);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.Service#process(nl.vu.queryfinder.model.Query)
	 */
	@Override
	public Query process(Query inputQuery) {
		return inputQuery;
	}

	/**
	 * @param keyword
	 * @return
	 */
	protected List<Value> getClasses(String keyword) {
		List<Value> results = new ArrayList<Value>();

		// Build the query
		String query = "SELECT DISTINCT ?c WHERE {";
		query += "?c a <http://www.w3.org/2002/07/owl#Class>.";
		if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
			query += "?c <http://www.w3.org/2000/01/rdf-schema#label> ?l.";
			query += "?l bif:contains 'KEYWORD'.} ORDER BY DESC ( <LONG::IRI_RANK> (?c) )";
			keyword = keyword.replace(" ", " and ");
		}
		if (endPoint.getType().equals(EndPointType.OWLIM)) {
			query += "?c <http://www.ontotext.com/owlim/lucene#> 'KEYWORD'.}";
		}
		query = query.replace("KEYWORD", keyword);

		// Process the query
		results.addAll(exec.process(query, "c"));

		logger.info(String.format("[class] \"%s\" -> %d", keyword, results.size()));

		return results;
	}

	/**
	 * @param keyword
	 * @return
	 */
	protected List<Value> getProperties(String keyword) {
		List<Value> results = new ArrayList<Value>();

		for (Value propertyType : PROP_TYPES) {
			// Build the query
			String query = "SELECT DISTINCT ?c WHERE {";
			query += "?c a <" + propertyType + ">.";
			if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
				query += "?c <http://www.w3.org/2000/01/rdf-schema#label> ?l.";
				query += "?l bif:contains 'KEYWORD'.} ORDER BY DESC ( <LONG::IRI_RANK> (?c) )";
				keyword = keyword.replace(" ", " and ");
			}
			if (endPoint.getType().equals(EndPointType.OWLIM)) {
				query += "?c <http://www.ontotext.com/owlim/lucene#> 'KEYWORD'.}";
			}
			query = query.replace("KEYWORD", keyword);

			// Process the query
			results.addAll(exec.process(query, "c"));
		}

		logger.info(String.format("[property] \"%s\" -> %d", keyword, results.size()));

		return results;

	}

	/**
	 * @param keyword
	 * @param context
	 * @return
	 */
	protected List<Value> getResources(String keyword, Statement context) {
		List<Value> results = new ArrayList<Value>();

		// Connect to the end point
		PaginatedQueryExec exec = null;
		try {
			exec = new PaginatedQueryExec(endPoint);
		} catch (RepositoryException e) {
			e.printStackTrace();
			return results;
		}

		// Build the query
		String query = "SELECT DISTINCT ?c WHERE {";
		query += "?c a <" + OWL.NAMESPACE + "Thing>.";
		if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
			query += "?c <http://www.w3.org/2000/01/rdf-schema#label> ?l.";
			query += "?l bif:contains 'KEYWORD'.} ORDER BY DESC ( <LONG::IRI_RANK> (?c) )";
			keyword = keyword.replace(" ", " and ");
		}
		if (endPoint.getType().equals(EndPointType.OWLIM)) {
			query += "?c <http://www.ontotext.com/owlim/lucene#> 'KEYWORD'.}";
		}
		query = query.replace("KEYWORD", keyword);

		// Process the query
		results.addAll(exec.process(query, "c"));

		if (context != null)
			logger.info(String.format("[resource] \"%s\" -> %d (%s)", keyword, results.size(), context.getPredicate()));
		else
			logger.info(String.format("[resource] \"%s\" -> %d", keyword, results.size()));

		return results;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws RepositoryException
	 */
	public static void main(String[] args) throws IOException, RepositoryException {
		EndPoint endPoint = new EndPoint(URI.create("http://dbpedia.org/sparql"), "http://dbpedia.org",
				EndPointType.VIRTUOSO);

		// EndPoint endPoint = new
		// EndPoint(URI.create("http://factforge.net/sparql"), null,
		// EndPointType.OWLIM);
		SPARQLMatcher me = new SPARQLMatcher(endPoint);
		List<Value> res = null;

		res = me.getClasses("artist");
		logger.info("artist      : " + res.size());
		for (Value v : res)
			logger.info(v.toString());

		res = me.getProperties("field");
		logger.info("field       : " + res.size());
		for (Value v : res)
			logger.info(v.toString());

		/*
		res = me.getProperties("birth");
		logger.info("birth       : " + res.size());
		for (Value v : res)
			logger.info(v.toString());

		res = me.getResources("amsterdam", null);
		logger.info("amsterdam   : " + res.size());
		for (Value v : res)
			logger.info(v.toString());
		
		res = me.getResources("Netherlands", null);
		logger.info("Netherlands  : " + res.size());
		for (Value v : res)
			logger.info(v.toString());

		res = me.getResources("hip hop", null);
		logger.info("hip hop      : " + res.size());
		for (Value v : res)
			logger.info(v.toString());
			*/
	}
}
