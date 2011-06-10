package nl.vu.queryfinder.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import nl.vu.queryfinder.services.ClassMatcher;
import nl.vu.queryfinder.services.PropertyMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author cgueret
 * 
 */
// ResultSetFormatter.out(System.out, resultSet, query);
public class KeywordMatcher implements ClassMatcher, PropertyMatcher {
	static final Logger logger = LoggerFactory.getLogger(KeywordMatcher.class);
	private final String endPoint;

	/**
	 * @param string
	 */
	public KeywordMatcher(String endPoint) {
		this.endPoint = endPoint;
	}

	/**
	 * @param queryString
	 * @return
	 * @throws IOException
	 */
	private Set<Resource> execQuery(String queryString) throws IOException {
		Set<Resource> properties = new HashSet<Resource>();

		// Get all the properties
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(endPoint, query);
		ResultSet resultSet = qexec.execSelect();
		if (resultSet.hasNext()) {
			for (QuerySolution result = resultSet.next(); resultSet.hasNext(); result = resultSet.next()) {
				properties.add(result.get("?result").asResource());
				if (result.get("?other1") != null)
					properties.add(result.get("?other1").asResource());
				if (result.get("?other2") != null)
					properties.add(result.get("?other2").asResource());
			}
		}

		return properties;
	}

	/**
	 * @param template
	 * @param keyword
	 * @return
	 * @throws IOException
	 */
	private String genQuery(String template, String keyword) throws IOException {
		// Read the query template and instanciate it
		InputStream in = this.getClass().getResourceAsStream("../../files/" + template + ".sparql");
		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		for (String line = reader.readLine(); line != null; line = reader.readLine())
			buffer.append(line);
		String queryString = buffer.toString().replace("%keyword%", keyword);
		return queryString;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.queryfinder.services.ClassMatcher#getClasses(java.lang.String)
	 */
	public Set<Resource> getClasses(String keyword) {
		logger.info(String.format("Look for classes for \"%s\"", keyword));

		Set<Resource> classes = new HashSet<Resource>();
		try {
			classes.addAll(execQuery(genQuery("classes", keyword)));
		} catch (Exception e) {
		}

		// Check which ones are actually used
		Set<Resource> unused = new HashSet<Resource>();
		for (Resource c : classes) {
			try {
				Query check = QueryFactory.create(genQuery("askclasses", c.getURI()));
				boolean used = QueryExecutionFactory.sparqlService(endPoint, check).execAsk();
				if (!used)
					unused.add(c);
			} catch (Exception e) {
			}
		}
		logger.info(String.format("%d filtered results out of %d ", classes.size() - unused.size(), classes.size()));
		classes.removeAll(unused);

		return classes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.PropertyMatcher#getProperties(java.lang.String
	 * )
	 */
	public Set<Resource> getProperties(String keyword) {
		logger.info(String.format("Look for properties for \"%s\"", keyword));

		// Execute the queries to get a set of properties of different type
		Set<Resource> properties = new HashSet<Resource>();
		for (String query : new String[] { "objectproperty", "datatypeproperty", "property" }) {
			try {
				properties.addAll(execQuery(genQuery(query, keyword)));
			} catch (Exception e) {
			}
		}

		// Check which ones are actually used
		Set<Resource> unused = new HashSet<Resource>();
		for (Resource property : properties) {
			try {
				Query check = QueryFactory.create(genQuery("askproperty", property.getURI()));
				boolean used = QueryExecutionFactory.sparqlService(endPoint, check).execAsk();
				if (!used)
					unused.add(property);
			} catch (Exception e) {
			}
		}
		logger.info(String.format("%d filtered results out of %d ", properties.size() - unused.size(),
				properties.size()));
		properties.removeAll(unused);

		return properties;
	}

	/**
	 * @param stmt
	 * @return
	 */
	public boolean validates(String stmt) {
		Query query = QueryFactory.create("ask {" + stmt + "}");
		return QueryExecutionFactory.sparqlService(endPoint, query).execAsk();
	}
}
