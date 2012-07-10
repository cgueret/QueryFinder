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
import nl.vu.queryfinder.services.impl.ModelExpander;
import nl.vu.queryfinder.services.impl.SPARQLMatcher;
import nl.vu.queryfinder.services.impl.WordNetExpander;

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
		// File names
		String outputFileName = null;
		String inputFileName = null;

		// Configure the data layer
		DataLayer dataLayer;
		if (dataLayerName.equals("spyros"))
			dataLayer = SpyrosHBaseDataLayer.getInstance("default");
		else if (dataLayerName.equals("sever_remote"))
			dataLayer = new SeverHBaseDataLayer(HBaseConnection.REST);
		else if (dataLayerName.equals("sever_local"))
			dataLayer = new SeverHBaseDataLayer(HBaseConnection.NATIVE_JAVA);
		else if (dataLayerName.equals("sparql"))
			dataLayer = new SPARQLDataLayer(null);
		else
			dataLayer = null;

		// Configure the SPARQL directory
		Directory directory = new Directory();
		// directory.add(new EndPoint("http://dbpedia.org/sparql",
		// "http://dbpedia.org", EndPointType.VIRTUOSO));
		directory.add(new EndPoint("http://factforge.net/sparql", null, EndPointType.OWLIM));

		// Expand all the literals using Wordnet
		logger.info("1 - Expand literals with wordnet");
		inputFileName = fileName;
		outputFileName = fileName.replace(".ttl", "-wordnet.ttl");
		if (!(new File(outputFileName)).exists()) {
			Query query = new Query();
			query.loadFrom(inputFileName);
			Service service = new WordNetExpander();
			service.process(query).saveTo(outputFileName);
		}

		// Expand the model
		logger.info("2 - Expand the model");
		inputFileName = outputFileName;
		outputFileName = fileName.replace(".ttl", "-expand.ttl");
		if (!(new File(outputFileName)).exists()) {
			Query query = new Query();
			query.loadFrom(inputFileName);
			Service service = new ModelExpander();
			service.process(query).saveTo(outputFileName);
		}

		// Turn the literals into resources
		logger.info("3 - Turn the literals into resources");
		inputFileName = outputFileName;
		outputFileName = fileName.replace(".ttl", "-matcher.ttl");
		if (!(new File(outputFileName)).exists()) {
			Query query = new Query();
			query.loadFrom(inputFileName);
			Service service = new SPARQLMatcher(directory);
			service.process(query).saveTo(outputFileName);
		}

		// Filter out non valid query patterns
		logger.info("4 - Filter out non valid query patterns");
		inputFileName = outputFileName;
		outputFileName = fileName.replace(".ttl", "-filter.ttl");
		if (!(new File(outputFileName)).exists()) {
			Query query = new Query();
			query.loadFrom(inputFileName);
			Service service = new AskFilter(dataLayer);
			service.process(query).saveTo(outputFileName);
		}

		// Run the evolutionary solver
		logger.info("5 - Run the evolutionary solver");
		inputFileName = outputFileName;
		outputFileName = fileName.replace(".ttl", "-solver.ttl");
		if (!(new File(outputFileName)).exists()) {
			Query query = new Query();
			query.loadFrom(inputFileName);
			Service service = new EvolutionarySolver(dataLayer);
			service.process(query).saveTo(outputFileName);
		}
	}
}
