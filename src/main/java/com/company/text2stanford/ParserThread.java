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
    ParserThread(String pathFile, StanfordCoreNLP coreParser, Map mateTools, jni.Parser desrParser,
                 ConcurrentMaltParserModel maltParser, Map<String, Object> freelingParser) {
        this.coreParser = coreParser;
        this.pathFile = pathFile;
        this.mateTools = mateTools;
        this.desrParser = desrParser;
        this.maltParser = maltParser;
        this.freelingParser = freelingParser;
    }

    private static Map<Integer, String> freelingTokenConstituents(int depth, TreeNode tr, List<String> constituency) {
        Word w;
        TreeNode child;
        long nch;
        Map<Integer, String> indexConstituency = new HashMap<>();
        // Indentation
//        for( int i = 0; i < depth; i++ ) {
//            System.out.print( "  " );
//        }

        nch = tr.numChildren();

        if (nch == 0) {
            // The node represents a leaf
//            if( tr.getInfo().isHead() ) {
//                System.out.print( "+" );
//            }
            w = tr.getInfo().getWord();
//            System.out.print("(" + w.getForm() + " " + w.getLemma() + " " + w.getTag() + " " + (w.getPosition() + 1) + ")\n");
            indexConstituency.put((int) w.getPosition(), String.join(",", constituency));
        } else {
            // The node probably represents a tree
//            if( tr.getInfo().isHead() ) {
//                System.out.print( "*" );
//            }

//            System.out.println(tr.getInfo().getLabel() + "_[");
            constituency.add(tr.getInfo().getLabel() + "_" + Integer.toString(tr.hashCode() % 100));
            for (int i = 0; i < nch; i++) {
                child = tr.nthChildRef(i);

                if (child != null) {
                    indexConstituency.putAll(freelingTokenConstituents(depth + 1, child, constituency));
                } else {
                    System.err.println("ERROR: Unexpected NULL child.");
                }
            }
//            for( int i = 0; i < depth; i++ ) {
//                System.out.print( "  " );
//            }
            constituency.remove(constituency.size() - 1);
//            System.out.println( "]" );
        }
        return indexConstituency;
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

    public Map<Integer, HashMap<String, String>> parseMalt(String[] words, String[] lemmas, String[] CPOStags, String[] POStags) throws MaltChainedException {
        Map<Integer, HashMap<String, String>> tokenDeps = new Utils.DefaultDict<>(HashMap.class);
        String[] tokens = new String[lemmas.length];
        for (int i = 0; i < words.length; i++)
            tokens[i] = String.format("%d\t%s\t%s\t%s\t%s", i + 1, words[i], lemmas[i], CPOStags[i], POStags[i]);
//            ConcurrentMaltParserModel miparso = createMaltParser("./resources/Spanish/espmalt-1.0.mco");
        ConcurrentDependencyGraph depGraph = this.maltParser.parse(tokens);
//            String[] fofo = this.maltParser.parseTokens(tokens);
        for (int i = 0; i < depGraph.nTokenNodes(); i++) {
            ConcurrentDependencyNode currentNode = depGraph.getTokenNode(i + 1);
            tokenDeps.get(i).put("headIndex", Integer.toString(currentNode.getHeadIndex()));
            tokenDeps.get(i).put("relation", currentNode.getLabel(7));
        }
//            ConcurrentUtils.printTokens(outputTokens);
        return tokenDeps;
    }

    public Map<Integer, HashMap> parseDeSR(String[] words, String[] lemmas, String[] CPOStags, String[] POStags,
                                           String[] feats) {

        final String[] atts = {"ID", "FORM", "LEMMA", "CPOSTAG", "POSTAG", "FEATS", "HEAD", "DEPREL"};
        ArrayList<String[]> listAttributesValues = new ArrayList<>();
        listAttributesValues.add(words);
        listAttributesValues.add(lemmas);
        listAttributesValues.add(CPOStags); //Coarse POS TAG
        listAttributesValues.add(POStags); // I don't have a fine POSTAG parser, so I will use the same as before
        listAttributesValues.add(feats);
        //listAttributesValues.add(POStags);
        jni.Language la = new jni.Language("es");
        jni.Corpus cc = jni.Corpus.create(la, "CoNLL");
        Map<Integer, HashMap> tokenDeps = new Utils.DefaultDict<>(HashMap.class);
        //Manually create a sentence
        jni.Sentence inputSentence = new jni.Sentence(la);
        jni.Sentence s2 = null;

        for (int w = 0; w < words.length; w++) {
            jni.Token tok = new Token(words[w], cc);
            tok.setAttribute("ID", "" + (w + 1));

            for (int a = 0; a < listAttributesValues.size(); ++a)
                tok.setAttribute(atts[a + 1], listAttributesValues.get(a)[w]);
            jni.TreeToken t = new jni.TreeToken(w + 1, tok);
            inputSentence.add(t);

        }
        // Finally parse after all the input preparation:

//            System.err.println("sent size: " + inputSentence.size());
        //        inputSentence.size();
        //            System.out.println("");

        s2 = desrParser.parse(inputSentence);

        for (int i = 0; i < s2.size(); i++) {
            jni.TreeToken currentToken = s2.get(i);
            int wordIndex = (int) currentToken.getId();
            tokenDeps.get(wordIndex).put("relation", currentToken.linkLabel());
            tokenDeps.get(wordIndex).put("headIndex", Integer.toString(currentToken.linkHead()));
//            tokenDeps.get(wordIndex).put("lemma", currentToken.get("LEMMA"));

        }
        //        System.out.println("Output:\n" + cc.toString(s2));
        //        System.out.println("Output:" + s2.get(0).get("HEAD").toString());
        //        System.out.println("Output:\n"+s2.get(0).linkLabel());
        return tokenDeps;
    }

    public Map<Integer, HashMap> mateTokenDependencies(List<CoreLabel> stanfordTokens) throws IOException {
        Map<Integer, HashMap> tokenDeps = new Utils.DefaultDict<>(HashMap.class);
        //This is for mate tools parser
        //We get the tokens and its posTags
//        Map<String, String[]> StanfordTokensAndPOSTags = getTokensAndPOSTags(stanfordTokens);
//
//        String[] phraseRoot = new String[stanfordTokens.size() + 1];
//        System.arraycopy(StanfordTokensAndPOSTags.get("forms"), 0, phraseRoot, 1, stanfordTokens.size());
//        phraseRoot[0] = CONLLReader09.ROOT;
//
//        SentenceData09 s = new SentenceData09();
//        s.init(phraseRoot);
//        s = mateTools.get("lemmatizer").apply(s);
////        s = mateTools.get("mtagger").apply(s);
//        s = mateTools.get("POStagger").apply(s);
//        s = mateTools.get("dependencyParser").apply(s);
//        //> This is here is to get the parse info from mate-tools. Unfortunately, mate parser is too slow.
//        for (int k = 0; k < s.length(); k++) {
//            tokenDeps.get(k + 1).put("relation", s.plabels[k]);
//            tokenDeps.get(k + 1).put("headIndex", Integer.toString(s.pheads[k]));
//            tokenDeps.get(k + 1).put("lemma", s.plemmas[k]);
//        }

        return tokenDeps;
    }

    public List<List<Map<String, String>>> freelingMaltParser(String text) throws IOException, MaltChainedException {
        Map<Integer, HashMap> tokenDeps = null;
//        Map<String, Object>  freelingParser = createFreelingParser();
        ListWord tokens = ((Tokenizer) this.freelingParser.get("tokenizer")).tokenize(text);
        ListSentence sentences = ((Splitter) this.freelingParser.get("splitter")).split(tokens, false);
        ((Maco) this.freelingParser.get("mtag")).analyze(sentences);
        ((ChartParser) this.freelingParser.get("constit")).analyze(sentences);


        List<List<Map<String, String>>> parseStore = new ArrayList<>();
        ListSentenceIterator sIt = new ListSentenceIterator(sentences);
        Map<String, ArrayList<String>> infoTokens = new Utils.DefaultDict<>();
        List<Map<String, String>> tempList = new ArrayList<>();
        Map<String, String> tempDict = new HashMap<>();
        while (sIt.hasNext()) {
            tempDict.clear();
            tempList.clear();
            infoTokens.clear();
            Sentence s = sIt.next();
            TreeNode tree = s.getParseTree();
            Map<Integer, String> tokenConstituencies = freelingTokenConstituents(0, tree, new ArrayList<>());
            ListWordIterator wIt = new ListWordIterator(s);

            while (wIt.hasNext()) {

                Word w = wIt.next();
                infoTokens.get("form").add(w.getForm());
                infoTokens.get("lemma").add(w.getLemma());
                infoTokens.get("pos").add(w.getTag());
                infoTokens.get("cpos").add(w.getTag().substring(0, 1));
//                System.out.print(w.getForm() + " " + w.getLemma() + " " + w.getTag());
//                System.out.print( " " + w.getSensesString() );
//                System.out.println();
//                tempList.add(tempDict);
            }// foreach token

            int sizeList = infoTokens.get("form").size();
//            tokenDeps = parseMalt(infoTokens.get("form").toArray(new String[sizeList]), infoTokens.get("lemma").toArray(new String[sizeList]),
//                    infoTokens.get("cpos").toArray(new String[sizeList]), infoTokens.get("pos").toArray(new String[sizeList]));

            for (int i = 0; i < sizeList; i++) {
                tempDict.put("form", infoTokens.get("form").get(i));
                tempDict.put("lemma", infoTokens.get("lemma").get(i));
                tempDict.put("pos", infoTokens.get("pos").get(i));
                tempDict.put("cpos", infoTokens.get("cpos").get(i));
//                tempDict.put("headIndex", (String)tokenDeps.get(i+1).get("headIndex"));
//                tempDict.put("relation", (String) tokenDeps.get(i+1).get("relation"));
                tempDict.put("constituents", tokenConstituencies.get(i));

                tempList.add(new HashMap<>(tempDict));

            }//foreach token (again)
            parseStore.add(new ArrayList<>(tempList));

        }//foreach sentence
        return parseStore;
    }

    @Deprecated
    public Map<Integer, HashMap<String, String>> combinedParser(List<CoreLabel> stanfordTokens) throws IOException, MaltChainedException {
        /**
         * This function takes the tokens of a phrase, inside a list of strings,  and returns
         * a dictionary of dictionaries: {wordIndex:{ "relation": subj", "headIndex": "2"}, ...}
         * Each word of the phrase has a dict with each dependency it belongs to and its corresponding head.
         * Using the mate-tools parser and either the DeSR or the Malt parser.
         * Mate-tools to get the lemmas and pos tags (which could be get with
         * Stanford parser). The actual dependecy with the other one. I do this because mate parser is very slow
         */


        //This is for mate tools parser
        //We get the tokens and its posTags
        Map<String, String[]> stanfordTokensAndPOSTags = getTokensAndPOSTags(stanfordTokens);

        String[] phraseRoot = new String[stanfordTokens.size() + 1];
        System.arraycopy(stanfordTokensAndPOSTags.get("forms"), 0, phraseRoot, 1, stanfordTokens.size());
        phraseRoot[0] = CONLLReader09.ROOT;


        SentenceData09 s = new SentenceData09();
        s.init(phraseRoot);
        s = mateTools.get("lemmatizer").apply(s);
        s = mateTools.get("POStagger").apply(s);
//        s = mateTools.get("mtagger").apply(s);

        String[] postagos = new String[19];
        postagos[0] = "SPS00";
        postagos[1] = "DA0MS0";
        postagos[2] = "NCMS000";
        postagos[3] = "SPS00";
        postagos[4] = "NP00000";
        postagos[5] = "DI0MS0";
        postagos[6] = "NCMS000";
        postagos[7] = "VMP00SM";
        postagos[8] = "VAIP3S0";
        postagos[9] = "VMP00SM";
        postagos[10] = "NCMS000";
        postagos[11] = "SPS00";
        postagos[12] = "VMN0000";
        postagos[13] = "SPS00";
        postagos[14] = "DI0MS0";
        postagos[15] = "NCMS000";
        postagos[16] = "SPS00";
        postagos[17] = "NCMP000";
        postagos[18] = "Fp";


        Map<Integer, HashMap<String, String>> tokenDeps = parseMalt(s.forms, s.plemmas, stanfordTokensAndPOSTags.get("CPOS"),
                stanfordTokensAndPOSTags.get("POS"));


//

        return tokenDeps;
    }

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
        try {
            parseWiki(pathFile);
        } catch (MaltChainedException e) {
            e.printStackTrace();
        }
//        parseOANCText(pathFile);
//        parseSemeval2007(pathFile);
        System.out.println("... DONE");

    }

    private void parseWiki(String pathFile) throws MaltChainedException {
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

                if (WikiParser.wikiLanguage.equals("es")) {
                    List<List<Map<String, String>>> freelingParse = freelingMaltParser(docText);
                    String line;
                    for (int sentenceIdx = 0; sentenceIdx < freelingParse.size(); sentenceIdx++) {
                        List<Map<String, String>> currentSentence = freelingParse.get(sentenceIdx);
                        int sentenceSize = currentSentence.size();
                        bufferedOut.write("%%#SEN\t" + Integer.toString(sentenceIdx + 1) + "\t" + Integer.toString(sentenceSize) + nline);
                        String head;
                        String dependency;
                        String constituents;
                        String lemma;
                        String word;
                        String cpos;
                        String pos;

                        Map<String, ArrayList<String>> infoTokens = new Utils.DefaultDict<>();
                        for (int tokenIdx = 0; tokenIdx < sentenceSize; tokenIdx++) {
                            Map<String, String> currentToken = currentSentence.get(tokenIdx);
                            infoTokens.get("form").add(currentToken.get("form"));
                            infoTokens.get("lemma").add(currentToken.get("lemma"));
                            infoTokens.get("pos").add(currentToken.get("pos"));
                            infoTokens.get("cpos").add(currentToken.get("cpos"));

                        }
                        Map<Integer, HashMap<String, String>> tokenDeps = parseMalt(infoTokens.get("form").toArray(new String[sentenceSize]), infoTokens.get("lemma").toArray(new String[sentenceSize]),
                                infoTokens.get("cpos").toArray(new String[sentenceSize]), infoTokens.get("pos").toArray(new String[sentenceSize]));


                        for (int tokenIdx = 0; tokenIdx < sentenceSize; tokenIdx++) {
                            Map<String, String> currentToken = currentSentence.get(tokenIdx);
                            word = currentToken.get("form");
                            lemma = currentToken.get("lemma");
                            pos = currentToken.get("pos");
                            cpos = currentToken.get("cpos");
                            constituents = currentToken.get("constituents");
                            head = tokenDeps.get(tokenIdx).get("headIndex");
                            dependency = tokenDeps.get(tokenIdx).get("relation");

//                            head = currentToken.get("headIndex");
//                            dependency = currentToken.get("dependency");
                            // create the line that will be written in the output
                            line = word + "\t" + lemma + "\t" + pos + "\t" + cpos + "\t" + constituents
                                    + "\t" + head + "\t" + dependency + nline;
                            bufferedOut.write(line);
                        }// endfor each token
                    }// endfor each sentence
                } else {
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
                        if (WikiParser.wikiLanguage.equals("en")) {

                            // this is the dependency graph of the current sentence
                            SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                            System.out.println(dependencies.toList());
                            dependencyTokens = coreNLPTokenDependencies(dependencies);
                        } else {//Otherwise, we use the mate and DeSR parsers.

                            dependencyTokens = mateTokenDependencies(listTokens);
                        }
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
                }// if english or other language, different approaches
            }//foreach page
            bufferedOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

