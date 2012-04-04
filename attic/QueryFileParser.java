/**
 * 
 */
package nl.vu.queryfinder.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.EndPoint.EndPointType;
import nl.vu.queryfinder.model.QueryPattern;
import nl.vu.queryfinder.model.StructuredQuery;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class QueryFileParser {
	// Logger
	private static final Logger logger = LoggerFactory.getLogger(QueryFileParser.class);
	// List of queries
	private final List<StructuredQuery> queries = new ArrayList<StructuredQuery>();
	// List of end points
	private final List<EndPoint> endPoints = new ArrayList<EndPoint>();;

	/**
	 * @return the queries
	 */
	public List<StructuredQuery> getQueries() {
		return queries;
	}

	/**
	 * @return the endPoint
	 */
	public List<EndPoint> getEndPoints() {
		return endPoints;
	}

	/**
	 * @param queriesFile
	 * @throws Exception
	 */
	public void parse(File queriesFile) throws Exception {
		StructuredQuery currentQuery = null;
		BufferedReader reader = new BufferedReader(new FileReader(queriesFile));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			// Skip blank lines
			if (line.length() == 0)
				continue;

			// Skip comments
			if (line.startsWith("#"))
				continue;

			// Parameters - end point
			if (line.startsWith("!endpoint")) {
				String[] params = line.split(" ")[1].split(",");
				URI uri = URI.create(params[0]);
				EndPointType type = null;
				if (params[1].equals("virtuoso"))
					type = EndPointType.VIRTUOSO;
				else if (params[1].equals("owlim"))
					type = EndPointType.OWLIM;
				else
					throw new Exception("Can not parse this endpoint line : " + line);
				String graph = (params.length == 3 ? params[2] : null);
				EndPoint endpoint = new EndPoint(uri, graph, type);
				endPoints.add(endpoint);
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

		// Print some info
		logger.info(String.format("Parsed %d queries", queries.size()));
		logger.info(String.format("Parsed %d end points", endPoints.size()));
	}

}
