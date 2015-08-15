package com.company.text2stanford;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import is2.data.InstancesTagger;
import is2.data.SentenceData09;
import is2.io.CONLLReader09;
import is2.lemmatizer.Lemmatizer;
import is2.tag.MFO;
import is2.tag.Tagger;
import is2.tools.Tool;
import org.apache.commons.cli.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.company.stanford2matrix.Utils.removeNotNeededFolders;
import static com.company.text2stanford.Utils.listFiles;

public class WikiParser {
    public static String wikiLanguage;
    private int nThreads;
    //    private static int imput
    private String inputFolderPath;
    private String pickupFolder;
    private String folderLimit;

    WikiParser() {
    }

//    WikiParser(String pathFolder, int nThreads) {
//        WikiParser.nThreads = nThreads;
//        WikiParser.pathFolder = pathFolder;
//    }

    public static int execute() throws Throwable {


        // Load lemmatizer
        //LOGGER.info("Loading lemmatizer");
        // true = do uppercase lemmatization
        Lemmatizer lemmatizer = new Lemmatizer("./resources/" + "Spanish" + "/CoNLL2009-ST-" + "Spanish" + "-ALL.anna-3.3.lemmatizer.model");

        // Load tagger
        //LOGGER.info("Loading tagger");
        Tagger tagger = new Tagger("./resources/" + "Spanish" + "/CoNLL2009-ST-" + "Spanish" + "-ALL.anna-3.3.postagger.model");

        // Load parser
        //LOGGER.info("Loading parser");
        is2.parser.Parser parser = new is2.parser.Parser("./resources/" + "Spanish" + "/CoNLL2009-ST-" + "Spanish" + "-ALL.anna-3.3.parser.model");


        // Sentences to parse
        String sentences[] = new String[]{
                "El Principado de Andorra ( en catalán : Principat d'Andorra ) es un pequeño país soberano del suroeste de Europa , constituido en Estado independiente , de derecho , democrático y social , cuya forma de gobierno es el coprincipado parlamentario ."};

        CONLLReader09 reader = new CONLLReader09(CONLLReader09.NO_NORMALIZE);

        for (String sentence : sentences) {
            // Prepare the sentence
            InstancesTagger instanceTagger = new InstancesTagger();
            instanceTagger.init(1, new MFO());

            String[] split = sentence.split("\\s+");
            String[] splitRoot = new String[split.length + 1];
            System.arraycopy(split, 0, splitRoot, 1, split.length);
            splitRoot[0] = CONLLReader09.ROOT;

            SentenceData09 instance = new SentenceData09();
            instance.init(splitRoot);

            reader.insert(instanceTagger, instance);

            SentenceData09 result = lemmatizer.apply(instance);
            result = tagger.apply(result);
            result = parser.parse(result, parser.params, false, parser.options);


            // Output
            System.out.println(Arrays.toString(result.forms));
            System.out.println(Arrays.toString(result.plemmas));
            System.out.println(Arrays.toString(result.ppos));
            System.out.println(Arrays.toString(result.pheads));
            System.out.println(Arrays.toString(result.plabels));
            System.out.println();

        }
        System.out.println("DONE!!");
        return 0;
    }

    public static void main(String[] args) throws InterruptedException {
//
//        try {
//            execute();
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//        }

        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("i", "input", true, "Input folder of extracted Wikipedia files");
        options.addOption("t", "threads", true, "Number of threads");
        options.addOption("p", "pickup", true, "Folder from where to restart (ignore the previous ones, ordered alphabetically)");
        options.addOption("n", "limit", true, "Parse files until this folder");
        options.addOption("l", "lang", true, "The language of the Wikipedia dump we are trying to parse");
        WikiParser myWiki = new WikiParser();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("l"))
                myWiki.wikiLanguage = line.getOptionValue("l");
            else
                myWiki.wikiLanguage = "en";

            if (line.hasOption("n"))
                myWiki.folderLimit = line.getOptionValue("n");
            else
                myWiki.folderLimit = "";

