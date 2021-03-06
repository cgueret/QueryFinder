package nl.vu.queryfinder.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.erdf.model.Directory;
import nl.erdf.model.EndPoint;
import nl.erdf.model.EndPoint.EndPointType;
import nl.vu.queryfinder.model.Quad;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;
import nl.vu.queryfinder.util.SelectQueryExecutor;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * @see http 
 *      ://www.openlinksw.com/dataspace/dav/wiki/Main/VirtuosoFacetsViewsLinkedData
 */
public class SPARQLMatcher extends Service {
	// Logger
	private static final Logger logger = LoggerFactory
			.getLogger(SPARQLMatcher.class);

	// Property types ( RDF.PROPERTY )
	private static final Value[] PROP_TYPES = { OWL.DATATYPEPROPERTY,
			OWL.OBJECTPROPERTY, OWL.FUNCTIONALPROPERTY };

	// List of end points to query
	private static final Map<EndPoint, SelectQueryExecutor> executors = new HashMap<EndPoint, SelectQueryExecutor>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.Service#process(nl.vu.queryfinder.model.Query)
	 */
	@Override
	public Query process(Query inputQuery) {
		// The end points to query
		for (EndPoint endPoint : directory) {
			if (endPoint.getType().equals(EndPointType.VIRTUOSO)
					|| endPoint.getType().equals(EndPointType.OWLIM)) {
				try {
					SelectQueryExecutor exec = new SelectQueryExecutor(endPoint);
					executors.put(endPoint, exec);
				} catch (RepositoryException e) {
					e.printStackTrace();
				}
			}
		}

		// Create the query
		Query outputQuery = new Query();

		// Copy the value of the description
		outputQuery.setDescription(inputQuery.getDescription());

		// Iterate over the triples
		for (Quad quad : inputQuery.getQuads()) {
			logger.info(quad.toString());

			// Prepare a list of subjects
			List<Value> subjects = new ArrayList<Value>();
			Value subject = quad.getSubject();
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
			Value predicate = quad.getPredicate();
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
			Value object = quad.getObject();
			if (object instanceof Resource) {
				objects.add(object);
			} else if (object instanceof Literal) {
				if (object.stringValue().startsWith("?")) {
					objects.add(object);
				} else {
					if (predicate instanceof org.openrdf.sail.memory.model.MemURI) {
						// URI type = ((Literal) predicate).getDatatype();
						// if (type != null && type.equals(new
						// URIImpl(RDF.NAMESPACE + "PlainLiteral"))) {
						objects.add(object);
					} else {
						if (predicate instanceof Literal
								&& predicate.stringValue().equals("type"))
							objects.addAll(getClasses(object.stringValue()));
						else if (predicate instanceof Resource
								&& predicate.equals(RDF.TYPE))
							objects.addAll(getClasses(object.stringValue()));
						else
							objects.addAll(getResources(object.stringValue(),
									null));
					}
				}
			}

			// Do the cartesian product of the lists
			for (Value s : subjects) {
				for (Value p : predicates) {
					for (Value o : objects) {
						Quad t = new Quad(s, p, o, quad.getContext());
						outputQuery.addQuad(t);
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
	protected List<Value> getClasses(final String keyword) {
		List<Value> results = new ArrayList<Value>();
		String keyword2 = keyword;

		for (Entry<EndPoint, SelectQueryExecutor> entry : executors.entrySet()) {
			EndPoint endPoint = entry.getKey();
			SelectQueryExecutor executor = entry.getValue();

			// Build the query
			String query = "SELECT DISTINCT ?c ";
			if (endPoint.getDefaultGraph() != null)
				query += "FROM <" + endPoint.getDefaultGraph() + "> ";
			query += "WHERE {";
			query += "?c a <http://www.w3.org/2002/07/owl#Class>.";
			if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
				query += "?c <http://www.w3.org/2000/01/rdf-schema#label> ?l.";
				query += "?l bif:contains 'KEYWORD'.} ORDER BY DESC ( <LONG::IRI_RANK> (?c) )";
				keyword2 = keyword.replace(" ", " and ");
			}
			if (endPoint.getType().equals(EndPointType.OWLIM)) {
				// query +=
				// "?c <http://www.ontotext.com/owlim/lucene#> 'KEYWORD'.}";
				query += "?c <http://www.w3.org/2000/01/rdf-schema#label> ?l.";
				query += "FILTER regex(str(?l), 'KEYWORD', 'i') }";
			}
			query = query.replace("KEYWORD", keyword2);
			logger.info(query);

			// Process the query
			results.addAll(executor.process(query, "c"));
		}

		logger.info(String.format("[class] \"%s\" -> %d", keyword,
				results.size()));

		for (Value l : results)
			logger.info("\t" + l.stringValue());

		return results;
	}

	/**
	 * @param keyword
	 * @return
	 */
	protected List<Value> getProperties(final String keyword) {
		List<Value> results = new ArrayList<Value>();
		String keyword2 = keyword;
		
		if (!keyword.equals("type")) {
			for (Value propertyType : PROP_TYPES) {
				for (Entry<EndPoint, SelectQueryExecutor> entry : executors
						.entrySet()) {
					EndPoint endPoint = entry.getKey();
					SelectQueryExecutor executor = entry.getValue();

					// Build the query
					String query = "SELECT DISTINCT ?c ";
					if (endPoint.getDefaultGraph() != null)
						query += "FROM <" + endPoint.getDefaultGraph() + "> ";
					query += "WHERE {";
					query += "?c a <" + propertyType + ">.";
					if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
						query += "?c <http://www.w3.org/2000/01/rdf-schema#label> ?l.";
						query += "?l bif:contains 'KEYWORD'.} ORDER BY DESC ( <LONG::IRI_RANK> (?c) )";
						keyword2 = keyword.replace(" ", " and ");
					}
					if (endPoint.getType().equals(EndPointType.OWLIM)) {
						query += "?c <http://www.ontotext.com/owlim/lucene#> 'KEYWORD'.}";
					}
					query = query.replace("KEYWORD", keyword2);
					logger.info(query);

					// Process the query
					results.addAll(executor.process(query, "c"));
				}
			}
		} else {
			results.add(RDF.TYPE);
		}

		logger.info(String.format("[property] \"%s\" -> %d", keyword,
				results.size()));
		for (Value l : results)
			logger.info("\t" + l.stringValue());

		return results;
	}

	/**
	 * @param keyword
	 * @param context
	 * @return
	 */
	protected List<Value> getResources(final String keyword,
			final Statement context) {
		List<Value> results = new ArrayList<Value>();
		String keyword2 = keyword;

		for (Entry<EndPoint, SelectQueryExecutor> entry : executors.entrySet()) {
			EndPoint endPoint = entry.getKey();
			SelectQueryExecutor executor = entry.getValue();

			// Build the query
			String query = "SELECT DISTINCT ?c ";
			if (endPoint.getDefaultGraph() != null)
				query += "FROM <" + endPoint.getDefaultGraph() + "> ";
			query += "WHERE {";
			// query += "?c a <" + OWL.NAMESPACE + "Thing>.";
			if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
				query += "?c <http://www.w3.org/2000/01/rdf-schema#label> ?l.";
				query += "?l bif:contains 'KEYWORD'.} ORDER BY DESC ( <LONG::IRI_RANK> (?c) )";
				keyword2 = keyword.replace(" ", " and ");
			}
			if (endPoint.getType().equals(EndPointType.OWLIM)) {
				// query +=
				// "?c <http://www.ontotext.com/owlim/lucene#> 'KEYWORD'.}";
				query += "?c <http://www.w3.org/2000/01/rdf-schema#label> ?l.";
				query += "FILTER regex(str(?l), 'KEYWORD', 'i') }";
			}
			query = query.replace("KEYWORD", keyword2);
			logger.info(query);

			// Process the query
			results.addAll(executor.process(query, "c"));
		}

		if (context != null)
			logger.info(String.format("[resource] \"%s\" -> %d (%s)", keyword,
					results.size(), context.getPredicate()));
		else
			logger.info(String.format("[resource] \"%s\" -> %d", keyword,
					results.size()));
		for (Value l : results)
			logger.info("\t" + l.stringValue());

		return results;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws RepositoryException
	 */
	public static void main(String[] args) throws IOException,
			RepositoryException {
		Directory directory = new Directory();
		EndPoint endPoint = new EndPoint("http://dbpedia.org/sparql",
				"http://dbpedia.org", EndPointType.VIRTUOSO);
		directory.add(endPoint);

		// EndPoint endPoint = new
		// EndPoint(URI.create("http://factforge.net/sparql"), null,
		// EndPointType.OWLIM);
		SPARQLMatcher me = new SPARQLMatcher();
		me.setDirectory(directory);
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
