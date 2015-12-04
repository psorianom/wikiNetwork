package com.company.text2stanford;

import com.company.stanford2matrix.Utils;
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
import edu.upc.freeling.*;
import is2.data.SentenceData09;
import is2.io.CONLLReader09;
import is2.tools.Tool;

import jni.Token;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentUtils;
import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;
import org.maltparser.concurrent.graph.ConcurrentDependencyNode;
import org.maltparser.core.exception.MaltChainedException;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static com.company.text2stanford.WikiParser.createFreelingParser;
import static com.company.text2stanford.WikiParser.createMaltParser;

/**
 * Created by pavel on 16/12/14.
 */


public class ParserThread implements Runnable {


    public final String nline = "\n";
    public final String header = "token\tlemma\tPOS\tconstituency\thead\tdependency";
    public String pathFile;
    public StanfordCoreNLP coreParser;
    public Map<String, Tool> mateTools;
    public jni.Parser desrParser;
    public ConcurrentMaltParserModel maltParser;
    public Map<String, Object> freelingParser;
    //    Constructor
    ParserThread(String pathFile, StanfordCoreNLP coreParser) {
        this.coreParser = coreParser;
        this.pathFile = pathFile;
        this.mateTools = mateTools;
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

    @Deprecated
    public Map<String, String[]> getTokensAndPOSTags(List<CoreLabel> stanfordTokens) {
        /**
         * Receives a list of CoreLabels and returns a list of only the tokens they contain.
         */
        Map<String, String[]> tokenPOStag = new HashMap<>();
        String[] tokens = new String[stanfordTokens.size()];
        String[] CPOStags = new String[stanfordTokens.size()];
        String[] POStags = new String[stanfordTokens.size()];
        for (int i = 0; i < stanfordTokens.size(); i++) {
            CoreLabel token = stanfordTokens.get(i);
            tokens[i] = token.get(TextAnnotation.class);
            String posTag = token.get(PartOfSpeechAnnotation.class);
            if (posTag.substring(0, 1).toLowerCase().equals("f"))
                POStags[i] = "Fp";
            else
                POStags[i] = posTag.toUpperCase();//.substring(0, Math.min(2, posTag.length()));
            CPOStags[i] = POStags[i].substring(0, 1);

        }
        tokenPOStag.put("forms", tokens);
        tokenPOStag.put("CPOS", CPOStags);
        tokenPOStag.put("POS", POStags);
        return tokenPOStag;
    }

    public HashMap<Integer, HashMap> coreNLPTokenDependencies(SemanticGraph depGraph) {
        /**
         * This function takes a SemanticGraph object (with the typed dependencies of a sentence) and returns
         * a dictionary of dictionaries: {wordIndex:{ "relation": subj", "headIndex": "2"}, ...}
         * Each word of the phrase has a dict with each dependency it belongs to and its corresponding head.
         */
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

    public HashMap<Integer, ArrayList> coreNLPTokenConstituents(Tree tree) {
        HashMap<Integer, ArrayList> tokenTags = new HashMap<>();
        List<Tree> children = tree.getChildrenAsList();
        ArrayList<String> listTags = new ArrayList<>();
        //Traverse depth search first the tree looking for all the constituents

//        String whereareweDEBUG = tree.label().value();
        for (Tree son : children)
            if (!son.isLeaf()) {
                tokenTags.putAll(coreNLPTokenConstituents(son));
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
            String parsedFilePath = input.getCanonicalPath() + ".parsed";
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
                    HashMap<Integer, ArrayList> constituencyTokens = coreNLPTokenConstituents(tree.skipRoot());

                    // this is the dependency graph of the current sentence
                    SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                    HashMap<Integer, HashMap> dependencyTokens = coreNLPTokenDependencies(dependencies);

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

    private void parseSemeval2010(String pathFile) {
        try {

            File input = new File(pathFile);
            InputStream xmlFile = new FileInputStream(input);
            String parsedFilePath = input.getCanonicalPath() + ".parsed";
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
                        HashMap<Integer, ArrayList> constituencyTokens = coreNLPTokenConstituents(tree.skipRoot());

                        // this is the dependency graph of the current sentence
                        SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                        HashMap<Integer, HashMap> dependencyTokens = coreNLPTokenDependencies(dependencies);

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
    private void parseSemeval2007(String pathFile) {
        try {

            File input = new File(pathFile);
            InputStream xmlFile = new FileInputStream(input);
            String parsedFilePath = input.getCanonicalPath() + ".parsed";
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
                        HashMap<Integer, ArrayList> constituencyTokens = coreNLPTokenConstituents(tree.skipRoot());

                        // this is the dependency graph of the current sentence
                        SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                        HashMap<Integer, HashMap> dependencyTokens = coreNLPTokenDependencies(dependencies);

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
            String parsedFilePath = input.getCanonicalPath() + ".parsed";
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
                    HashMap<Integer, ArrayList> constituencyTokens = coreNLPTokenConstituents(tree.skipRoot());

                    // this is the dependency graph of the current sentence
                    SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                    HashMap<Integer, HashMap> dependencyTokens = coreNLPTokenDependencies(dependencies);

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

    @Override
    public void run() {
        System.out.print("WORKING on " + pathFile + "\n");
        parseWiki(pathFile);
//        parseOANCText(pathFile);
        parseSemeval2007(pathFile);
        parseSemeval2010(pathFile);
        System.out.println("... DONE");

    }

    private void parseWiki(String pathFile) {
        try {

            File input = new File(pathFile);
            String parsedFilePath = input.getCanonicalPath() + ".parsed";
            FileWriter parsedOutput = new FileWriter(parsedFilePath);
            BufferedWriter bufferedOut = new BufferedWriter(parsedOutput);

            Document doc;
            doc = Jsoup.parse(input, "UTF-8", "");

            bufferedOut.write("FILENAME " + input.getName() + nline);
            bufferedOut.write(header + nline);

            // Foreach doc in the file
            Elements docs = doc.getElementsByTag("doc");
            for (Element c : docs) {
                String articleTile = c.attr("title");
                String docText = c.text();
                docText = docText.replaceFirst(Pattern.quote(articleTile), ""); ///> Remove the title from the content
//                System.out.println(docText);
                bufferedOut.write("%%#PAGE " + articleTile + nline);


                // Get anchors (links to other wiki articles) used in the document
                HashMap<String, String> anchorMap = getAnchors(c.getElementsByTag("a"));

                // Parse the document with CoreNLP
//                docText = "A great brigand becomes a ruler of a Nation";
                docText = "The mouse eats the cheese.";
//                docText = "The cat eats the mouse.";
//                docText = "En el tramo de Telefónica un toro descolgado ha creado peligro tras embestir contra un grupo de mozos.";
                docText = "The reports indicate that the meetings hit a snag quickly.";
                docText = "The report contains copies of the minutes of these meetings.";

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
                    String pennString = tree.pennString();
                    System.out.println(sentence.toString());
                    System.out.println(pennString);
                    HashMap<Integer, ArrayList> constituencyTokens = coreNLPTokenConstituents(tree.skipRoot());
                    Map<Integer, HashMap> dependencyTokens = null;
                    //> Here we determine which dependency parser to use according to the language. We also find the lemmas.
                    // if it is english we use only the Stanford Parser.

                    // this is the dependency graph of the current sentence
                    SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                    System.out.println(dependencies.toList());
                    dependencyTokens = coreNLPTokenDependencies(dependencies);
                    // traversing the words in the current sentence
                    // a CoreLabel is a CoreMap with additional token-specific methods
                    String head;
                    String dependency;
                    String lemma;

                    for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                        // this is the text of the token
                        String word = token.get(TextAnnotation.class);
                        // this is the index of said token
                        int wordIndex = token.get(IndexAnnotation.class);
                        // this is the POS tag of the token
                        String pos = token.get(PartOfSpeechAnnotation.class);
                        // this is the constituency information of the token
                        String constituency = String.join(",", constituencyTokens.get(wordIndex));


                        if (WikiParser.wikiLanguage.equals("en"))

                            lemma = token.get(LemmaAnnotation.class);
                        else
                            lemma = (String) dependencyTokens.get(wordIndex).get("lemma");

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

                        }//foreach token

                    }//foreach sentence
            }//foreach page
            bufferedOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

