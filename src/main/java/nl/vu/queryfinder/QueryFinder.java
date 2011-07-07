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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
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
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		// Build the options
		Option queriesFileOption = OptionBuilder.withArgName("queries.txt").hasArg()
				.withDescription("use the given queries file").create("queries");
		options.addOption(queriesFileOption);
		options.addOption("ignoreblocks", false, "ignore the alternative blocks created from the mapping");
		options.addOption("h", false, "print help message");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);

		// Handle option "h"
		if (cmd.hasOption("h")) {
			printHelpAndExit(0);
		}

		// Handle option "queries"
		String queriesFilePath = cmd.getOptionValue("queries");
		if (queriesFilePath == null) {
			logger.error("You must provide a valid file for the queries");
			printHelpAndExit(-1);
		}
		File queriesFile = new File(queriesFilePath);
		if (!queriesFile.exists()) {
			logger.error("The specified file '" + queriesFile.getAbsolutePath() + "' does not exist.");
			printHelpAndExit(-1);
		}

		// Handle option "ignoreblocks"
		boolean ignoreblocks = cmd.hasOption("h");
		
		// Go!
		QueryFinder me = new QueryFinder();
		QueryFileParser queryFileParser = new QueryFileParser();
		queryFileParser.parse(queriesFile);
		me.processQueries(queryFileParser.getEndPoints(), queryFileParser.getQueries(), ignoreblocks);
	}

	/**
	 * @throws Exception
	 * 
	 */
	private void processQueries(List<EndPoint> endPoints, List<StructuredQuery> queries, boolean ignoreblocks) throws Exception {
		// Create the workflow
		logger.info("Initialise workflow");
		logger.info("Ignore blocks = " + ignoreblocks);
		WorkFlow workFlow = new WorkFlow();
		SPARQLMatcher matcher = new SPARQLMatcher(endPoints);
		workFlow.setPropertyMatcher(matcher);
		workFlow.setClassMatcher(matcher);
		workFlow.setResourceMatcher(matcher);
		workFlow.setMappedQueryGenerator(new DefaultMappedQueryGen());
		EvolutionarySolver solver = new EvolutionarySolver(endPoints, ignoreblocks);
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

		// Stop the solver
		solver.terminate();
	}

}
