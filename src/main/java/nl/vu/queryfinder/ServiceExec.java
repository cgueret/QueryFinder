package nl.vu.queryfinder;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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
import nl.vu.queryfinder.services.impl.Copy;
import nl.vu.queryfinder.services.impl.EvolutionarySolver;
import nl.vu.queryfinder.services.impl.ModelExpander;
import nl.vu.queryfinder.services.impl.SPARQLMatcher;
import nl.vu.queryfinder.services.impl.WordNetExpander;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

// http://www.velocityreviews.com/forums/t303241-instantiate-a-class-with-a-variable-as-its-name.html
public class ServiceExec {
	// The different services available
	private final static Set<String> services = new HashSet<String>();
	static {
		services.add(AskFilter.class.getName());
		services.add(Copy.class.getName());
		services.add(EvolutionarySolver.class.getName());
		services.add(ModelExpander.class.getName());
		services.add(SPARQLMatcher.class.getName());
		services.add(WordNetExpander.class.getName());
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
		options.addOption("s", "service", true, "name of the service to use");
		options.addOption("i", "input", true, "query input file (ttl)");
		options.addOption("o", "output", true, "query output file (ttl)");
		options.addOption("p", "parameters", true, "coma separated list of parameters");
		options.addOption("l", "list", false, "list available services");
		options.addOption("h", "help", false, "print help message");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine line = parser.parse(options, args);

		// Handle request for help
		if (line.hasOption("h"))
			printHelpAndExit(options, 0);

		// Handle listing services
		if (line.hasOption("l")) {
			for (String ss : services)
				System.out.println(ss);
			System.exit(0);
		}

		// Handle miss-use
		if (!line.hasOption("s") || !line.hasOption("o") || !line.hasOption("i"))
			printHelpAndExit(options, -1);

		// Handle asking for an unknown service
		String serviceName = line.getOptionValue("s");
		if (!services.contains(serviceName)) {
			System.out.println("Service " + serviceName + " is unknown");
			printHelpAndExit(options, -1);
		}

		// Handle using a non existing file as an input
		String inputQueryName = line.getOptionValue("i");
		if (!(new File(inputQueryName)).canRead()) {
			System.out.println("Can not open " + inputQueryName);
			printHelpAndExit(options, -1);
		}

		// Load the input query
		Query inputQuery = new Query();
		inputQuery.loadFrom(inputQueryName);

		// Create an instance of the service
		Service service = (Service) Class.forName(serviceName).newInstance();

		// Load the parameters
		if (line.hasOption("p")) {
			String[] params = line.getOptionValue("p").split("'");
			for (String param : params) {
				String[] keyvalue = param.split("=");
				if (keyvalue.length == 2)
					service.setParameter(keyvalue[0], keyvalue[1]);
			}
		}

		// Configure the datalayer
		DataLayer dataLayer = null;
		String dataLayerName = service.getParameter("datalayer");
		if (dataLayerName != null) {
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
		}
		service.setDataLayer(dataLayer);

		// Configure the SPARQL end points
		Directory directory = new Directory();
		// directory.add(new EndPoint("http://dbpedia.org/sparql",
		// "http://dbpedia.org", EndPointType.VIRTUOSO));
		directory.add(new EndPoint("http://factforge.net/sparql", null, EndPointType.OWLIM));
		service.setDirectory(directory);

		// Process the query
		Query outputQuery = service.process(inputQuery);

		// Save the new query to disk
		outputQuery.saveTo(line.getOptionValue("o"));

		System.exit(0);
	}
}
