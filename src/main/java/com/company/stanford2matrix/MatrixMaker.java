package com.company.stanford2matrix;

import com.google.gson.Gson;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.company.stanford2matrix.Utils.*;

public class MatrixMaker {
    private static String pathFolder;
    private static String outputFolder;
    private static int column_j = 0;
    private static int row_i = 0; ///> The i index for the matrixContainer
    public int numSent = 0;
    Map<String, Integer> cDictAllTokens = new DefaultDict<String, Integer>();
    //    ArrayList<String> cListAllTokens = new ArrayList<>();
    ArrayList<Integer> averageLengthSentence = new ArrayList<>();
    //All this should not be part of the class... it should just have a MatrixContainer object
    private MatrixContainer matrixContainer;

    //Constructor
    MatrixMaker() {
        row_i = 1; ///> The i index for the matrixContainer
        column_j = 0; ///> The j index for the matrixContainer
        matrixContainer = new MatrixContainer();
    }

    public static void main(String[] args) throws InterruptedException, IOException, InvalidLengthsException {

        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("i", "input", true, "Input folder of parsed files");
//        options.addOption("o", "output", true, "Output folder of matrixContainer+metadata");
        MatrixMaker myMaker = new MatrixMaker();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("i")) {
                myMaker.setDataPath(line.getOptionValue("i"));
            } else
            //We cant continue if this is not set
            {
                System.out.println("Please give an input folder");
                return;
            }

//            if (line.hasOption("o")) {
//                myMaker.setOutputMatrixPath(line.getOptionValue("o"));
//            } else
//            //We cant continue if this is not set
//            {
//                System.out.println("Please give an output folder");
//                return;
//            }

        } catch (ParseException exp) {
            System.err.println("Parameter parsing failed.  Reason: " + exp.getMessage());
            return;
        }

        if (myMaker.getDataPath().contains("wiki") || myMaker.getDataPath().contains("oanc")
                || myMaker.getDataPath().contains("oanc")) {
//            dataPath = "/home_nfs/eric/esorianomorales/wikitest/extracted/BL";
//            dataPath = "/media/stuff/temp/extracted/BL";
//            dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/wikidata/AA";

            //        String dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/sentencedata";
            //        String dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/oanc/corpus";
            myMaker.runParseCorpus(true);
        } else if (myMaker.getDataPath().contains("semeval2007")) {
//            dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/semeval2007/task 02/key/data/";
            myMaker.runParseSemeval2007();
        } else if (myMaker.getDataPath().contains("semeval2010")) {
//          dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/semeval2010/test_data/";
            myMaker.runParseSemeval2010();
        }


    }

    public String getDataPath() {
        return pathFolder;
    }

    public void setDataPath(String dataPath) {
        pathFolder = dataPath;
    }

    public void setOutputMatrixPath(String outputPath) {
        outputFolder = outputPath;
    }

    public String getOutputPath() {
        return outputFolder;
    }

    private Map<String, ArrayList<Integer>> clauseToIndices(ArrayList<String> clauseNames) {
        Map<String, ArrayList<Integer>> columnsIndices = new LinkedHashMap<>();
        String lastSeen = clauseNames.get(0);

        for (String clause : clauseNames) {
            if (!clause.equals(lastSeen))
                column_j++;
            if (columnsIndices.containsKey(clause) && !columnsIndices.get(clause).contains(column_j))
                columnsIndices.get(clause).add(column_j);
            else
                columnsIndices.put(clause, new ArrayList(Arrays.asList(column_j)));
            lastSeen = clause;
            matrixContainer.cCols.add(column_j);
        }

        return columnsIndices;
    }

