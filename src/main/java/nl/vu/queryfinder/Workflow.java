/**
 * 
 */
package nl.vu.queryfinder;

import java.io.File;

import nl.erdf.datalayer.DataLayer;
import nl.erdf.datalayer.hbase.SeverHBaseDataLayer;
import nl.erdf.datalayer.hbase.SpyrosHBaseDataLayer;
import nl.erdf.datalayer.sparql.SPARQLDataLayer;
import nl.erdf.model.Directory;
import nl.erdf.model.EndPoint;
import nl.erdf.model.EndPoint.EndPointType;
import nl.vu.datalayer.hbase.connection.HBaseConnection;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;
import nl.vu.queryfinder.services.impl.AskFilter;
import nl.vu.queryfinder.services.impl.EvolutionarySolver;
import nl.vu.queryfinder.services.impl.SPARQLMatcher;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Workflow {
	// Logger
	protected static final Logger logger = LoggerFactory.getLogger(Workflow.class);

	/**
	 * @param exitCode
	 */
	public static void printHelpAndExit(Options options, int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(Workflow.class.getName(), options);
		System.exit(exitCode);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// Compose the options
		Options options = new Options();
		options.addOption("i", "input", true, "query file (ttl)");
		options.addOption("d", "datalayer", true, "data layer name (spyros, sever_local, sever_remote)");
		options.addOption("h", "help", false, "print help message");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine line = parser.parse(options, args);

		// Handle request for help
		if (line.hasOption("h"))
			printHelpAndExit(options, 0);

		// Handle miss-use
		if (!line.hasOption("i"))
			printHelpAndExit(options, -1);
		if (!line.hasOption("d"))
			printHelpAndExit(options, -1);

		// Create an instance
		Workflow instance = new Workflow();

		// Process the input query and get the new one
		instance.process(line.getOptionValue("i"), line.getOptionValue("d"));

	}

	/**
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	private void process(String fileName, String dataLayerName) throws Exception {
		DataLayer dataLayer;
		if (dataLayerName.equals("spyros"))
			dataLayer = SpyrosHBaseDataLayer.getInstance("default");
		else if (dataLayerName.equals("sever_remote"))
			dataLayer = new SeverHBaseDataLayer(HBaseConnection.REST, true);
		else if (dataLayerName.equals("sever_local"))
			dataLayer = new SeverHBaseDataLayer(HBaseConnection.NATIVE_JAVA, false);
		else if (dataLayerName.equals("sparql"))
			dataLayer = new SPARQLDataLayer(null);
		else
			dataLayer = null;

		// Turn the literals into resources
		logger.info("1 - Turn the literals into resources");
		String outputMatcherFileName = fileName.replace(".ttl", "-sparqlmatcher.ttl");
		if (!(new File(outputMatcherFileName)).exists()) {
			Directory directory = new Directory();
			directory.add(new EndPoint("http://dbpedia.org/sparql", "http://dbpedia.org", EndPointType.VIRTUOSO));
			Service matcher = new SPARQLMatcher(directory);
			Query query = new Query();
			query.loadFrom(fileName);
			matcher.process(query).saveTo(outputMatcherFileName);
		}

		// Filter out non valid query patterns
		logger.info("2 - Filter out non valid query patterns");
		String outputAskFileName = fileName.replace(".ttl", "-askfilter.ttl");
		if (!(new File(outputAskFileName)).exists()) {
			Service matcher = new AskFilter(dataLayer);
			Query query = new Query();
			query.loadFrom(outputMatcherFileName);
			matcher.process(query).saveTo(outputAskFileName);
		}

		// Run the evolutionary solver
		logger.info("3 - Run the evolutionary solver");
		String outputSolverFileName = fileName.replace(".ttl", "-evosolver.ttl");
		if (!(new File(outputSolverFileName)).exists()) {
			Service matcher = new EvolutionarySolver(dataLayer);
			Query query = new Query();
			query.loadFrom(outputAskFileName);
			matcher.process(query).saveTo(outputSolverFileName);
		}
	}
}
