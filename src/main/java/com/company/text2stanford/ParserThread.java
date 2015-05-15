package com.company.text2stanford;

import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pavel on 16/12/14.
 */


public class ParserThread implements Runnable {


    public final String nline = "\n";
    public final String header = "token\tlemma\tPOS\tconstituency\thead\tdependency";
    public String pathFile;
    public StanfordCoreNLP coreParser;
    public ArrayList<String> seenPhrasals = new ArrayList<>();

    //    Constructor
    ParserThread(String pathFile, StanfordCoreNLP coreParser) {
        this.coreParser = coreParser;
        this.pathFile = pathFile;
    }

    public HashMap<String, String> getAnchors(Elements anchors) {
        HashMap<String, String> anchorMap = new HashMap<>();

        for (Element a : anchors) {

            String href = a.attr("href");
            String hrefWord = a.text();
//            System.out.println("\t" + hrefWord + ": " + href);
            anchorMap.put(href, hrefWord);

        }

        return anchorMap;
    }


    public HashMap<Integer, HashMap> tokenDependencies(SemanticGraph depGraph) {
        HashMap<Integer, HashMap> tokenDeps = new HashMap<>();
        Collection<TypedDependency> typedDeps = depGraph.typedDependencies();
        for (TypedDependency depn : typedDeps) {
            HashMap<String, String> relationHead = new HashMap<>();
            int wordIndex = depn.dep().index();
            int headIndex = depn.gov().index();
            String relation = depn.reln().getShortName();

//            if (depn.reln().getSpecific() != null)
//                relation += "_" + depn.reln().getSpecific();

            relationHead.put("relation", relation);
            relationHead.put("headIndex", Integer.toString(headIndex));
            tokenDeps.put(wordIndex, relationHead);
        }

        return tokenDeps;
    }

    public HashMap<Integer, ArrayList> tokenConstituencies(Tree tree) {
        HashMap<Integer, ArrayList> tokenTags = new HashMap<>();
        List<Tree> children = tree.getChildrenAsList();
        List<Tree> siblings = new ArrayList<>();
        ArrayList<String> listTags = new ArrayList<>();
        //Traverse depth search first the tree looking for all the constituents

//        String whereareweDEBUG = tree.label().value();
        for (Tree son : children)
            if (!son.isLeaf()) {
                tokenTags.putAll(tokenConstituencies(son));
            } else {
                tokenTags.put(((CoreLabel) son.label()).index(), listTags);
            }
        if (!tree.isPreTerminal()) // I do not want the POS tag in this list
            for (Integer key : tokenTags.keySet())
                tokenTags.get(key).add(tree.label().value() + "_" + Integer.toString(tree.hashCode() % 100));

        return tokenTags;
    }

