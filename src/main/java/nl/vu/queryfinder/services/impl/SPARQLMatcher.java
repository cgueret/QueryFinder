package nl.vu.queryfinder.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.vu.queryfinder.model.Directory;
import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.EndPoint.EndPointType;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.model.Triple;
import nl.vu.queryfinder.services.Service;
import nl.vu.queryfinder.util.PaginatedQueryExec;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPARQLMatcher extends Service {
	// Logger
	private static final Logger logger = LoggerFactory.getLogger(SPARQLMatcher.class);

	// Property types ( RDF.PROPERTY )
	private static final Value[] PROP_TYPES = { RDF.PROPERTY, OWL.DATATYPEPROPERTY, OWL.OBJECTPROPERTY,
			OWL.FUNCTIONALPROPERTY };

	// The end point to query
	private final Map<EndPoint, PaginatedQueryExec> executors = new HashMap<EndPoint, PaginatedQueryExec>();

	/**
	 * @param endPoint
	 * @throws RepositoryException
	 */
	public SPARQLMatcher(Directory directory) throws RepositoryException {
		for (EndPoint endPoint : directory) {
			PaginatedQueryExec exec = new PaginatedQueryExec(endPoint);
			executors.put(endPoint, exec);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.Service#process(nl.vu.queryfinder.model.Query)
	 */
	@Override
	public Query process(Query inputQuery) {
		// Create the query
		Query outputQuery = new Query();

		// Copy the value of the description
		outputQuery.setDescription(inputQuery.getDescription());

		// Iterate over the triples
		for (Triple triple : inputQuery.getTriples()) {
			logger.info("Process " + triple);

			// Prepare a list of subjects
			List<Value> subjects = new ArrayList<Value>();
			Value subject = triple.getSubject();
			if (subject instanceof Resource) {
				subjects.add(subject);
			} else if (subject instanceof Literal) {
				if (subject.stringValue().startsWith("?")) {
					subjects.add(subject);
				} else {
					subjects.addAll(getResources(subject.stringValue(), null));
				}
			}

			// Prepare a list of predicates
			List<Value> predicates = new ArrayList<Value>();
			Value predicate = triple.getPredicate();
			if (predicate instanceof Resource) {
				predicates.add(predicate);
			} else if (predicate instanceof Literal) {
				if (predicate.stringValue().startsWith("?")) {
					predicates.add(predicate);
				} else {
					predicates.addAll(getProperties(predicate.stringValue()));
				}
			}

			// Prepare a list of objects
			List<Value> objects = new ArrayList<Value>();
			Value object = triple.getObject();
			if (object instanceof Resource) {
				objects.add(object);
			} else if (object instanceof Literal) {
				if (object.stringValue().startsWith("?")) {
					objects.add(object);
				} else {
					URI type = ((Literal) predicate).getDatatype();
					if (type != null && type.equals(new URIImpl(RDF.NAMESPACE + "PlainLiteral")))
						objects.add(object);
					else
						objects.addAll(getResources(object.stringValue(), null));
				}
			}

			// Do the cartesian product of the lists
			for (Value s : subjects) {
				for (Value p : predicates) {
					for (Value o : objects) {
						Triple t = new Triple(s, p, o);
						outputQuery.addTriple(t);
					}
				}
			}
		}

		return outputQuery;
	}

	/**
	 * @param keyword
	 * @return
	 */
	protected List<Value> getClasses(String keyword) {
		List<Value> results = new ArrayList<Value>();

		for (Entry<EndPoint, PaginatedQueryExec> entry : executors.entrySet()) {
			EndPoint endPoint = entry.getKey();
			PaginatedQueryExec executor = entry.getValue();

			// Build the query
			String query = "SELECT DISTINCT ?c ";
			if (endPoint.getDefaultGraph() != null)
				query += "FROM <" + endPoint.getDefaultGraph() + "> ";
			query += "WHERE {";
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
			results.addAll(executor.process(query, "c"));
		}

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
			for (Entry<EndPoint, PaginatedQueryExec> entry : executors.entrySet()) {
				EndPoint endPoint = entry.getKey();
				PaginatedQueryExec executor = entry.getValue();

				// Build the query
				String query = "SELECT DISTINCT ?c ";
				if (endPoint.getDefaultGraph() != null)
					query += "FROM <" + endPoint.getDefaultGraph() + "> ";
				query += "WHERE {";
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
				results.addAll(executor.process(query, "c"));
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
	protected List<Value> getResources(String keyword, Statement context) {
		List<Value> results = new ArrayList<Value>();

		for (Entry<EndPoint, PaginatedQueryExec> entry : executors.entrySet()) {
			EndPoint endPoint = entry.getKey();
			PaginatedQueryExec executor = entry.getValue();

			// Build the query
			String query = "SELECT DISTINCT ?c ";
			if (endPoint.getDefaultGraph() != null)
				query += "FROM <" + endPoint.getDefaultGraph() + "> ";
			query += "WHERE {";
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
			results.addAll(executor.process(query, "c"));
		}

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
		Directory directory = new Directory();
		EndPoint endPoint = new EndPoint("http://dbpedia.org/sparql", "http://dbpedia.org", EndPointType.VIRTUOSO);
		directory.add(endPoint);

		// EndPoint endPoint = new
		// EndPoint(URI.create("http://factforge.net/sparql"), null,
		// EndPointType.OWLIM);
		SPARQLMatcher me = new SPARQLMatcher(directory);
		List<Value> res = null;

		res = me.getClasses("artist");
		logger.info("artist      : " + res.size());
		for (Value v : res)
			logger.info(v.toString());

		res = me.getProperties("field");
		logger.info("field       : " + res.size());
		for (Value v : res)
			logger.info(v.toString());

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
	}
}
