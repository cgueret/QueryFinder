/**
 * 
 */
package nl.vu.queryfinder;

import java.io.File;
import java.util.List;

import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.StructuredQuery;
import nl.vu.queryfinder.model.WorkFlow;
import nl.vu.queryfinder.services.impl.DefaultMappedQueryGen;
import nl.vu.queryfinder.services.impl.EvolutionarySolver;
import nl.vu.queryfinder.services.impl.SPARQLMatcher;
import nl.vu.queryfinder.util.QueryFileParser;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class QueryFinder {
	// Logger
	private static final Logger logger = LoggerFactory.getLogger(QueryFinder.class);
	// Options for the command line
	private static final Options options = new Options();

	/**
	 * @param exitCode
	 */
	public static void printHelpAndExit(int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(QueryFinder.class.getName(), options);
		System.exit(exitCode);
	}
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		QueryFinder me = new QueryFinder();
		QueryFileParser parser = new QueryFileParser();
		parser.parse(new File("queries/queries-factforge_expandedStefan.txt"));
		me.processQueries(parser.getEndPoints(), parser.getQueries());
	}

	/**
	 * @throws Exception
	 * 
	 */
	private void processQueries(List<EndPoint> endPoints, List<StructuredQuery> queries) throws Exception {
		// Create the workflow
		WorkFlow workFlow = new WorkFlow();
		SPARQLMatcher matcher = new SPARQLMatcher(endPoints);
		workFlow.setPropertyMatcher(matcher);
		workFlow.setClassMatcher(matcher);
		workFlow.setResourceMatcher(matcher);
		workFlow.setMappedQueryGenerator(new DefaultMappedQueryGen());
		EvolutionarySolver solver = new EvolutionarySolver(endPoints);
		workFlow.setQueryGenerator(solver);

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

		solver.terminate();
	}

}
