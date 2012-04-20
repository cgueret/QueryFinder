package nl.vu.queryfinder;

import java.util.HashMap;
import java.util.Map;

import nl.erdf.model.Directory;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;
import nl.vu.queryfinder.services.impl.Copy;
import nl.vu.queryfinder.services.impl.AskFilter;
import nl.vu.queryfinder.services.impl.EvolutionarySolver;
import nl.vu.queryfinder.services.impl.SPARQLMatcher;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public class ServiceExec {
	// The parameters given to the component
	private final Map<String, String> parameters = new HashMap<String, String>();

	/**
	 * @param input
	 * @param component
	 * @return
	 * @throws Exception
	 */
	public Query process(String input, String component) throws Exception {
		Query inputQuery = new Query();
		inputQuery.loadFrom(input);

		// If a component fail, we will return the input query
		Query outputQuery = inputQuery;

		if (component.equals("copy")) {
			Service service = new Copy();
			outputQuery = service.process(inputQuery);
		}

		else if (component.equals("sparqlmatcher")) {
			if (!parameters.containsKey("endpoints"))
				throw new Exception("Requiered parameter is missing: endpoints");
			Directory directory = Directory.create(parameters.get("endpoints"));
			Service service = new SPARQLMatcher(directory);
			outputQuery = service.process(inputQuery);
		}

		else if (component.equals("askfilter")) {
			if (!parameters.containsKey("endpoints"))
				throw new Exception("Requiered parameter is missing: endpoints");
			Directory directory = Directory.create(parameters.get("endpoints"));
			Service service = new AskFilter(directory);
			outputQuery = service.process(inputQuery);
		}
		
		else if (component.equals("evosolver")) {
			if (!parameters.containsKey("endpoints"))
				throw new Exception("Requiered parameter is missing: endpoints");
			Directory directory = Directory.create(parameters.get("endpoints"));
			Service service = new EvolutionarySolver(directory);
			outputQuery = service.process(inputQuery);
		}

		else
			throw new Exception("Unrecognised component:" + component);

		return outputQuery;
	}

	/**
	 * @param exitCode
	 */
	public static void printHelpAndExit(Options options, int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(ServiceExec.class.getName(), options);
		System.exit(exitCode);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// Compose the options
		Options options = new Options();
		options.addOption("c", "component", true, "name of the processing component");
		options.addOption("i", "input", true, "query input file (ttl)");
		options.addOption("o", "output", true, "query output file (ttl)");
		options.addOption("p", "parameters", true, "component parameters");
		options.addOption("h", "help", false, "print help message");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine line = parser.parse(options, args);

		// Handle request for help
		if (line.hasOption("h"))
			printHelpAndExit(options, 0);

		// Handle miss-use
		if (!line.hasOption("c") || !line.hasOption("o") || !line.hasOption("i"))
			printHelpAndExit(options, -1);

		// Create an instance
		ServiceExec instance = new ServiceExec();

		// Handle parameters
		if (line.hasOption("p")) {
			String[] params = line.getOptionValue("p").split("'");
			for (String param : params) {
				String[] keyvalue = param.split("=");
				if (keyvalue.length == 2)
					instance.parameters.put(keyvalue[0], keyvalue[1]);
			}
		}

		// Process the input query and get the new one
		Query newQuery = instance.process(line.getOptionValue("i"), line.getOptionValue("c"));

		// Save the new query to disk
		newQuery.saveTo(line.getOptionValue("o"));
	}
}
