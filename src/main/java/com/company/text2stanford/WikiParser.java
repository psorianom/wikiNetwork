package com.company.text2stanford;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.cli.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.company.stanford2matrix.Utils.removeAlreadyParsedFolders;
import static com.company.text2stanford.Utils.listFiles;

public class WikiParser {
    public static String wikiLanguage;
    public static int nThreads;
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



    public static void main(String[] args) throws InterruptedException {
//        testDesr("./resources/Spanish/spanish.MLP");

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
        ArrayList<String> listPaths = listFiles(inputFolderPath);
        listPaths = removeAlreadyParsedFolders(listPaths, pickupFolder, folderLimit);

        //Debug
//        listPaths.clear();
//        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/wikidata/es_AA/wiki_00");
//        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/wikidata/es_AA/wiki_01");
//        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/wikidata/DU/wiki_12");

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(nThreads, listPaths.size()));
//        ParserThread.lock = new ReentrantLock();
        for (String path : listPaths) {
            Runnable worker = new ParserThread(path, nlpPipe);
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
                props.put("threads", "2");
                props.put("pos.model", "edu/stanford/nlp/models/pos-tagger/spanish/spanish-distsim.tagger");
                props.put("parse.model", "edu/stanford/nlp/models/srparser/spanishSR.ser.gz");

        }
        return new StanfordCoreNLP(props);
    }

} 
