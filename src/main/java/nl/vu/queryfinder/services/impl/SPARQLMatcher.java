package nl.vu.queryfinder.services.impl;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.EndPoint;
import nl.vu.queryfinder.services.EndPoint.EndPointType;
import nl.vu.queryfinder.services.Service;
import nl.vu.queryfinder.util.PaginatedQueryExec;

import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPARQLMatcher extends Service {
	// Logger
	private static final Logger logger = LoggerFactory.getLogger(SPARQLMatcher.class);

	// Property types
	private static final Value[] propertyTypes = { RDF.PROPERTY, OWL.DATATYPEPROPERTY, OWL.OBJECTPROPERTY };

	// The end point to query
	private EndPoint endPoint;

	/**
	 * @param endPoint
	 */
	public SPARQLMatcher(EndPoint endPoint) {
		this.endPoint = endPoint;
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
	private Set<Value> getClasses(String keyword) {
		Set<Value> results = new HashSet<Value>();

		// Build the query
		String query = "SELECT DISTINCT ?c WHERE {";
		query += "?c a <http://www.w3.org/2002/07/owl#Class>.";
		if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
			query += "?c <http://www.w3.org/2000/01/rdf-schema#label> ?l.";
			query += "?l bif:contains 'KEYWORD'.";
		}
		if (endPoint.getType().equals(EndPointType.OWLIM)) {
			query += "?c <http://www.ontotext.com/owlim/lucene#> 'KEYWORD'.";
		}
		query += "}";
		query.replace("KEYWORD", keyword);

		// Process the query
		results.addAll(PaginatedQueryExec.process(endPoint, query, "c"));

		logger.info(String.format("[class] \"%s\" -> %d", keyword, results.size()));

		return results;
	}

	/**
	 * @param keyword
	 * @return
	 */
	private Set<Value> getProperties(String keyword) {
		Set<Value> results = new HashSet<Value>();

		for (EndPoint endPoint : endPoints) {
			Value var = Value.createVariable("r");
			Value label = Value.createVariable("l");
			Query query = QueryFactory.create();
			query.setQuerySelectType();
			query.setDistinct(true);
			query.addResultVar(var);

			for (Value propertyType : propertyTypes) {
				ElementGroup group = new ElementGroup();
				group.addTriplePattern(new Triple(var, RDF.type.asValue(), propertyType));
				if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
					String text = "'" + keyword + "'";
					group.addTriplePattern(new Triple(var, RDFS.label.asValue(), label));
					group.addTriplePattern(new Triple(label, Value.createURI("bif:contains"), Value.createLiteral(text)));
				} else if (endPoint.getType().equals(EndPointType.OWLIM)) {
					group.addTriplePattern(new Triple(var, Value.createURI("http://www.ontotext.com/owlim/lucene#"),
							Value.createLiteral(keyword)));
				}
				query.setQueryPattern(group);
				results.addAll(PaginatedQueryExec.process(endPoint, query, var));
			}
		}

		logger.info(String.format("[property] \"%s\" -> %d", keyword, results.size()));

		return results;

	}

	/**
	 * @param keyword
	 * @param context
	 * @return
	 */
	private Set<Value> getResources(String keyword, Triple context) {
		Set<Value> results = new HashSet<Value>();

		for (EndPoint endPoint : endPoints) {
			Value var = Value.createVariable("r");
			Value label = Value.createVariable("l");
			Query query = QueryFactory.create();
			query.setQuerySelectType();
			query.setDistinct(true);
			query.addResultVar(var);
			ElementGroup group = new ElementGroup();
			group.addTriplePattern(new Triple(var, RDF.type.asValue(), Value.createAnon()));
			if (context != null)
				group.addTriplePattern(context);
			if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
				// String text = StringUtils.join(keyword.split(" "), " and ");
				String text = "'" + keyword + "'";
				group.addTriplePattern(new Triple(var, RDFS.label.asValue(), label));
				group.addTriplePattern(new Triple(label, Value.createURI("bif:contains"), Value.createLiteral(text)));
			} else if (endPoint.getType().equals(EndPointType.OWLIM)) {
				group.addTriplePattern(new Triple(var, Value.createURI("http://www.ontotext.com/owlim/lucene#"), Value
						.createLiteral(keyword)));
			}
			query.setQueryPattern(group);
			results.addAll(PaginatedQueryExec.process(endPoint, query, var));
		}

		logger.info(String.format("[resource] \"%s\" -> %d (%s)", keyword, results.size(), context.getPredicate()));

		return results;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// new EndPoint("http://dbpedia.org/sparql", "http://dbpedia.org",
		// EndPointType.VIRTUOSO);

		EndPoint endPoint = new EndPoint(URI.create("http://factforge.net/sparql"), null, EndPointType.OWLIM);
		SPARQLMatcher me = new SPARQLMatcher(endPoint);
		logger.info("artist      : " + me.getClasses("artist").size());
		logger.info("field       : " + me.getProperties("field").size());
		logger.info("birth       : " + me.getProperties("birth").size());
		logger.info("amsterdam   : " + me.getResources("amsterdam", null).size());
		logger.info("Netherlands : " + me.getResources("Netherlands", null).size());
	}
}