            if (line.hasOption("t"))
                myWiki.nThreads = Integer.parseInt(line.getOptionValue("t"));
            else
                myWiki.nThreads = 1;
            if (line.hasOption("p"))
                myWiki.pickupFolder = line.getOptionValue("p");
            else
                myWiki.pickupFolder = "";

            if (line.hasOption("i")) {
                myWiki.inputFolderPath = line.getOptionValue("i");
            } else
            //We cant continue if this is not set
            {
                System.out.println("Please give an input folder");
                return;
            }

        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            return;

        }
        System.out.println("Remember to use the appropriate parser, depending on the data that you are trying to parse.\n");
//        String data = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/oanc/corpus";
//        WikiParser myWiki = new WikiParser(inputFolderPath, pickupFolder, Integer.parseInt(numThreads));

        myWiki.run();


    }

    public void run() throws InterruptedException {
        long start = System.nanoTime();
        StanfordCoreNLP nlpPipe = createCoreNLPObject(wikiLanguage);
        Map<String, Tool> mateTools = new HashMap<>();
        if (!wikiLanguage.equals("en"))
            mateTools = createMateTools(wikiLanguage);

        ArrayList<String> listPaths = listFiles(inputFolderPath);
        listPaths = removeNotNeededFolders(listPaths, pickupFolder, folderLimit);

        //Debug
        listPaths.clear();
        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/wikidata/es_AA/wiki_00");
        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/wikidata/es_AA/wiki_01");
//        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/wikidata/DU/wiki_12");

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(nThreads, listPaths.size()));
        for (String path : listPaths) {
            Runnable worker = new ParserThread(path, nlpPipe, mateTools);

            //worker will execute its "run()" function
            executor.execute(worker);
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        executor.awaitTermination(10, TimeUnit.DAYS);
        long time = System.nanoTime() - start;
        System.out.println("Finished all threads");
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));
    }


    public Map<String, Tool> createMateTools(String lang) {
        if (lang.equals("es"))
            lang = "Spanish";
        else
            lang = "French";

        Map<String, Tool> tools = new HashMap<>();

        // Lemmatizer
        System.out.println("\nLoading mate-tools lemmatizer...\n\n");
        Tool lemmatizer = new Lemmatizer("./resources/" + lang + "/CoNLL2009-ST-" + lang + "-ALL.anna-3.3.lemmatizer.model");

        // POStagger
        System.out.println("\nLoading mate-tools POS tagger...\n\n");
        Tool posTagger = new Tagger("./resources/" + lang + "/CoNLL2009-ST-" + lang + "-ALL.anna-3.3.postagger.model");

        // Parser
        String modelName = "./resources/" + lang + "/CoNLL2009-ST-" + lang + "-ALL.anna-3.3.parser.model";
        is2.parser.Options opts = new is2.parser.Options(new String[]{"-model", modelName, "-cores", "1"});
        System.out.println("\nLoading mate-tools parser...\n\n");
        Tool parser = new is2.parser.Parser(opts);
        tools.put("lemmatizer", lemmatizer);
        tools.put("POStagger", posTagger);
        tools.put("dependencyParser", parser);
        System.out.println("\nFinished loading mate-tools...\n");
        return tools;

    }

    public StanfordCoreNLP createCoreNLPObject(String lang) {
        Properties props = new Properties();
        switch (lang) {
            case "en":
                props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
                props.put("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
                break;
            case "es":
                props.put("annotators", "tokenize, ssplit, pos, parse");
                props.setProperty("tokenize.options", "invertible=true,ptb3Escaping=true,normalizeParentheses=false");
                props.put("tokenize.language", "es");
                props.put("pos.model", "edu/stanford/nlp/models/pos-tagger/spanish/spanish-distsim.tagger");
                props.put("parse.model", "edu/stanford/nlp/models/srparser/spanishSR.ser.gz");

        }
        return new StanfordCoreNLP(props);
    }

} 
