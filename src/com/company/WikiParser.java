package com.company;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.company.Utils.listFiles;

public class WikiParser {
    private static int nThreads;

    private static String pathFolder;

    WikiParser(String pathFolder, int nThreads) {
        WikiParser.nThreads = nThreads;
        WikiParser.pathFolder = pathFolder;
        //"/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data"
    }


    public static void run() throws InterruptedException {
        StanfordCoreNLP nlpPipe = createCoreNLPObject();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        ArrayList<String> listPaths = listFiles(pathFolder);
//        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/AA/wiki_00");
        for (String path : listPaths) {
            Runnable worker = new ParserThread(path, nlpPipe);
            executor.execute(worker);
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        executor.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("Finished all threads");
    }

    public static StanfordCoreNLP createCoreNLPObject() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
        props.put("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        return new StanfordCoreNLP(props);
    }

    public static void main(String[] args) throws InterruptedException {
        WikiParser myWiki = new WikiParser("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data", 1000);
        run();


    }
} 