    private void parseSentencesText(String pathFile) {
        LineIterator it = null;
        try {


            File input = new File(pathFile);
            String parsedFilePath = input.getCanonicalPath() + ".parsed.txt";
            FileWriter parsedOutput = new FileWriter(parsedFilePath);
            BufferedWriter bufferedOut = new BufferedWriter(parsedOutput);
            it = FileUtils.lineIterator(input, "UTF-8");
            /// We remove the line numbers if any

            bufferedOut.write("FILENAME " + input.getName() + nline);
            bufferedOut.write(header + nline);
            bufferedOut.write("%%#PAGE " + input.getName() + nline);


            while (it.hasNext()) {
                String lineFile = it.nextLine().trim();
                if (lineFile.isEmpty())
                    continue;
                lineFile = lineFile.replaceAll("^[0-9]+", "");
                // Parse the document with CoreNLP
//                docText = "The collection is often a set of results of an experiment, or a set of results from a survey";
                Annotation document = new Annotation(lineFile);
                coreParser.annotate(document);
                // Treat the result
                List<CoreMap> sentences = document.get(SentencesAnnotation.class);
                int sentenceId = 0;
                for (CoreMap sentence : sentences) {
                    String line;
                    sentenceId++;
                    bufferedOut.write("%%#SEN " + Integer.toString(sentenceId) + nline);

                    // this is the parse tree of the current sentence
                    Tree tree = sentence.get(TreeAnnotation.class);
                    HashMap<Integer, ArrayList> constituencyTokens = tokenConstituencies(tree.skipRoot());

                    // this is the dependency graph of the current sentence
                    SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                    HashMap<Integer, HashMap> dependencyTokens = tokenDependencies(dependencies);

                    // traversing the words in the current sentence
                    // a CoreLabel is a CoreMap with additional token-specific methods
                    String head;
                    String dependency;
                    for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

                        // this is the text of the token
                        String word = token.get(TextAnnotation.class);
                        // this is the index of said token
                        int wordIndex = token.get(IndexAnnotation.class);
                        // this is the POS tag of the token
                        String pos = token.get(PartOfSpeechAnnotation.class);
                        // this is the  lemma of the token
                        String lemma = token.get(LemmaAnnotation.class);
                        // this is the constituency information of the token
                        String constituency = String.join(",", constituencyTokens.get(wordIndex));
                        // this is the constituency information of the token
                        // the head first
                        if (dependencyTokens.get(wordIndex) == null) {
                            head = "0";
                            dependency = "PUNCT";
                        } else {
                            head = (String) dependencyTokens.get(wordIndex).get("headIndex");
                            // the relation (dependency label)
                            dependency = (String) dependencyTokens.get(wordIndex).get("relation");
                        }
                        // create the line that will be written in the output
                        line = word + "\t" + lemma + "\t" + pos + "\t" + constituency + "\t" + head + "\t" + dependency + nline;
                        bufferedOut.write(line);

                    }

                }
            }

            bufferedOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            LineIterator.closeQuietly(it);
        }

    }

    private void parseSemeval2007(String pathFile) {
        try {

            File input = new File(pathFile);
            InputStream xmlFile = new FileInputStream(input);
            String parsedFilePath = input.getCanonicalPath() + ".parsed.txt";
            FileWriter parsedOutput = new FileWriter(parsedFilePath);

            BufferedWriter bufferedOut = new BufferedWriter(parsedOutput);
            Document doc = Jsoup.parse(xmlFile, "UTF-8", "", Parser.xmlParser());

            bufferedOut.write("FILENAME\t" + input.getName() + nline);
            bufferedOut.write(header + nline);


            //Foreach lexical element "lexelt"
            Elements lexelts = doc.getElementsByTag("lexelt");
            for (Element lx : lexelts) {
                String lexeltName = lx.attr("item");
                bufferedOut.write("%%#LEXELT\t" + lexeltName + nline);

                Elements instances = lx.getElementsByTag("instance");
                for (Element ins : instances) {
                    String instanceName = ins.attr("id");
                    bufferedOut.write("%%#INSTANCE\t" + instanceName + nline);
                    String textToParse = ins.text();

                    Annotation document = new Annotation(textToParse);
                    coreParser.annotate(document);
                    // Treat the result
                    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
                    int sentenceId = 0;
                    for (CoreMap sentence : sentences) {

                        List<CoreLabel> listTokens = sentence.get(TokensAnnotation.class);
                        int sentenceSize = listTokens.size();
                        String line;
                        sentenceId++;
                        bufferedOut.write("%%#SEN\t" + Integer.toString(sentenceId) + nline);

                        // this is the parse tree of the current sentence
                        Tree tree = sentence.get(TreeAnnotation.class);
                        HashMap<Integer, ArrayList> constituencyTokens = tokenConstituencies(tree.skipRoot());

                        // this is the dependency graph of the current sentence
                        SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                        HashMap<Integer, HashMap> dependencyTokens = tokenDependencies(dependencies);

                        // traversing the words in the current sentence
                        // a CoreLabel is a CoreMap with additional token-specific methods
                        String head;
                        String dependency;
                        for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

                            // this is the string of the token
                            String word = token.get(TextAnnotation.class);
                            // this is the index of said token
                            int wordIndex = token.get(IndexAnnotation.class);
                            // this is the POS tag of the token
                            String pos = token.get(PartOfSpeechAnnotation.class);
                            // this is the  lemma of the token
                            String lemma = token.get(LemmaAnnotation.class);
                            // this is the constituency information of the token
                            String constituency = String.join(",", constituencyTokens.get(wordIndex));
                            // this is the constituency information of the token
                            // the head first
                            if (dependencyTokens.get(wordIndex) == null) {
                                head = "0";
                                dependency = "PUNCT";
                            } else {
                                head = (String) dependencyTokens.get(wordIndex).get("headIndex");
                                // the relation (dependency label)
                                dependency = (String) dependencyTokens.get(wordIndex).get("relation");
                            }
                            // create the line that will be written in the output
                            line = word + "\t" + lemma + "\t" + pos + "\t" + constituency + "\t" + head + "\t" + dependency + nline;
                            bufferedOut.write(line);

                        }

                    }//foreach sentence
                }//foreach instance
            }//foreach lexelt
            bufferedOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseOANCText(String pathFile) {
        LineIterator it = null;
        try {


            File input = new File(pathFile);
            String parsedFilePath = input.getCanonicalPath() + ".parsed.txt";
            FileWriter parsedOutput = new FileWriter(parsedFilePath);
            BufferedWriter bufferedOut = new BufferedWriter(parsedOutput);
            it = FileUtils.lineIterator(input, "UTF-8");
            /// We remove the line numbers if any

            bufferedOut.write("FILENAME " + input.getName() + nline);
            bufferedOut.write(header + nline);
            bufferedOut.write("%%#PAGE " + input.getName() + nline);

            String paragraph = "";
            while (it.hasNext()) {

                String lineFile = it.nextLine().trim();
                if (!lineFile.isEmpty()) {
                    paragraph = paragraph + lineFile + " ";
                    continue;
                } else if (paragraph.isEmpty())
                    continue;
                lineFile = paragraph;
                paragraph = "";
                lineFile = lineFile.replaceAll("^[0-9]+", "");
                // Parse the document with CoreNLP
//                docText = "The collection is often a set of results of an experiment, or a set of results from a survey";
                Annotation document = new Annotation(lineFile);
                coreParser.annotate(document);
                // Treat the result
                List<CoreMap> sentences = document.get(SentencesAnnotation.class);
                int sentenceId = 0;
                for (CoreMap sentence : sentences) {
                    List<CoreLabel> listTokens = sentence.get(TokensAnnotation.class);
                    int sentenceSize = listTokens.size();
                    String line;
                    sentenceId++;
                    bufferedOut.write("%%#SEN\t" + Integer.toString(sentenceId) + "\t" + Integer.toString(sentenceSize) + nline);

                    // this is the parse tree of the current sentence
                    Tree tree = sentence.get(TreeAnnotation.class);
                    HashMap<Integer, ArrayList> constituencyTokens = tokenConstituencies(tree.skipRoot());

                    // this is the dependency graph of the current sentence
                    SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                    HashMap<Integer, HashMap> dependencyTokens = tokenDependencies(dependencies);

                    // traversing the words in the current sentence
                    // a CoreLabel is a CoreMap with additional token-specific methods
                    String head;
                    String dependency;
                    for (CoreLabel token : listTokens) {

                        // this is the text of the token
                        String word = token.get(TextAnnotation.class);
                        // this is the index of said token
                        int wordIndex = token.get(IndexAnnotation.class);
                        // this is the POS tag of the token
                        String pos = token.get(PartOfSpeechAnnotation.class);
                        // this is the  lemma of the token
                        String lemma = token.get(LemmaAnnotation.class);
                        // this is the constituency information of the token
                        String constituency = String.join(",", constituencyTokens.get(wordIndex));
                        // this is the constituency information of the token
                        // the head first
                        if (dependencyTokens.get(wordIndex) == null) {
                            head = "0";
                            dependency = "PUNCT";
                        } else {
                            head = (String) dependencyTokens.get(wordIndex).get("headIndex");
                            // the relation (dependency label)
                            dependency = (String) dependencyTokens.get(wordIndex).get("relation");
                        }
                        // create the line that will be written in the output
                        line = word + "\t" + lemma + "\t" + pos + "\t" + constituency + "\t" + head + "\t" + dependency + nline;
                        bufferedOut.write(line);

                    }

                }
            }

            bufferedOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            LineIterator.closeQuietly(it);
        }

    }

    private void parseWiki(String pathFile) {
        try {

            File input = new File(pathFile);

            String parsedFilePath = input.getCanonicalPath() + ".parsed.txt";
            FileWriter parsedOutput = new FileWriter(parsedFilePath);
            BufferedWriter bufferedOut = new BufferedWriter(parsedOutput);

            Document doc;
            doc = Jsoup.parse(input, "UTF-8", "");

            bufferedOut.write("FILENAME " + input.getName() + nline);
            bufferedOut.write(header + nline);

            // Foreach doc in the file
            Elements docs = doc.getElementsByTag("doc");
            for (Element c : docs) {
                String docText = c.text();
                docText = docText.replaceFirst(c.attr("title"), ""); ///> Remove the title from the content
                bufferedOut.write("%%#PAGE " + c.attr("title") + nline);


                // Get anchors (links to other wiki articles) used in the document
                HashMap<String, String> anchorMap = getAnchors(c.getElementsByTag("a"));

                // Parse the document with CoreNLP
//                docText = "The collection is often a set of results of an experiment, or a set of results from a survey";
                Annotation document = new Annotation(docText);
                coreParser.annotate(document);
                // Treat the result
                List<CoreMap> sentences = document.get(SentencesAnnotation.class);
                int sentenceId = 0;
                for (CoreMap sentence : sentences) {
                    List<CoreLabel> listTokens = sentence.get(TokensAnnotation.class);
                    int sentenceSize = listTokens.size();
                    String line;
                    sentenceId++;
                    bufferedOut.write("%%#SEN\t" + Integer.toString(sentenceId) + "\t" + Integer.toString(sentenceSize) + nline);

                    // this is the parse tree of the current sentence
                    Tree tree = sentence.get(TreeAnnotation.class);
                    HashMap<Integer, ArrayList> constituencyTokens = tokenConstituencies(tree.skipRoot());

                    // this is the dependency graph of the current sentence
                    SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                    HashMap<Integer, HashMap> dependencyTokens = tokenDependencies(dependencies);

                    // traversing the words in the current sentence
                    // a CoreLabel is a CoreMap with additional token-specific methods
                    String head;
                    String dependency;
                    for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

                        // this is the text of the token
                        String word = token.get(TextAnnotation.class);
                        // this is the index of said token
                        int wordIndex = token.get(IndexAnnotation.class);
                        // this is the POS tag of the token
                        String pos = token.get(PartOfSpeechAnnotation.class);
                        // this is the  lemma of the token
                        String lemma = token.get(LemmaAnnotation.class);
                        // this is the constituency information of the token
                        String constituency = String.join(",", constituencyTokens.get(wordIndex));
                        // this is the constituency information of the token
                        // the head first
                        if (dependencyTokens.get(wordIndex) == null) {
                            head = "0";
                            dependency = "PUNCT";
                        } else {
                            head = (String) dependencyTokens.get(wordIndex).get("headIndex");
                            // the relation (dependency label)
                            dependency = (String) dependencyTokens.get(wordIndex).get("relation");
                        }
                        // create the line that will be written in the output
                        line = word + "\t" + lemma + "\t" + pos + "\t" + constituency + "\t" + head + "\t" + dependency + nline;
                        bufferedOut.write(line);

                    }

                }//foreach sentence
            }//foreach page
            bufferedOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.print("WORKING on " + pathFile);
//        parseOANCText(pathFile);
        parseSemeval2007(pathFile);
        System.out.println("... DONE");

    }
}

