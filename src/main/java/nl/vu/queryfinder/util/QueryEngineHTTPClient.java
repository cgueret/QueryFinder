package nl.vu.queryfinder.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.openjena.atlas.lib.NotImplemented;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.ARQException;
import com.hp.hpl.jena.sparql.resultset.XMLInput;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.FileManager;

public class QueryEngineHTTPClient implements QueryExecution {
	static final Logger logger = LoggerFactory.getLogger(QueryEngineHTTPClient.class);
	public static final String QUERY_MIME_TYPE = "application/sparql-query";
	public static final String QUERY_RESULT_MIME_TYPE = "application/sparql-results+xml";
	public static final String PARAM_QUERY = "query";
	public static final String PARAM_DEFAULT_GRAPH = "default-graph-uri";
	public static final String PARAM_NAMED_GRAPH = "named-graph-uri";
	// The query to be executed
	private final String queryString;
	// The SPARQL EndPoint
	private final String service;
	// List of default graph
	List<String> defaultGraphURIs = new ArrayList<String>();
	// List of named graphs
	List<String> namedGraphURIs = new ArrayList<String>();
	// Connection finished ?
	private boolean finished = false;
	// Used for select
	private InputStream retainedConnection = null;
	private List<NameValuePair> params = new ArrayList<NameValuePair>();

	/**
	 * @param service
	 * @param query
	 */
	public QueryEngineHTTPClient(String service, Query query) {
		this.service = service;
		this.queryString = query.serialize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.hpl.jena.query.QueryExecution#setFileManager(com.hp.hpl.jena.util
	 * .FileManager)
	 */
	public void setFileManager(FileManager fm) {
		throw new UnsupportedOperationException("FileManagers do not apply to remote query execution");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.hpl.jena.query.QueryExecution#setInitialBinding(com.hp.hpl.jena
	 * .query.QuerySolution)
	 */
	public void setInitialBinding(QuerySolution binding) {
		throw new UnsupportedOperationException("Initial bindings not supported for remote queries");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#getDataset()
	 */
	public Dataset getDataset() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#getContext()
	 */
	public Context getContext() {
		throw new NotImplemented("Not implemented yet");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#execSelect()
	 */
	public ResultSet execSelect() {
		// Get a query object
		HttpGet httpQuery = makeHttpQuery();
		httpQuery.setHeader("Accept", QUERY_RESULT_MIME_TYPE);
		//logger.info("[SEL] " + httpQuery.getURI());

		String replyString = "";
		try {
			// Send the query
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(httpQuery);
			HttpEntity entity = response.getEntity();
			if (entity == null || response.getStatusLine().getStatusCode() != 200)
				throw new Exception("No or invalid reply");

			// Get the results
			InputStream in = entity.getContent();

			// DEBUG trick
			
//			byte b[] = IO.readWholeFile(in); replyString = new String(b);
//			logger.info(replyString); in = new ByteArrayInputStream(b);
			 

			retainedConnection = in;
			return ResultSetFactory.fromXML(in);

		} catch (Exception e) {

			logger.info("[SEL] " + queryString);
			logger.info(replyString);
			logger.info(e.getMessage());
			for (StackTraceElement st : e.getStackTrace())
				logger.info(st.toString());

			if (httpQuery != null)
				httpQuery.abort();
		}

		return null;
	}

	/**
	 * @return
	 */
	private HttpGet makeHttpQuery() {
		if (finished)
			throw new ARQException("HTTP execution already closed");

		// Create the query parameters
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair(PARAM_QUERY, queryString));
		for (Iterator<String> iter = defaultGraphURIs.iterator(); iter.hasNext();) {
			String dft = iter.next();
			qparams.add(new BasicNameValuePair(PARAM_DEFAULT_GRAPH, dft));
		}
		for (Iterator<String> iter = namedGraphURIs.iterator(); iter.hasNext();) {
			String name = iter.next();
			qparams.add(new BasicNameValuePair(PARAM_NAMED_GRAPH, name));
		}
		for (NameValuePair extraParam : params)
			qparams.add(extraParam);

		qparams.add(new BasicNameValuePair("format", QUERY_RESULT_MIME_TYPE));
		
		// Create the query object
		String uri = service + "?" + URLEncodedUtils.format(qparams, "UTF-8");
		HttpGet httpQuery = new HttpGet(uri);
		return httpQuery;
	}

	/**
	 * @param defaultGraph
	 *            The defaultGraph to add.
	 */
	public void addDefaultGraph(String defaultGraph) {
		if (defaultGraphURIs == null)
			defaultGraphURIs = new ArrayList<String>();
		defaultGraphURIs.add(defaultGraph);
	}

	/**
	 * @param name
	 *            The URI to add.
	 */
	public void addNamedGraph(String name) {
		if (namedGraphURIs == null)
			namedGraphURIs = new ArrayList<String>();
		namedGraphURIs.add(name);
	}

	/**
	 * @param field
	 * @param value
	 */
	public void addParam(String field, String value) {
		if (params == null)
			params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(field, value));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#execConstruct()
	 */
	public Model execConstruct() {
		throw new NotImplemented("Not implemented yet");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.hpl.jena.query.QueryExecution#execConstruct(com.hp.hpl.jena.rdf
	 * .model.Model)
	 */
	public Model execConstruct(Model model) {
		throw new NotImplemented("Not implemented yet");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#execDescribe()
	 */
	public Model execDescribe() {
		throw new NotImplemented("Not implemented yet");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.hpl.jena.query.QueryExecution#execDescribe(com.hp.hpl.jena.rdf.
	 * model.Model)
	 */
	public Model execDescribe(Model model) {
		throw new NotImplemented("Not implemented yet");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#execAsk()
	 */
	public boolean execAsk() {
		HttpGet httpQuery = makeHttpQuery();
		httpQuery.setHeader("Accept", QUERY_RESULT_MIME_TYPE);

		try {
			// Send the query
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(httpQuery);
			HttpEntity entity = response.getEntity();
			if (entity == null)
				throw new Exception("No entity");

			// Get the result
			InputStream in = entity.getContent();
			boolean result = XMLInput.booleanFromXML(in);
			in.close();
			return result;
		} catch (Exception e) {
			logger.info("[ASK] " + queryString);
			e.printStackTrace();
			if (httpQuery != null)
				httpQuery.abort();
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#abort()
	 */
	public void abort() {
		throw new NotImplemented("Not implemented yet");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#close()
	 */
	public void close() {
		finished = false;
		if (retainedConnection != null) {
			try {
				retainedConnection.close();
			} catch (java.io.IOException e) {
				logger.warn("Failed to close connection", e);
			} finally {
				retainedConnection = null;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#setTimeout(long,
	 * java.util.concurrent.TimeUnit)
	 */
	public void setTimeout(long timeout, TimeUnit timeoutUnits) {
		throw new NotImplemented("Not implemented yet");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#setTimeout(long)
	 */
	public void setTimeout(long timeout) {
		throw new NotImplemented("Not implemented yet");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#setTimeout(long,
	 * java.util.concurrent.TimeUnit, long, java.util.concurrent.TimeUnit)
	 */
	public void setTimeout(long timeout1, TimeUnit timeUnit1, long timeout2, TimeUnit timeUnit2) {
		throw new NotImplemented("Not implemented yet");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.hpl.jena.query.QueryExecution#setTimeout(long, long)
	 */
	public void setTimeout(long timeout1, long timeout2) {
		throw new NotImplemented("Not implemented yet");
	}

}
