/**
 * 
 */
package nl.vu.queryfinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.RDF;

import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.EndPoint.EndPointType;
import nl.vu.queryfinder.model.QueryPattern;
import nl.vu.queryfinder.model.StructuredQuery;
import nl.vu.queryfinder.model.WorkFlow;
import nl.vu.queryfinder.services.impl.DefaultMappedQueryGen;
import nl.vu.queryfinder.services.impl.IncrementalBuilder;
import nl.vu.queryfinder.services.impl.SPARQLMatcher;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class BatchRun {
	// Logger
	static final Logger logger = LoggerFactory.getLogger(BatchRun.class);
	// Queried end point
	private EndPoint endPoint = null;
	// Set of queries
	private List<StructuredQuery> queries = new ArrayList<StructuredQuery>();

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		BatchRun me = new BatchRun();
		me.parse(new File("queries/queries-test.txt"));
		me.processQueries();
	}

	/**
	 * @throws Exception
	 * 
	 */
	private void processQueries() throws Exception {
		// Create the workflow
		WorkFlow workFlow = new WorkFlow();
		SPARQLMatcher matcher = new SPARQLMatcher(endPoint);
		workFlow.setPropertyMatcher(matcher);
		workFlow.setClassMatcher(matcher);
		workFlow.setResourceMatcher(matcher);
		workFlow.setQueryGenerator(new IncrementalBuilder(endPoint));
		workFlow.setMappedQueryGenerator(new DefaultMappedQueryGen());
		
		for (StructuredQuery query : queries) {
			logger.info("");
			logger.info("------------------------------------------------");
			logger.info("");
			logger.info(query.getTitle());
			logger.info("");
			logger.info("Query = " + query.toString());
			logger.info("");
			try {
				workFlow.process(query);
			} catch (Exception e) {
				logger.warn("Failed ! " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param queriesFile
	 * @throws Exception
	 */
	private void parse(File queriesFile) throws Exception {
		StructuredQuery currentQuery = null;
		Map<String, String> params = new HashMap<String, String>();

		BufferedReader reader = new BufferedReader(new FileReader(queriesFile));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			// Skip blank lines
			if (line.length() == 0)
				continue;

			// Skip comments
			if (line.startsWith("#"))
				continue;

			// Parameters
			if (line.startsWith("!")) {
				String[] param = line.substring(1, line.length()).split("=");
				if (param.length == 2)
					params.put(param[0], param[1]);
				continue;
			}

			// New query
			if (line.startsWith("---")) {
				if (currentQuery != null && !currentQuery.isEmpty())
					queries.add(currentQuery);
				currentQuery = new StructuredQuery();
				continue;
			}

			// Query title
			if (!line.startsWith("(")) {
				currentQuery.setTitle(line);
				continue;
			}

			// Query pattern
			if (line.startsWith("(")) {
				String text = line.substring(1, line.indexOf(')'));
				Pattern pattern = Pattern.compile("\"\\s+\"|\\s+\"|\"\\s+");
				String[] parts = pattern.split(text);
				if (parts.length == 3) {
					Node[] nodes = new Node[3];
					for (int i = 0; i < 3; i++) {
						String element = parts[i].replace('"', ' ').trim();
						if (element.startsWith("?"))
							nodes[i] = Node.createVariable(element.substring(1, element.length()));
						else if (element.equals("type"))
							nodes[i] = RDF.type.asNode();
						else
							nodes[i] = Node.createLiteral(element);
					}
					currentQuery.add(QueryPattern.create(nodes[0], nodes[1], nodes[2]));
				} else {
					throw new Exception("Error parsing " + text + " = " + StringUtils.join(parts, "|"));
				}
				continue;
			}

			logger.info("Not managed: " + line);
		}
		if (currentQuery != null && !currentQuery.isEmpty())
			queries.add(currentQuery);
		reader.close();

		// Initialise the end point
		EndPointType type = null;
		if (params.get("type") != null && params.get("type").equals("virtuoso"))
			type = EndPointType.VIRTUOSO;
		if (params.get("type") != null && params.get("type").equals("owlim"))
			type = EndPointType.OWLIM;
		endPoint = new EndPoint(params.get("endpoint"), params.get("graph"), type);

		// Print some info
		logger.info(String.format("Parsed %d queries", queries.size()));
		logger.info(String.format("EndPoint = %s", endPoint.toString()));
	}

}