//    private Map<Integer, String> NPIndex2ColumnIndex()

    private final LinkedHashMap<Integer, String> getSelectedPhrases(ArrayList<String> clauses) {
        /*
        Receives the list of clauses a word belongs to. Returns a hashmap with the position of the clause
        and its type (NP, VP, etc)
         */
        LinkedHashMap<Integer, String> npPosition = new LinkedHashMap<>();
        Collections.reverse(clauses);
        for (int i = 0; i < clauses.size(); i++)
            if (clauses.get(i).contains("NP")) {
                npPosition.put(i, clauses.get(i));
            }
        return npPosition;
    }

    private final LinkedHashMap<Integer, String> getSelectedPhrasesHash(ArrayList<String> clauses) {
        LinkedHashMap<Integer, String> npHash = new LinkedHashMap<>();
//        Collections.reverse(clauses);
        for (int i = 0; i < clauses.size(); i++)
            if (clauses.get(i).contains("NP")) {
//                ArrayList<String> sublisto = new ArrayList<>(clauses.subList(0, i + 1));
                npHash.put(clauses.subList(0, i + 1).hashCode() % 1000, clauses.get(i));

            }
        return npHash;
    }

    /**
     * Function that transforms a list of dict NPhash : NP, like so [{968:NP_18, -784:NP_20}, {...}, ...], one in the
     * list per token found in the phrase. The output is a dict with the NPhash as keys and a list of tokenIds as values,
     * it is like this: {897: [2,4,7,9], 345:[1,3,5], ...}
     *
     * @param NPhash
     * @return
     */
    private OrderedDefaultDict<Integer, ArrayList<Integer>> hashNP2hashToken(ArrayList<LinkedHashMap<Integer, String>> NPhash) {
        OrderedDefaultDict<Integer, ArrayList<Integer>> hashTokenId = new OrderedDefaultDict<>(ArrayList.class);

        for (int i = 0; i < NPhash.size(); i++) {
            for (Map.Entry<Integer, String> entry : NPhash.get(i).entrySet()) {
                int hash = entry.getKey();
//                String NPtype = entry.getValue();
                hashTokenId.get(hash).add(i);
            }

        }

        return hashTokenId;
    }


    private final void addSentenceColumns(ArrayList<String> listTokens, int hashLength) {
//        System.out.println(listTokens.hashCode());
//        Set<String> hashTokens = new HashSet<>(listTokens);
//        listTokens = new ArrayList<>(hashTokens);

        String phrase = String.join(" ", listTokens);
        int hashcode;
        if (hashLength > 0)
            hashcode = listTokens.hashCode() % hashLength;
        else {
            hashcode = listTokens.hashCode();
//            hashcode = matrixContainer.sentenceID; // if I assign the hashcode equal to sentenceID, I will always have different sentences = no counts
            matrixContainer.sentenceID++;
        }
        int lSentenceDataIndex;
        int ngram_col;
        if (matrixContainer.cSentenceHashColumn.containsKey(hashcode)) {
            ngram_col = matrixContainer.cSentenceHashColumn.get(hashcode);
            lSentenceDataIndex = matrixContainer.cSentenceDataVectorIndex.get(ngram_col);
            for (int i = 0; i < listTokens.size(); i++)
                matrixContainer.cData.set(lSentenceDataIndex + i, matrixContainer.cData.get(lSentenceDataIndex + i) + 1);
        } else {
            matrixContainer.cSentenceHashColumn.put(hashcode, ++column_j);
            matrixContainer.cColumnSentenceHash.put(column_j, Integer.toString(hashcode)); ///> We save the inverted index
//            matrixContainer.cColumnSentenceWords.put(column_j, phrase);
            matrixContainer.cSentenceDataVectorIndex.put(column_j, matrixContainer.cCols.size());
            for (int i = 0; i < listTokens.size(); i++) {
                matrixContainer.cRows.add(matrixContainer.cTokenRow.get(listTokens.get(i)));
                matrixContainer.cCols.add(column_j);
                matrixContainer.cData.add(1);
            }

        }

    }


    private final void addDependenciesColumns(ArrayList<String> listTokens,
                                              Map<String, ArrayList<ArrayList<String>>> lDictDependencyHeadIndex) {
        String dependencyName;
        Integer dependencyNameCol;
        int lDependencyDataIndex;
        for (Map.Entry<String, ArrayList<ArrayList<String>>> entry : lDictDependencyHeadIndex.entrySet()) {
            String word = entry.getKey();
            ArrayList<ArrayList<String>> listRelations = entry.getValue();
            for (ArrayList<String> relation : listRelations) {
                String dependency = relation.get(0);
                int headIndex = Integer.parseInt(relation.get(1));
                String headWord;
                if (dependency.equals("root"))
                    headWord = "SENTENCE";
                else
                    headWord = listTokens.get(headIndex - 1);
                dependencyName = dependency + "_of_" + headWord; ///>Have to remove one cause listokens is 0 index based

                if (matrixContainer.cDependencyColumn.containsKey(dependencyName)) {
                    if (matrixContainer.cWordDependencyDataVectorIndex.containsKey(word + dependencyName)) {
                        lDependencyDataIndex = matrixContainer.cWordDependencyDataVectorIndex.get(word + dependencyName);
                        /// Get the index on the cData vector of the value that must be modified
                        matrixContainer.cData.set(lDependencyDataIndex, matrixContainer.cData.get(lDependencyDataIndex) + 1);
                    } else {
                        dependencyNameCol = matrixContainer.cDependencyColumn.get(dependencyName);
                        matrixContainer.cWordDependencyDataVectorIndex.put(word + dependencyName, matrixContainer.cCols.size());
                        matrixContainer.cRows.add(matrixContainer.cTokenRow.get(word));
                        matrixContainer.cCols.add(dependencyNameCol);
                        matrixContainer.cData.add(1);
                    }
                } else {
                    matrixContainer.cDependencyColumn.put(dependencyName, ++column_j);
                    /// Save the reverse index
                    matrixContainer.cColumnDependency.put(column_j, dependencyName);
                    matrixContainer.cWordDependencyDataVectorIndex.put(word + dependencyName, matrixContainer.cCols.size());
                    matrixContainer.cRows.add(matrixContainer.cTokenRow.get(word));
                    matrixContainer.cCols.add(column_j);
                    matrixContainer.cData.add(1);


                }
            }
        }
    }

    private Map<String, ArrayList<Integer>> addNgramsColumns(ArrayList<String> listTokens, int n) {
        /**
         * From a list of tokens, calculates its n ngrams and adds them to the matrixContainer.
         * It computes the ngrams and then updates the ijv vectors containing the matrixContainer, according to
         * the existence or not of the ngram in the matrixContainer.
         */
        Map<String, ArrayList<Integer>> ngramIndices = new DefaultDict<>();
        String ngram;
        Integer ngram_col;
        for (int i = 0; i < listTokens.size() - (n - 1); i++) {
            ngram = String.join("__", listTokens.subList(i, i + n));

            if (matrixContainer.cNgramColumn.containsKey(ngram)) {
                ngram_col = matrixContainer.cNgramColumn.get(ngram);
                int indexNgramCol = matrixContainer.cNgramColDataVectorIndex.get(ngram_col);
                for (int ii = 0; ii < n; ii++)
                    matrixContainer.cData.set(indexNgramCol + ii, matrixContainer.cData.get(indexNgramCol + ii) + 1);
            } else {
                matrixContainer.cNgramColumn.put(ngram, ++column_j);
                ngram_col = column_j;

                /// This here is to keep a dict ngram_col_index : column_position_index to easily and rapidly find the
                /// corresponding position index for a given ngram column index. IOW, a dict that maps the ngram columns to
                /// its corresponding index in the cColumns list.
                matrixContainer.cNgramColDataVectorIndex.put(ngram_col, matrixContainer.cCols.size());

                /// We add to the matrixContainer ijv vectors the new values. We iterate from 0 to n to add the values to the
                /// corresponding lines (words). So, a given trigram will have 1s in each of the three words that formed it.
                for (int j = i; j < i + n; j++) {
                    matrixContainer.cRows.add(matrixContainer.cTokenRow.get(listTokens.get(j)));
                    matrixContainer.cCols.add(ngram_col);
                    matrixContainer.cData.add(1);
                }
            }


        }


        return ngramIndices;
    }

    private final String getKinship(String constituency, int index, boolean includeCurrentNP) {
        /**
         * This returns the ancestor(s)  of the NP, which is located at the indicated index.
         * The ancestors are those constituents that appear after the current constituent.
         * I added a includeCurrentNP because there are cases where two words may be part of a NP
         * and this NP may have the same ancestors than another NP from a different word (that is
         * they are brothers) but the current word is not in the branch, so we distinguish by the
         * type of the current NP + the ancestors. I will not understand shit about this later....
         */
        if (includeCurrentNP)
            index = index + 1;
        ArrayList<String> listConstituents = new ArrayList<>(Arrays.asList(constituency.split(",")));
        index = listConstituents.size() - index;
        String kinship = String.join("_", listConstituents.subList(index, listConstituents.size()));

        return kinship;

    }


    //TODO: Make a method that receives a string (instead of a file path) and creates a matrixContainer from the text string.
    private void prepareSemeval2007MatrixNPs(String pathFile) throws IOException, InvalidLengthsException {
        String file = readFile(pathFile, true);

        ///DECLARATIONS
        ArrayList<String> lexelts = new ArrayList(Arrays.asList(file.split("%%#LEXELT\t")));
        Map<String, String> lpreClause;
        ArrayList<LinkedHashMap<Integer, String>> lListNPposition;
        ArrayList<LinkedHashMap<Integer, String>> lListHashNP;
        ArrayList<Integer> lListConstituencies;
        ArrayList<String> lListTokensPOSSeen;
        ArrayList<String> lListAllTokensPOS;
        Map<String, ArrayList<ArrayList<String>>> lDictDependencyHeadIndex;
        int ii = 0;


        if (lexelts.get(0).equals(""))
            lexelts.remove(0);

        for (String p : lexelts) {
            ii++;
            Map<String, Map<String, MatrixContainer>> lTargetWordInstancesMatrices = new HashMap<>();
            ArrayList<String> instances = new ArrayList(Arrays.asList(p.split("%%#INSTANCE\t")));
            String targetWordID = instances.get(0).trim();
//            String twPOStag = targetWordInfo[1];
            System.out.println("Target word: " + ii + " " + targetWordID);
            instances.remove(0);
            lTargetWordInstancesMatrices.put(targetWordID, new HashMap<>());

            for (String in : instances) {
                matrixContainer = new MatrixContainer();

                row_i = 1; ///> The i index for the matrixContainer
                column_j = 0; ///> The j index for the matrixContainer

                //TODO: Change this "SEN " to "SEN\t", but first re-run the stanford2matrix parser with this modif.
                ArrayList<String> sentences = new ArrayList(Arrays.asList(in.split("%%#SEN ")));
                String instanceID = sentences.get(0).trim();
                System.out.println("\tinstance: " + instanceID);
                sentences.remove(0);


                for (String s : sentences) {
                    ArrayList<String> lines = new ArrayList(Arrays.asList(s.split("\n"))); ///> List of lines that compose each sentence
                    lines.remove(0); ///> We remove the sentence ID from the list of lines

                    Set<String> lClausesSeen = new HashSet<>(); ///> List that contains which clauses (NP) are found during the iteration
                    lpreClause = new HashMap<>(); ///> Map that contains the subclauses and what prenodes compose them. NP_18:"DT_NN"
                    lListNPposition = new ArrayList<>(); ///> List that contains dictionaries containing the position of the NPs in each word constituency string [{1:NP_18}]
                    lListHashNP = new ArrayList<>();
                    lListConstituencies = new ArrayList<>();
                    lListTokensPOSSeen = new ArrayList<>();
                    lListAllTokensPOS = new ArrayList<>();
                    lDictDependencyHeadIndex = new DefaultDict<>();
                    Map<Integer, String> lNPHashNPName = new HashMap<>();
                    for (String l : lines) {///>Each line is a word
                        String[] splittedLine = l.split("\t");
                        String token = splittedLine[0];
                        String lemma = splittedLine[1];
                        String posTag = splittedLine[2];
                        String constituency = splittedLine[3];
                        String dependencyHead = splittedLine[4];
                        String dependency = splittedLine[5];
                        String token_pos = lemma + "_" + posTag;

                        lListAllTokensPOS.add(token_pos);

                        // HERE WE START. If the word is not a punctuation mark (PUNCT) or it is not part of a NP
                        if (dependency.equals("PUNCT") || !constituency.contains("NP"))
                            continue;

                        //We save the current word dependency and its head, if it is of interest: nsubj, dobj, or pobj
                        if (dependency.equals("nsubj") || dependency.equals("dobj") || dependency.equals("pobj")) {
                            //                        lDictDependencyHeadIndex.get(token_pos).add(new HashMap<String, Integer>(){{put(dependency, Integer.parseInt(dependencyHead));}});
                            lDictDependencyHeadIndex.get(token_pos).add(new ArrayList<>(Arrays.asList(dependency, dependencyHead)));
                        }

                        /***
                         * 1. Get the token and store it in a dictionary string:int, with its row index as value:
                         *      {"the_DT":0, "car_NN":1, ...}
                         *  1.a Add to the row list the current row i. rows[0,0,1,1...] for the i vector of the ijv sparse matrixContainer
                         */

                        if (!matrixContainer.cTokenRow.containsKey(token_pos)) {
                            ///>This is the dict with the pos_tag : row_index
                            matrixContainer.cTokenRow.put(token_pos, row_i);
                            ///> Save inverted index
                            matrixContainer.cRowToken.put(row_i, token_pos);
                            matrixContainer.cPOSToken.get(posTag).add(row_i);
                            row_i++;

                        }


                        lListTokensPOSSeen.add(token_pos);
                        /**
                         * 2. Using the constituency results, we find all the NPs clauses contained in the phrase.
                         *      We discard, at the beginning, any other type of clause (VP, ADJP, PP, PRP, etc).
                         *
                         */

                        ArrayList<String> clauses = new ArrayList(Arrays.asList(constituency.split(",")));

                        LinkedHashMap<Integer, String> lNPposition = getSelectedPhrases(clauses);
                        LinkedHashMap<Integer, String> lNPsHashes = getSelectedPhrasesHash(clauses);
                        /// Here we create a dict that maps NP hash to NP name: {-917:NP_18, 587:NP_20, ...}
                        for (Map.Entry<Integer, String> e : lNPsHashes.entrySet())
                            if (!lNPHashNPName.containsKey(e.getKey()))
                                lNPHashNPName.put(e.getKey(), e.getValue());

                        lListNPposition.add(lNPposition);
                        lListHashNP.add(lNPsHashes);
                        /**
                         * We get the preclauses of each of the NPs this token belongs to
                         */
                        for (Map.Entry<Integer, String> entry : lNPposition.entrySet()) {
                            Integer index_j = entry.getKey();
                            String targetClause = entry.getValue();

                            String clauseInitials = targetClause.split("_")[0];
                            lClausesSeen.add(clauseInitials);
                            /// We add hashed code of ancestors to distinguish NPs below
                            lListConstituencies.add(getKinship(constituency, index_j, true).hashCode() % 1000);
                            /**
                             * 2.1 We get the tags of what constitutes the targetClause. Such that:
                             * a dictionary str:str with NP_XX as key and a key describing its components as values:
                             * {"NP_18":"DET_NN_JJ', 'NP_70':'NP_NN',...}
                             *
                             *
                             */
                            //TODO: I could store this preClause structure for later use, as a MatrixContainer property
                            String tempPreClause;
                            /// There is actually a preClause  (e.g., a PRP or a NP). This check has to do with the length of the
                            /// constituencies string. If there are more elements, that indicates that there is another parent.
                            /// if not, there is only the pos tag left.
                            if (clauses.size() > index_j + 1)
                                tempPreClause = clauses.get(index_j + 1).split("_")[0];
                            else
                                tempPreClause = posTag; ///> There is no preClause. We take the POS tag.

                            if (lpreClause.containsKey(targetClause)) {
                                if (!lpreClause.get(targetClause).contains(tempPreClause))
                                    lpreClause.put(targetClause, lpreClause.get(targetClause) + ":" + tempPreClause);
                            } else
                                lpreClause.put(targetClause, tempPreClause);


                        }
                    }// end of current line. Sentence is completely read.
                    /**
                     * 3. Once the complete pass over the sentence is done, we get what represents each column.
                     * 3.1 We get the columns for each different type of clause. In a dict str:int. Keys are
                     * the clauses, columns are the values: {"NP_18": [1,3,5,7], "VP_70":[2,4], ...}
                     */
                    if (lClausesSeen.isEmpty())
                        continue;
                    ///Converts lListHashNP (a list of dicts containing all the NPs hashchode (569) the words occur in as key
                    /// and  the NP name (NP_18) as value) into a dict with the NP hashcode as key and the token indices of thos tokens
                    /// that are aprt of said NP.
                    Map<Integer, ArrayList<Integer>> lHashTokenIds = hashNP2hashToken(lListHashNP);
                    DefaultDict<String, HashSet<Integer>> lSubClausesColumns = new DefaultDict<>(HashSet.class);

                    /**
                     * Here we iterate for each different NP (different even if they are the same (NP_18 = NP_18), they are still made
                     * up from different words)
                     * We get the hash of the NP, we get the local indices of the words that build it. Then we get the real matrixContainer indices
                     * for these tokens. Then we check if we already stocked this NP with these same words. If yes, we update the values
                     * of cData. If not, we add it to cData and cRow and cCols
                     */
                    int np_col;
                    for (Map.Entry<Integer, ArrayList<Integer>> entry : lHashTokenIds.entrySet()) { ///> This is per each type of NP i.e., for each NP hashcode
                        int hashNP = entry.getKey();
                        ArrayList<Integer> words = entry.getValue();///> List of local indices (not the matrixContainer indices) that appear in this NP
                        int n = words.size();
                        ArrayList<Integer> wordIndices = new ArrayList<>();
                        ArrayList<String> wordTokens = new ArrayList<>();
                        for (int w = 0; w < n; w++) {
                            wordIndices.add(matrixContainer.cTokenRow.get(lListTokensPOSSeen.get(words.get(w))));
                            wordTokens.add(lListTokensPOSSeen.get(words.get(w)));
                        }
                        /// Here we create a hash id that will identify these particular words as well as the type of NP
                        //                    int keyNP = hashNP + (wordIndices.hashCode() % 1000
                        String keyNP = lNPHashNPName.get(hashNP) + wordTokens.toString();
                        // If this NP type plus these specific tokens have been seen before, we update the count (its value in the matrixContainer)
                        if (matrixContainer.cNPstringColumn.containsKey(keyNP)) {
                            np_col = matrixContainer.cNPstringColumn.get(keyNP);
                            int indexNPCol = matrixContainer.cNPColVectorIndex.get(np_col);
                            /// Update values in cData
                            for (int i = 0; i < n; i++)
                                matrixContainer.cData.set(indexNPCol + i, matrixContainer.cData.get(indexNPCol + i) + 1);

                        } else { ///> Else, we create a new matrixContainer column
                            matrixContainer.cNPstringColumn.put(keyNP, ++column_j);///> Dict with the word+np hash : column_index.
                            np_col = column_j;

                            matrixContainer.cSubClausesColumns.get(lpreClause.get(lNPHashNPName.get(hashNP))).add(np_col);///> Dict { NP_19:[1,3,5,7], NP_20:[2,6,8,10], ...}
                            lSubClausesColumns.get(lNPHashNPName.get(hashNP)).add(np_col); ///> Same as before, but local
                            matrixContainer.cNPColVectorIndex.put(np_col, matrixContainer.cCols.size()); ///> Dict for fast vector index searching given a column value. answers: Where in the vector is this value??
                            for (int j = 0; j < n; j++) {
                                matrixContainer.cRows.add(wordIndices.get(j));
                                matrixContainer.cCols.add(np_col);
                                matrixContainer.cData.add(1);
                            }
                        }

                    }
                    //Here we add the dependencies
                    addDependenciesColumns(lListAllTokensPOS, lDictDependencyHeadIndex);

                    //Here we add the sentence columns
                    addSentenceColumns(lListTokensPOSSeen, 100000);//1865652

                    //Here we add the ngrams columns
                    //                addNgramsColumns(lListTokensPOSSeen, 3);

                    if (matrixContainer.cRows.size() != matrixContainer.cCols.size())
                        throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something is wrong...");

                    /** Here we save the information about the columns, and the constituents each phrase is made of
                     * **/
                    for (String cl : lClausesSeen) {
                        for (Map.Entry<String, String> entry : lpreClause.entrySet()) {
                            String clause = entry.getKey();
                            String clauseComponents = entry.getValue();
                            matrixContainer.cClauseSubClauseColumns.get(cl).get(clauseComponents).addAll(lSubClausesColumns.get(clause));
                        }

                    }

                }//for each sentence
                /// Save the inverted indices
                matrixContainer.cColumnSubClause = invertMapOfSets(matrixContainer.cSubClausesColumns);
                matrixContainer.cTokenPOS = invertMapOfLists(matrixContainer.cPOSToken);
                /// Save the market format matrixContainer also
//                matrixContainer.cMarketFormatMatrix = saveMatrixMarketFormat(pathFolder + "/../matrixContainer/MMMatrix", matrixContainer, false);
                ///> Delete the row, cols and data arrays to save space. UPDATE: This is not a good idea. I can generate
                /// the matrixContainer faster with just the arrays.
//                matrixContainer.cRows.clear();
//                matrixContainer.cCols.clear();
//                matrixContainer.cData.clear();
//                lTargetWordInstancesMatrices.get(targetWord).put(instanceID, new JSONGraphContainer(matrixContainer));
                lTargetWordInstancesMatrices.get(targetWordID).put(instanceID, matrixContainer);
            }//for each instance
            Gson gson = new Gson();
            System.out.print("Saving " + targetWordID + " JSON file...");
            String sTargetWordInstancesMatrices = gson.toJson(lTargetWordInstancesMatrices);
            saveTextFile(this.pathFolder + "/../matrixContainer/" + "semeval2007_" + targetWordID, sTargetWordInstancesMatrices, ".json");
            System.out.println("Done");
        }// for each targetword
    }

    private void prepareSemeval2010TestMatrixNPs(String pathFile) throws IOException, InvalidLengthsException {
        String file = readFile(pathFile, true);
        String parentDirectory = new File(pathFile).getParentFile().getCanonicalPath();

        ///DECLARATIONS
        ArrayList<String> instances = new ArrayList(Arrays.asList(file.split("%%#INSTANCE\t")));
        Map<String, String> lpreClause;
        ArrayList<LinkedHashMap<Integer, String>> lListNPposition;
        ArrayList<LinkedHashMap<Integer, String>> lListHashNP;
        ArrayList<Integer> lListConstituencies;
        ArrayList<String> lListTokensPOSSeen;
        ArrayList<String> lListAllTokensPOS;
        Map<String, ArrayList<ArrayList<String>>> lDictDependencyHeadIndex;
        int ii = 0;

        String instanceID = null;
        if (instances.get(0).equals(""))
            instances.remove(0);

        String targetWord = instances.get(0).split("\n")[0];
        targetWord = targetWord.split("\\.")[0] + "." + targetWord.split("\\.")[1];

        Map<String, Map<String, MatrixContainer>> lTargetWordInstancesMatrices = new HashMap<>();
        lTargetWordInstancesMatrices.put(targetWord, new HashMap<>());

        for (String inst : instances) {
            ii++;
//            ArrayList<String> instances = new ArrayList(Arrays.asList(p.split("%%#INSTANCE")));
            ArrayList<String> sentences = new ArrayList(Arrays.asList(inst.split("%%#SEN\t")));

//            String twPOStag = targetWordInfo[1];
//            System.out.println("Target word: " + ":\t" + targetWord);
            instanceID = sentences.get(0).trim();
            System.out.println("\tinstance: " + instanceID);
            sentences.remove(0);

            matrixContainer = new MatrixContainer();

            row_i = 1; ///> The i index for the matrixContainer
            column_j = 0; ///> The j index for the matrixContainer


            for (String s : sentences) {
                ArrayList<String> lines = new ArrayList(Arrays.asList(s.split("\n"))); ///> List of lines that compose each sentence
                lines.remove(0); ///> We remove the sentence ID from the list of lines

                Set<String> lClausesSeen = new HashSet<>(); ///> List that contains which clauses (NP) are found during the iteration
                lpreClause = new HashMap<>(); ///> Map that contains the subclauses and what prenodes compose them. NP_18:"DT_NN"
                lListNPposition = new ArrayList<>(); ///> List that contains dictionaries containing the position of the NPs in each word constituency string [{1:NP_18}]
                lListHashNP = new ArrayList<>();
                lListConstituencies = new ArrayList<>();
                lListTokensPOSSeen = new ArrayList<>();
                lListAllTokensPOS = new ArrayList<>();
                lDictDependencyHeadIndex = new DefaultDict<>();
                Map<Integer, String> lNPHashNPName = new HashMap<>();
                for (String l : lines) {///>Each line is a word
                    String[] splittedLine = l.split("\t");
                    String token = splittedLine[0];
                    String lemma = splittedLine[1];
                    String posTag = splittedLine[2];
                    String constituency = splittedLine[3];
                    String dependencyHead = splittedLine[4];
                    String dependency = splittedLine[5];
                    String token_pos = lemma + "_" + posTag;

                    lListAllTokensPOS.add(token_pos);

                    // HERE WE START. If the word is not a punctuation mark (PUNCT) or it is not part of a NP
                    if (dependency.equals("PUNCT") || !constituency.contains("NP"))
                        continue;

                    //We save the current word dependency and its head, if it is of interest: nsubj, dobj, or pobj
//                    if (dependency.equals("nsubj") || dependency.equals("dobj") || dependency.equals("pobj")) {
                        //                        lDictDependencyHeadIndex.get(token_pos).add(new HashMap<String, Integer>(){{put(dependency, Integer.parseInt(dependencyHead));}});
                    lDictDependencyHeadIndex.get(token_pos).add(new ArrayList<>(Arrays.asList(dependency, dependencyHead)));
//                    }

                    /***
                     * 1. Get the token and store it in a dictionary string:int, with its row index as value:
                     *      {"the_DT":0, "car_NN":1, ...}
                     *  1.a Add to the row list the current row i. rows[0,0,1,1...] for the i vector of the ijv sparse matrixContainer
                     */

                    if (!matrixContainer.cTokenRow.containsKey(token_pos)) {
                        ///>This is the dict with the pos_tag : row_index
                        matrixContainer.cTokenRow.put(token_pos, row_i);
                        ///> Save inverted index
                        matrixContainer.cRowToken.put(row_i, token_pos);
                        matrixContainer.cPOSToken.get(posTag).add(row_i);
                        row_i++;

                    }


                    lListTokensPOSSeen.add(token_pos);
                    /**
                     * 2. Using the constituency results, we find all the NPs clauses contained in the phrase.
                     *      We discard, at the beginning, any other type of clause (VP, ADJP, PP, PRP, etc).
                     *
                     */

                    ArrayList<String> clauses = new ArrayList(Arrays.asList(constituency.split(",")));

                    LinkedHashMap<Integer, String> lNPposition = getSelectedPhrases(clauses);
                    LinkedHashMap<Integer, String> lNPsHashes = getSelectedPhrasesHash(clauses);
                    /// Here we create a dict that maps NP hash to NP name: {-917:NP_18, 587:NP_20, ...}
                    for (Map.Entry<Integer, String> e : lNPsHashes.entrySet())
                        if (!lNPHashNPName.containsKey(e.getKey()))
                            lNPHashNPName.put(e.getKey(), e.getValue());

                    lListNPposition.add(lNPposition);
                    lListHashNP.add(lNPsHashes);
                    /**
                     * We get the preclauses of each of the NPs this token belongs to
                     */
                    for (Map.Entry<Integer, String> entry : lNPposition.entrySet()) {
                        Integer index_j = entry.getKey();
                        String targetClause = entry.getValue();

                        String clauseInitials = targetClause.split("_")[0];
                        lClausesSeen.add(clauseInitials);
                        /// We add hashed code of ancestors to distinguish NPs below
                        lListConstituencies.add(getKinship(constituency, index_j, true).hashCode() % 1000);
                        /**
                         * 2.1 We get the tags of what constitutes the targetClause. Such that:
                         * a dictionary str:str with NP_XX as key and a key describing its components as values:
                         * {"NP_18":"DET_NN_JJ', 'NP_70':'NP_NN',...}
                         *
                         *
                         */
                        //TODO: I could store this preClause structure for later use, as a MatrixContainer property
                        String tempPreClause;
                        /// There is actually a preClause  (e.g., a PRP or a NP). This check has to do with the length of the
                        /// constituencies string. If there are more elements, that indicates that there is another parent.
                        /// if not, there is only the pos tag left.
                        if (clauses.size() > index_j + 1)
                            tempPreClause = clauses.get(index_j + 1).split("_")[0];
                        else
                            tempPreClause = posTag; ///> There is no preClause. We take the POS tag.

                        if (lpreClause.containsKey(targetClause)) {
                            if (!lpreClause.get(targetClause).contains(tempPreClause))
                                lpreClause.put(targetClause, lpreClause.get(targetClause) + ":" + tempPreClause);
                        } else
                            lpreClause.put(targetClause, tempPreClause);


                    }
                }// end of current line. Sentence is completely read.
                /**
                 * 3. Once the complete pass over the sentence is done, we get what represents each column.
                 * 3.1 We get the columns for each different type of clause. In a dict str:int. Keys are
                 * the clauses, columns are the values: {"NP_18": [1,3,5,7], "VP_70":[2,4], ...}
                 */
                if (lClausesSeen.isEmpty())
                    continue;
                ///Converts lListHashNP (a list of dicts containing all the NPs hashchode (569) the words occur in as key
                /// and  the NP name (NP_18) as value) into a dict with the NP hashcode as key and the token indices of thos tokens
                /// that are aprt of said NP.
                Map<Integer, ArrayList<Integer>> lHashTokenIds = hashNP2hashToken(lListHashNP);
                DefaultDict<String, HashSet<Integer>> lSubClausesColumns = new DefaultDict<>(HashSet.class);

                /**
                 * Here we iterate for each different NP (different even if they are the same (NP_18 = NP_18), they are still made
                 * up from different words)
                 * We get the hash of the NP, we get the local indices of the words that build it. Then we get the real matrixContainer indices
                 * for these tokens. Then we check if we already stocked this NP with these same words. If yes, we update the values
                 * of cData. If not, we add it to cData and cRow and cCols
                 */
                int np_col;
                for (Map.Entry<Integer, ArrayList<Integer>> entry : lHashTokenIds.entrySet()) { ///> This is per each type of NP i.e., for each NP hashcode
                    int hashNP = entry.getKey();
                    ArrayList<Integer> words = entry.getValue();///> List of local indices (not the matrixContainer indices) that appear in this NP
                    int n = words.size();
                    ArrayList<Integer> wordIndices = new ArrayList<>();
                    ArrayList<String> wordTokens = new ArrayList<>();
                    for (int w = 0; w < n; w++) {
                        wordIndices.add(matrixContainer.cTokenRow.get(lListTokensPOSSeen.get(words.get(w))));
                        wordTokens.add(lListTokensPOSSeen.get(words.get(w)));
                    }
                    /// Here we create a hash id that will identify these particular words as well as the type of NP
                    //                    int keyNP = hashNP + (wordIndices.hashCode() % 1000
                    String keyNP = lNPHashNPName.get(hashNP) + wordTokens.toString();
                    // If this NP type plus these specific tokens have been seen before, we update the count (its value in the matrixContainer)
                    if (matrixContainer.cNPstringColumn.containsKey(keyNP)) {
                        np_col = matrixContainer.cNPstringColumn.get(keyNP);
                        int indexNPCol = matrixContainer.cNPColVectorIndex.get(np_col);
                        /// Update values in cData
                        for (int i = 0; i < n; i++)
                            matrixContainer.cData.set(indexNPCol + i, matrixContainer.cData.get(indexNPCol + i) + 1);

                    } else { ///> Else, we create a new matrixContainer column
                        matrixContainer.cNPstringColumn.put(keyNP, ++column_j);///> Dict with the word+np hash : column_index.
                        np_col = column_j;

                        matrixContainer.cSubClausesColumns.get(lpreClause.get(lNPHashNPName.get(hashNP))).add(np_col);///> Dict { NP_19:[1,3,5,7], NP_20:[2,6,8,10], ...}
                        lSubClausesColumns.get(lNPHashNPName.get(hashNP)).add(np_col); ///> Same as before, but local
                        matrixContainer.cNPColVectorIndex.put(np_col, matrixContainer.cCols.size()); ///> Dict for fast vector index searching given a column value. answers: Where in the vector is this value??
                        for (int j = 0; j < n; j++) {
                            matrixContainer.cRows.add(wordIndices.get(j));
                            matrixContainer.cCols.add(np_col);
                            matrixContainer.cData.add(1);
                        }
                    }

                }
                //Here we add the dependencies
                addDependenciesColumns(lListAllTokensPOS, lDictDependencyHeadIndex);

                //Here we add the sentence columns
                addSentenceColumns(lListTokensPOSSeen, 0);//1865652

                //Here we add the ngrams columns
                //                addNgramsColumns(lListTokensPOSSeen, 3);

                if (matrixContainer.cRows.size() != matrixContainer.cCols.size())
                    throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something is wrong...");

                /** Here we save the information about the columns, and the constituents each phrase is made of
                 * **/
                for (String cl : lClausesSeen) {
                    for (Map.Entry<String, String> entry : lpreClause.entrySet()) {
                        String clause = entry.getKey();
                        String clauseComponents = entry.getValue();
                        matrixContainer.cClauseSubClauseColumns.get(cl).get(clauseComponents).addAll(lSubClausesColumns.get(clause));
                    }

                }

            }//for each sentence
            /// Save the inverted indices

            matrixContainer.cColumnSubClause = invertMapOfSets(matrixContainer.cSubClausesColumns);
            matrixContainer.cTokenPOS = invertMapOfLists(matrixContainer.cPOSToken);
            /// Save the market format matrixContainer also
//                matrixContainer.cMarketFormatMatrix = saveMatrixMarketFormat(pathFolder + "/../matrixContainer/MMMatrix", matrixContainer, false);
            ///> Delete the row, cols and data arrays to save space. UPDATE: This is not a good idea. I can generate
            /// the matrixContainer faster with just the arrays.
//                matrixContainer.cRows.clear();
//                matrixContainer.cCols.clear();
//                matrixContainer.cData.clear();
//                lTargetWordInstancesMatrices.get(targetWord).put(instanceID, new JSONGraphContainer(matrixContainer));
            lTargetWordInstancesMatrices.get(targetWord).put(instanceID, matrixContainer);
        }
        //}//for each instance
        Gson gson = new Gson();
        System.out.print("Saving " + targetWord + " JSON file...");
        String sTargetWordInstancesMatrices = gson.toJson(lTargetWordInstancesMatrices);
        saveTextFile(parentDirectory + "/" + targetWord + "_metamatrix", sTargetWordInstancesMatrices, ".json");
        System.out.println("Done");
        matrixContainer = null;

    }


    private void prepareSemeval2010TrainMatrixNPs(String pathFile) throws IOException, InvalidLengthsException {
        String file = readFile(pathFile, true);
        String parentDirectory = new File(pathFile).getParentFile().getCanonicalPath();

        ///DECLARATIONS
        ArrayList<String> instances = new ArrayList(Arrays.asList(file.split("%%#INSTANCE\t")));
        Map<String, String> lpreClause;
        ArrayList<LinkedHashMap<Integer, String>> lListNPposition;
        ArrayList<LinkedHashMap<Integer, String>> lListHashNP;
        ArrayList<Integer> lListConstituencies;
        ArrayList<String> lListTokensPOSSeen;
        ArrayList<String> lListAllTokensPOS;
        Map<String, ArrayList<ArrayList<String>>> lDictDependencyHeadIndex;

        String instanceID;
        if (instances.get(0).equals(""))
            instances.remove(0);

        String targetWord = instances.get(0).split("\n")[0];
        targetWord = targetWord.split("\\.")[0] + "." + targetWord.split("\\.")[1];

        Map<String, MatrixContainer> lTargetWordInstancesMatrix = new HashMap<>();
//        lTargetWordInstancesMatrices.put(targetWord, new HashMap<>());

        matrixContainer = new MatrixContainer();

        row_i = 1; ///> The i index for the matrixContainer
        column_j = 0; ///> The j index for the matrixContainer
        for (String inst : instances) {
            //            ArrayList<String> instances = new ArrayList(Arrays.asList(p.split("%%#INSTANCE")));
            ArrayList<String> sentences = new ArrayList(Arrays.asList(inst.split("%%#SEN\t")));

//            String twPOStag = targetWordInfo[1];
//            System.out.println("Target word: " + ":\t" + targetWord);
            instanceID = sentences.get(0).trim();
            System.out.println("\tinstance: " + instanceID);
            sentences.remove(0);


            for (String s : sentences) {
                ArrayList<String> lines = new ArrayList(Arrays.asList(s.split("\n"))); ///> List of lines that compose each sentence
                lines.remove(0); ///> We remove the sentence ID from the list of lines

                Set<String> lClausesSeen = new HashSet<>(); ///> List that contains which clauses (NP) are found during the iteration
                lpreClause = new HashMap<>(); ///> Map that contains the subclauses and what prenodes compose them. NP_18:"DT_NN"
                lListNPposition = new ArrayList<>(); ///> List that contains dictionaries containing the position of the NPs in each word constituency string [{1:NP_18}]
                lListHashNP = new ArrayList<>();
                lListConstituencies = new ArrayList<>();
                lListTokensPOSSeen = new ArrayList<>();
                lListAllTokensPOS = new ArrayList<>();
                lDictDependencyHeadIndex = new DefaultDict<>();
                Map<Integer, String> lNPHashNPName = new HashMap<>();
                for (String l : lines) {///>Each line is a word
                    String[] splittedLine = l.split("\t");
                    String token = splittedLine[0];
                    String lemma = splittedLine[1];
                    String posTag = splittedLine[2];
                    String constituency = splittedLine[3];
                    String dependencyHead = splittedLine[4];
                    String dependency = splittedLine[5];
//                    String token_pos = lemma + "_" + posTag;
                    String token_pos = lemma;
                    lListAllTokensPOS.add(token_pos);

                    // HERE WE START. If the word is not a punctuation mark (PUNCT)
                    if (dependency.equals("PUNCT"))
                        continue;

                    //We save the current word dependency and its head, if it is of interest: nsubj, dobj, or pobj
//                    if (dependency.equals("nsubj") || dependency.equals("dobj") || dependency.equals("pobj")) {
                    //                        lDictDependencyHeadIndex.get(token_pos).add(new HashMap<String, Integer>(){{put(dependency, Integer.parseInt(dependencyHead));}});
                    lDictDependencyHeadIndex.get(token_pos).add(new ArrayList<>(Arrays.asList(dependency, dependencyHead)));
//                    }

                    /***
                     * 1. Get the token and store it in a dictionary string:int, with its row index as value:
                     *      {"the_DT":0, "car_NN":1, ...}
                     *  1.a Add to the row list the current row i. rows[0,0,1,1...] for the i vector of the ijv sparse matrixContainer
                     */

                    if (!matrixContainer.cTokenRow.containsKey(token_pos)) {
                        ///>This is the dict with the pos_tag : row_index
                        matrixContainer.cTokenRow.put(token_pos, row_i);
                        ///> Save inverted index
                        matrixContainer.cRowToken.put(row_i, token_pos);
                        matrixContainer.cPOSToken.get(posTag).add(row_i);
                        row_i++;

                    }


                    lListTokensPOSSeen.add(token_pos);
                    /**
                     * 2. Using the constituency results, we find all the NPs clauses contained in the phrase.
                     *      We discard, at the beginning, any other type of clause (VP, ADJP, PP, PRP, etc).
                     *
                     */

                    ArrayList<String> clauses = new ArrayList(Arrays.asList(constituency.split(",")));

                    LinkedHashMap<Integer, String> lNPposition = getSelectedPhrases(clauses);
                    LinkedHashMap<Integer, String> lNPsHashes = getSelectedPhrasesHash(clauses);
                    /// Here we create a dict that maps NP hash to NP name: {-917:NP_18, 587:NP_20, ...}
                    for (Map.Entry<Integer, String> e : lNPsHashes.entrySet())
                        if (!lNPHashNPName.containsKey(e.getKey()))
                            lNPHashNPName.put(e.getKey(), e.getValue());

                    lListNPposition.add(lNPposition);
                    lListHashNP.add(lNPsHashes);
                    /**
                     * We get the preclauses of each of the NPs this token belongs to
                     */
                    for (Map.Entry<Integer, String> entry : lNPposition.entrySet()) {
                        Integer index_j = entry.getKey();
                        String targetClause = entry.getValue();

                        String clauseInitials = targetClause.split("_")[0];
                        lClausesSeen.add(clauseInitials);
                        /// We add hashed code of ancestors to distinguish NPs below
                        lListConstituencies.add(getKinship(constituency, index_j, true).hashCode() % 1000);
                        /**
                         * 2.1 We get the tags of what constitutes the targetClause. Such that:
                         * a dictionary str:str with NP_XX as key and a key describing its components as values:
                         * {"NP_18":"DET_NN_JJ', 'NP_70':'NP_NN',...}
                         *
                         *
                         */
                        //TODO: I could store this preClause structure for later use, as a MatrixContainer property
                        String tempPreClause;
                        /// There is actually a preClause  (e.g., a PRP or a NP). This check has to do with the length of the
                        /// constituencies string. If there are more elements, that indicates that there is another parent.
                        /// if not, there is only the pos tag left.
                        if (clauses.size() > index_j + 1)
                            tempPreClause = clauses.get(index_j + 1).split("_")[0];
                        else
                            tempPreClause = posTag; ///> There is no preClause. We take the POS tag.

                        if (lpreClause.containsKey(targetClause)) {
                            if (!lpreClause.get(targetClause).contains(tempPreClause))
                                lpreClause.put(targetClause, lpreClause.get(targetClause) + ":" + tempPreClause);
                        } else
                            lpreClause.put(targetClause, tempPreClause);


                    }
                }// end of current line. Sentence is completely read.
                /**
                 * 3. Once the complete pass over the sentence is done, we get what represents each column.
                 * 3.1 We get the columns for each different type of clause. In a dict str:int. Keys are
                 * the clauses, columns are the values: {"NP_18": [1,3,5,7], "VP_70":[2,4], ...}
                 */
                if (lClausesSeen.isEmpty())
                    continue;
                ///Converts lListHashNP (a list of dicts containing all the NPs hashchode (569) the words occur in as key
                /// and  the NP name (NP_18) as value) into a dict with the NP hashcode as key and the token indices of thos tokens
                /// that are aprt of said NP.
                Map<Integer, ArrayList<Integer>> lHashTokenIds = hashNP2hashToken(lListHashNP);
                DefaultDict<String, HashSet<Integer>> lSubClausesColumns = new DefaultDict<>(HashSet.class);

                /**
                 * Here we iterate for each different NP (different even if they are the same (NP_18 = NP_18), they are still made
                 * up from different words)
                 * We get the hash of the NP, we get the local indices of the words that build it. Then we get the real matrixContainer indices
                 * for these tokens. Then we check if we already stocked this NP with these same words. If yes, we update the values
                 * of cData. If not, we add it to cData and cRow and cCols
                 */
                int np_col;
                for (Map.Entry<Integer, ArrayList<Integer>> entry : lHashTokenIds.entrySet()) { ///> This is per each type of NP i.e., for each NP hashcode
                    int hashNP = entry.getKey();
                    ArrayList<Integer> words = entry.getValue();///> List of local indices (not the matrixContainer indices) that appear in this NP
                    int n = words.size();
                    ArrayList<Integer> wordIndices = new ArrayList<>();
                    ArrayList<String> wordTokens = new ArrayList<>();
                    for (int w = 0; w < n; w++) {
                        wordIndices.add(matrixContainer.cTokenRow.get(lListTokensPOSSeen.get(words.get(w))));
                        wordTokens.add(lListTokensPOSSeen.get(words.get(w)));
                    }
                    /// Here we create a hash id that will identify these particular words as well as the type of NP
                    //                    int keyNP = hashNP + (wordIndices.hashCode() % 1000
                    String keyNP = lNPHashNPName.get(hashNP) + wordTokens.toString();
                    // If this NP type plus these specific tokens have been seen before, we update the count (its value in the matrixContainer)
                    if (matrixContainer.cNPstringColumn.containsKey(keyNP)) {
                        np_col = matrixContainer.cNPstringColumn.get(keyNP);
                        int indexNPCol = matrixContainer.cNPColVectorIndex.get(np_col);
                        /// Update values in cData
                        for (int i = 0; i < n; i++)
                            matrixContainer.cData.set(indexNPCol + i, matrixContainer.cData.get(indexNPCol + i) + 1);

                    } else { ///> Else, we create a new matrixContainer column
                        matrixContainer.cNPstringColumn.put(keyNP, ++column_j);///> Dict with the word+np hash : column_index.
                        np_col = column_j;

                        matrixContainer.cSubClausesColumns.get(lpreClause.get(lNPHashNPName.get(hashNP))).add(np_col);///> Dict { NP_19:[1,3,5,7], NP_20:[2,6,8,10], ...}
                        lSubClausesColumns.get(lNPHashNPName.get(hashNP)).add(np_col); ///> Same as before, but local
                        matrixContainer.cNPColVectorIndex.put(np_col, matrixContainer.cCols.size()); ///> Dict for fast vector index searching given a column value. answers: Where in the vector is this value??
                        for (int j = 0; j < n; j++) {
                            matrixContainer.cRows.add(wordIndices.get(j));
                            matrixContainer.cCols.add(np_col);
                            matrixContainer.cData.add(1);
                        }
                    }

                }
                //Here we add the dependencies
                addDependenciesColumns(lListAllTokensPOS, lDictDependencyHeadIndex);

                //Here we add the sentence columns
                addSentenceColumns(lListTokensPOSSeen, 0);//1865652

                //Here we add the ngrams columns
                //                addNgramsColumns(lListTokensPOSSeen, 3);

                if (matrixContainer.cRows.size() != matrixContainer.cCols.size())
                    throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something is wrong...");

                /** Here we save the information about the columns, and the constituents each phrase is made of
                 * **/
                for (String cl : lClausesSeen) {
                    for (Map.Entry<String, String> entry : lpreClause.entrySet()) {
                        String clause = entry.getKey();
                        String clauseComponents = entry.getValue();
                        matrixContainer.cClauseSubClauseColumns.get(cl).get(clauseComponents).addAll(lSubClausesColumns.get(clause));
                    }

                }

            }//for each sentence
            /// Save the inverted indices

            matrixContainer.cColumnSubClause = invertMapOfSets(matrixContainer.cSubClausesColumns);
            matrixContainer.cTokenPOS = invertMapOfLists(matrixContainer.cPOSToken);
            /// Save the market format matrixContainer also
//                matrixContainer.cMarketFormatMatrix = saveMatrixMarketFormat(pathFolder + "/../matrixContainer/MMMatrix", matrixContainer, false);
            ///> Delete the row, cols and data arrays to save space. UPDATE: This is not a good idea. I can generate
            /// the matrixContainer faster with just the arrays.
//                matrixContainer.cRows.clear();
//                matrixContainer.cCols.clear();
//                matrixContainer.cData.clear();
//                lTargetWordInstancesMatrices.get(targetWord).put(instanceID, new JSONGraphContainer(matrixContainer));

        }//for each instance
        lTargetWordInstancesMatrix.put(targetWord, matrixContainer);

        Gson gson = new Gson();
        System.out.print("Saving " + targetWord + " JSON file...");
        String sTargetWordInstancesMatrices = gson.toJson(lTargetWordInstancesMatrix);
        saveTextFile(parentDirectory + "/" + targetWord + "_metamatrix", sTargetWordInstancesMatrices, ".json");
        System.out.println("Done");
        matrixContainer = null;

    }


    private void prepareCorpusMatrixNPs(String pathFile, boolean removeLongSentences, boolean onlyStatistics) throws IOException, InvalidLengthsException {
        String file = readFile(pathFile, true);

        ///DECLARATIONS
        ArrayList<String> pages = new ArrayList(Arrays.asList(file.split("%%#PAGE ")));
        Map<String, String> lpreClause;
        ArrayList<LinkedHashMap<Integer, String>> lListNPposition;
        ArrayList<LinkedHashMap<Integer, String>> lListHashNP;
        ArrayList<Integer> lListConstituencies;
        ArrayList<String> lListTokensPOSSeen;
        ArrayList<String> lListAllTokensPOS;
        Map<String, ArrayList<ArrayList<String>>> lDictDependencyHeadIndex;

        if (pages.get(0).equals(""))
            pages.remove(0);

        for (String p : pages) {

            ArrayList<String> sentences = new ArrayList(Arrays.asList(p.split("%%#SEN\t")));
            String pageTitle = sentences.get(0).trim();
            System.out.println("page: " + pageTitle);
            sentences.remove(0);

            for (String s : sentences) {
                numSent++;

                ArrayList<String> lines = new ArrayList(Arrays.asList(s.split("\n"))); ///> List of lines that compose each sentence
                String sentenceID = lines.get(0).trim(); ///> We get the first line which is the sentence ID
                int sentenceSize = Integer.parseInt(sentenceID.split("\t")[1]);
//                System.out.println("\t\tsentence: " + sentenceID);
                lines.remove(0); ///> We remove the sentence ID from the list of lines

                int maxSentenceSize = 40;
                if (removeLongSentences & sentenceSize > maxSentenceSize)
                    continue;

//                Sentence short enough, we continue
                Set<String> lClausesSeen = new HashSet<>(); ///> List that contains which clauses (NP) are found during the iteration
                lpreClause = new HashMap<>(); ///> Map that contains the subclauses and what prenodes compose them. NP_18:"DT_NN"
                lListNPposition = new ArrayList<>(); ///> List that contains dictionaries containing the position of the NPs in each word constituency string [{1:NP_18}]
                lListHashNP = new ArrayList<>();
                lListConstituencies = new ArrayList<>();
                lListTokensPOSSeen = new ArrayList<>();
                lListAllTokensPOS = new ArrayList<>();
                lDictDependencyHeadIndex = new DefaultDict<>();
                Map<Integer, String> lNPHashNPName = new HashMap<>();
                averageLengthSentence.add(lines.size());
                for (String l : lines) {///>Each line is a word
                    String[] splittedLine = l.split("\t");
                    if (splittedLine.length < 6) {
                        System.out.format("The document %s does not have a complete parse. This should not happen. Check it!", pathFile);
                        continue;
                    }
                    String token = splittedLine[0];
                    String lemma = splittedLine[1];
                    String posTag = splittedLine[2];
//                    System.out.println("\t " + l);

                    String constituency = splittedLine[3];
                    String dependencyHead = splittedLine[4];
                    String dependency = splittedLine[5];
                    String token_pos = lemma + "_" + posTag;

//                    cListAllTokens.add(token);
                    if (cDictAllTokens.containsKey(token))
                        cDictAllTokens.put(token, cDictAllTokens.get(token) + 1);
                    else
                        cDictAllTokens.put(token, 1);


                    if (onlyStatistics)
                        continue;
                    lListAllTokensPOS.add(token_pos);
                    // HERE WE START. If the word is not a punctuation mark (PUNCT) or it is not part of a NP
                    if (dependency.equals("PUNCT"))
                        continue;
//                    if (!constituency.contains("NP"))
//                        continue;
//                    if (!constituency.contains("VP"))
//                        continue;

                    //We save the current word dependency and its head, if it is of interest: nsubj, dobj, or pobj
//                    if (dependency.equals("nsubj") || dependency.equals("dobj") || dependency.equals("pobj") || dependency.equals("iobj")) {
//                        lDictDependencyHeadIndex.get(token_pos).add(new HashMap<String, Integer>(){{put(dependency, Integer.parseInt(dependencyHead));}});
                    lDictDependencyHeadIndex.get(token_pos).add(new ArrayList<>(Arrays.asList(dependency, dependencyHead)));
//                    }

                    /***
                     * 1. Get the token and store it in a dictionary string:int, with its row index as value:
                     *      {"the_DT":0, "car_NN":1, ...}
                     *  1.a Add to the row list the current row i. rows[0,0,1,1...] for the i vector of the ijv sparse matrixContainer
                     */

                    if (!matrixContainer.cTokenRow.containsKey(token_pos)) {
                        ///>This is the dict with the pos_tag : row_index
                        matrixContainer.cTokenRow.put(token_pos, row_i);
                        /// Save inverted index
                        matrixContainer.cRowToken.put(row_i, lemma); ///> WTF PAVEL! why dint i put lemma also in cTokenRow???
                        matrixContainer.cPOSToken.get(posTag).add(row_i);
                        row_i++;

                    }


                    lListTokensPOSSeen.add(token_pos);
                    /**
                     * 2. Using the constituency results, we find all the NPs clauses contained in the phrase.
                     *      We discard, at the beginning, any other type of clause (VP, ADJP, PP, PRP, etc).
                     *
                     */

                    ArrayList<String> clauses = new ArrayList(Arrays.asList(constituency.split(",")));

                    LinkedHashMap<Integer, String> lNPposition = getSelectedPhrases(clauses);
                    LinkedHashMap<Integer, String> lNPsHashes = getSelectedPhrasesHash(clauses);
                    /// Here we create a dict that maps NP hash to NP name: {-917:NP_18, 587:NP_20, ...}
                    for (Map.Entry<Integer, String> e : lNPsHashes.entrySet())
                        if (!lNPHashNPName.containsKey(e.getKey()))
                            lNPHashNPName.put(e.getKey(), e.getValue());

                    lListNPposition.add(lNPposition);
                    lListHashNP.add(lNPsHashes);
                    /**
                     * We get the preclauses of each of the NPs this token belongs to
                     */
                    for (Map.Entry<Integer, String> entry : lNPposition.entrySet()) {
                        Integer index_j = entry.getKey();
                        String targetClause = entry.getValue();

                        String clauseInitials = targetClause.split("_")[0];
                        lClausesSeen.add(clauseInitials);
                        /// We add hashed code of ancestors to distinguish NPs below (a sentence may have multiple NPs)
                        lListConstituencies.add(getKinship(constituency, index_j, true).hashCode() % 1000);
                        /**
                         * 2.1 We get the tags of what constitutes the targetClause. Such that:
                         * a dictionary str:str with NP_XX as key and a key describing its components as values:
                         * {"NP_18":"DET_NN_JJ', 'NP_70':'NP_NN',...}
                         *
                         *
                         */
                        //TODO: I could store this preClause structure for later use, as a MatrixContainer property
                        String tempPreClause;
                        /// There is actually a preClause  (e.g., a PRP or a NP). This check has to do with the length of the
                        /// constituencies string. If there are more elements, that indicates that there is another parent.
                        /// if not, there is only the pos tag left.
                        if (clauses.size() > index_j + 1)
                            tempPreClause = clauses.get(index_j + 1).split("_")[0];
                        else
                            tempPreClause = posTag; ///> There is no preClause. We take the POS tag.

                        if (lpreClause.containsKey(targetClause)) {
                            if (!lpreClause.get(targetClause).contains(tempPreClause))
                                lpreClause.put(targetClause, lpreClause.get(targetClause) + ":" + tempPreClause);
                        } else
                            lpreClause.put(targetClause, tempPreClause);


                    }
                }// end of current line (each line is a token!).
                /**
                 * 3. Once the complete pass over the sentence is done, we get what represents each column.
                 * 3.1 We get the columns for each different type of clause. In a dict str:int. Keys are
                 * the clauses, columns are the values: {"NP_18": [1,3,5,7], "VP_70":[2,4], ...}
                 */
                if (lClausesSeen.isEmpty())
                    continue;
                ///Converts lListHashNP (a list of dicts containing all the NPs hashchode (569) the words occur in as key
                /// and  the NP name (NP_18) as value) into a dict with the NP hashcode as key and the token indices of thos tokens
                /// that are aprt of said NP.
                Map<Integer, ArrayList<Integer>> lHashTokenIds = hashNP2hashToken(lListHashNP);
                DefaultDict<String, HashSet<Integer>> lSubClausesColumns = new DefaultDict<>(HashSet.class);

                /**
                 * Here we iterate for each different NP (different even if they are the same (NP_18 = NP_18), they are still made
                 * up from different words)
                 * We get the hash of the NP, we get the local indices of the words that build it. Then we get the real matrixContainer indices
                 * for these tokens. Then we check if we already stocked this NP with these same words. If yes, we update the values
                 * of cData. If not, we add it to cData and cRow and cCols
                 */
                int np_col;
                for (Map.Entry<Integer, ArrayList<Integer>> entry : lHashTokenIds.entrySet()) { ///> This is per each type of NP i.e., for each NP hashcode
                    int hashNP = entry.getKey();
                    ArrayList<Integer> words = entry.getValue();///> List of local indices (not the matrixContainer indices) that appear in this NP
                    int n = words.size();
                    ArrayList<Integer> wordIndices = new ArrayList<>();
                    ArrayList<String> wordTokens = new ArrayList<>();
                    for (int w = 0; w < n; w++) {
                        wordIndices.add(matrixContainer.cTokenRow.get(lListTokensPOSSeen.get(words.get(w))));
                        wordTokens.add(lListTokensPOSSeen.get(words.get(w)));
                    }
                    /// Here we create a hash id that will identify these particular words as well as the type of NP
                    /// Specifically, we will link words to NP or VPs
//                    String keyNP = Integer.toString((hashNP + wordIndices.hashCode()) % 10000);
                    String keyNP = lNPHashNPName.get(hashNP) + wordTokens.toString();
                    System.out.println(keyNP);
                    // If this NP type plus these specific tokens have been seen before, we update the count (its value in the matrixContainer)
                    if (matrixContainer.cNPstringColumn.containsKey(keyNP)) {
                        np_col = matrixContainer.cNPstringColumn.get(keyNP);
                        int indexNPCol = matrixContainer.cNPColVectorIndex.get(np_col);
                        /// Update values in cData
                        for (int i = 0; i < n; i++) {
                            int testi = matrixContainer.cData.get(indexNPCol + i);
                            matrixContainer.cData.set(indexNPCol + i, testi + 1);
                        }
                    } else { ///> Else, we create a new matrixContainer column
                        matrixContainer.cNPstringColumn.put(keyNP, ++column_j);///> Dict with the word+np hash : column_index.
                        np_col = column_j;

                        matrixContainer.cSubClausesColumns.get(lpreClause.get(lNPHashNPName.get(hashNP))).add(np_col);///> Dict { NP_19:[1,3,5,7], NP_20:[2,6,8,10], ...}
                        lSubClausesColumns.get(lNPHashNPName.get(hashNP)).add(np_col); ///> Same as before, but local
                        matrixContainer.cNPColVectorIndex.put(np_col, matrixContainer.cCols.size()); ///> Dict for fast vector index searching given a column value. answers: Where in the vector is this value??
                        for (int j = 0; j < n; j++) {
                            matrixContainer.cRows.add(wordIndices.get(j));
                            matrixContainer.cCols.add(np_col);
                            matrixContainer.cData.add(1);
                        }
                    }

                }
                //Here we add the dependencies
                addDependenciesColumns(lListAllTokensPOS, lDictDependencyHeadIndex);

                //Here we add the sentence columns
                addSentenceColumns(lListTokensPOSSeen, 0);
//                addSentenceColumns(lListTokensPOSSeen, (int) Math.pow(2,8));//with sentences: 2349022 // without sentences: 1865652 // difference: 483k
                //Here we add the ngrams columns
//                addNgramsColumns(lListTokensPOSSeen, 3);

//                if (matrixContainer.cRows.size() != matrixContainer.cCols.size())
//                    throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something is wrong...");

                /** Here we save the information about the columns, and the constituents each phrase is made of
                 * **/
                for (String cl : lClausesSeen) {
                    for (Map.Entry<String, String> entry : lpreClause.entrySet()) {
                        String clause = entry.getKey();
                        String clauseComponents = entry.getValue();
                        matrixContainer.cClauseSubClauseColumns.get(cl).get(clauseComponents).addAll(lSubClausesColumns.get(clause));
                    }

                }

            }//for each sentence
        }//for each page
        System.out.println();


    }

    /**
     * These functions runs the stanford2matrix conversion in a single thread
     */

    /**
     * Creates  sparse matrices from the parsed semeval 2007 XML file.
     * Each sense from each word is made into a matrixContainer, analog to the matrixContainer created from the
     * background corpus (oanc, wiki, sentences, ...)
     * <p>
     * Results are saved into ../matrixContainer/file.json, as a JSON file which represents a dictionary like this:
     * {  targetword1: {sense1: MatrixContainer, sense2: MatrixContainer, sense3...}, targetword2: {sense1: MatrixContainer, ...}, ...}
     * <p>
     * This is made to be readable in Python.
     *
     * @throws IOException
     * @throws InvalidLengthsException
     */
    public void runParseSemeval2007() throws IOException, InvalidLengthsException {

        long start = System.nanoTime();
        ArrayList<String> listPaths = listFiles(pathFolder, "parsed.txt");

        /* big loop with for each file of the wiki stanford-parsed files*/
        int idx = 1;

        ///> This loops goes file by file
        /// There is only one file here, the semeval 2007 XML
        for (String path : listPaths) {
            System.out.println(Integer.toString(idx) + ": " + path);
            this.prepareSemeval2007MatrixNPs(path);
            idx++;
            System.out.flush();
        }

        long time = System.nanoTime() - start;
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));
    }

    public void runParseSemeval2010() throws IOException, InvalidLengthsException {

        long start = System.nanoTime();
        ArrayList<String> listPaths = listFiles(pathFolder, "parsed");

        /* big loop with for each file of the wiki stanford-parsed files*/
        int idx = 1;

        /// There is only one file here, the semeval 2010 XML
        for (String path : listPaths) {
//            if (!path.contains("moment"))
//                continue;
            System.out.println(Integer.toString(idx) + ": " + path);
            this.prepareSemeval2010TrainMatrixNPs(path);
            idx++;
            System.out.flush();
        }

        long time = System.nanoTime() - start;
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));
    }


    public void runParseCorpus(boolean saveMatrix) throws IOException, InvalidLengthsException {
        long start = System.nanoTime();
        ArrayList<String> listPaths = listFiles(pathFolder, "parsed");

        /* big loop with for each file of the wiki stanford-parsed files*/
        int idx = 1;
        ///Debug
//        listPaths.clear();
//        listPaths.add("/media/stuff/temp/extracted/BV/");

        ///> This loops goes file by file
//        listPaths = new ArrayList<>(listPaths.subList(0, 100)); ///>DEBUG sublist
        for (String path : listPaths) {
            System.out.println(Integer.toString(idx) + ": " + path);
            ///> This is the function that actually parses the wiki Stanford parse into a graph

            this.prepareCorpusMatrixNPs(path, true, false);
            idx++;
            System.out.flush();
        }
//        HashSet<String> typesSet = new HashSet<>(cListAllTokens);


        System.out.println("Parse statistics:\n");
        System.out.println("\tNumber of parsed sentences: " + Integer.toString(numSent));
        System.out.println("\tNumber of types: " + Integer.toString(cDictAllTokens.size()));
        System.out.println("\tNumber of tokens: " + Integer.toString(sumValues(cDictAllTokens)));
        System.out.println("\tAverage number of tokens per sentence: " + Double.toString(average(averageLengthSentence)));

        /// Matrix creation done. We save it and display some stats
        if (matrixContainer.cRows.size() != matrixContainer.cCols.size())
            throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something was wrong...");
        ///>Print some matrixContainer statistics
        String stats = String.format("Number of rows: %d\n" +
                        "Number of columns: %d\n" +
                        "Number of unique types of clauses: %s\n" +
                        "Number of non-zero values: %d\n" +
                        "Sparsity: %f %%\n",
                matrixContainer.getNumberRows(), matrixContainer.getNumberColumns(), Arrays.toString(matrixContainer.cClauseSubClauseColumns.keySet().toArray()),
                matrixContainer.getNumberNonZeroElements(),
                matrixContainer.sparsity());


        /// Invert the row and column metadata maps to have direct access to rows and columns
        matrixContainer.cColumnSubClause = invertMapOfSets(matrixContainer.cSubClausesColumns);
        matrixContainer.cTokenPOS = invertMapOfLists(matrixContainer.cPOSToken);

        if (saveMatrix) {
            /// Create and save matrixContainer to MatrixMarket Format
            saveMatrixMarketFormat(pathFolder + "/../matrixContainer/MMMatrix", matrixContainer, true);

            /// Save matrixContainer metadata to JSON format
            saveMetaData(pathFolder + "/../metadata/", matrixContainer);
        }
        System.out.println();
        System.out.println(stats);
        long time = System.nanoTime() - start;
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));

    }
}
