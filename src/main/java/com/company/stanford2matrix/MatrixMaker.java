package com.company.stanford2matrix;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.*;

import static com.company.stanford2matrix.Utils.*;

public class MatrixMaker {
    private static String pathFolder;
    private static int column_j = 0;
    private static int row_i = 0; ///> The i index for the matrix
    public int numSent = 0;
    ArrayList<String> listAllTokens = new ArrayList<>();
    ArrayList<Integer> averageLengthSentence = new ArrayList<>();
    //All this should not be part of the class... it should just have a MatrixContainer object
    private MatrixContainer matrix;
    //Constructor
    MatrixMaker() {
        row_i = 1; ///> The i index for the matrix
        column_j = 0; ///> The j index for the matrix
        matrix = new MatrixContainer();
    }

    public static void main(String[] args) throws InterruptedException, IOException, InvalidLengthsException {
        if (args.length == 0) {
            System.out.println("Proper Usage is: java MatrixMaker what_to_parse.\n What_to_parse may be corpus OR semeval_XXXX");
            System.exit(0);
        }
        String dataPath;
        String mode = args[0];
        MatrixMaker myMaker = new MatrixMaker();

        if (args.length > 1)
            dataPath = args[1];

        else if (mode.equals("corpus")) {
//            dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/oanc/corpus";
            dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/wikidata/AA";
            //        String dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/sentencedata";
            //        String dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/oanc/corpus";
            myMaker.setDataPath(dataPath);
            myMaker.runParseCorpus(false);
        } else if (mode.equals("semeval_2007")) {
            dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/semeval2007/task 02/key/data/";
            myMaker.setDataPath(dataPath);
            myMaker.runParseSemeval2007();
        }



    }

    public void setDataPath(String dataPath) {
        pathFolder = dataPath;
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
            matrix.cCols.add(column_j);
        }

        return columnsIndices;
    }

//    private Map<Integer, String> NPIndex2ColumnIndex()

    private final LinkedHashMap<Integer, String> getNPs(ArrayList<String> clauses) {
        LinkedHashMap<Integer, String> npPosition = new LinkedHashMap<>();
        Collections.reverse(clauses);
        for (int i = 0; i < clauses.size(); i++)
            if (clauses.get(i).contains("NP")) {
                npPosition.put(i, clauses.get(i));
            }
        return npPosition;
    }

    private final LinkedHashMap<Integer, String> getNPhash(ArrayList<String> clauses) {
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
        Set<String> hashTokens = new HashSet<>(listTokens);
        listTokens = new ArrayList<>(hashTokens);
        int hashcode;
        if (hashLength > 0)
            hashcode = listTokens.hashCode() % hashLength;
        else {
//            hashcode = listTokens.hashCode();
            hashcode = this.matrix.sentenceID;
            this.matrix.sentenceID++;
        }
        int lSentenceDataIndex;
        int ngram_col;
        if (matrix.cSentenceColumn.containsKey(hashcode)) {
            ngram_col = matrix.cSentenceColumn.get(hashcode);
            lSentenceDataIndex = matrix.cSentenceDataVectorIndex.get(ngram_col);
            for (int i = 0; i < listTokens.size(); i++)
                matrix.cData.set(lSentenceDataIndex + i, this.matrix.cData.get(lSentenceDataIndex + i) + 1);
        } else {
            matrix.cSentenceColumn.put(hashcode, ++column_j);
            matrix.cColumnSentence.put(column_j, Integer.toString(hashcode)); ///> We save the inverted index
            matrix.cSentenceDataVectorIndex.put(column_j, matrix.cCols.size());
            for (int i = 0; i < listTokens.size(); i++) {
                matrix.cRows.add(this.matrix.cTokenRow.get(listTokens.get(i)));
                matrix.cCols.add(column_j);
                matrix.cData.add(1);
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
                dependencyName = dependency + "_of_" + listTokens.get(headIndex - 1); ///>Have to remove one cause listokens is 0 index based

                if (this.matrix.cDependencyColumn.containsKey(dependencyName)) {
                    if (this.matrix.cWordDependencyDataVectorIndex.containsKey(word + dependencyName)) {
                        lDependencyDataIndex = this.matrix.cWordDependencyDataVectorIndex.get(word + dependencyName);
                        /// Get the index on the cData vector of the value that must be modified
                        this.matrix.cData.set(lDependencyDataIndex, this.matrix.cData.get(lDependencyDataIndex) + 1);
                    } else {
                        dependencyNameCol = this.matrix.cDependencyColumn.get(dependencyName);
                        this.matrix.cWordDependencyDataVectorIndex.put(word + dependencyName, matrix.cCols.size());
                        this.matrix.cRows.add(this.matrix.cTokenRow.get(word));
                        this.matrix.cCols.add(dependencyNameCol);
                        this.matrix.cData.add(1);
                    }
                } else {
                    this.matrix.cDependencyColumn.put(dependencyName, ++column_j);
                    /// Save the reverse index
                    this.matrix.cColumnDependency.put(column_j, dependencyName);
                    this.matrix.cWordDependencyDataVectorIndex.put(word + dependencyName, matrix.cCols.size());
                    this.matrix.cRows.add(this.matrix.cTokenRow.get(word));
                    this.matrix.cCols.add(column_j);
                    this.matrix.cData.add(1);


                }
            }
        }
    }

    private final Map<String, ArrayList<Integer>> addNgramsColumns(ArrayList<String> listTokens, int n) {
        /**
         * From a list of tokens, calculates its n ngrams and adds them to the matrix.
         * It computes the ngrams and then updates the ijv vectors containing the matrix, according to
         * the existence or not of the ngram in the matrix.
         */
        Map<String, ArrayList<Integer>> ngramIndices = new DefaultDict<>();
        String ngram;
        Integer ngram_col;
        for (int i = 0; i < listTokens.size() - (n - 1); i++) {
            ngram = String.join("__", listTokens.subList(i, i + n));

            if (this.matrix.cNgramColumn.containsKey(ngram)) {
                ngram_col = this.matrix.cNgramColumn.get(ngram);
                int indexNgramCol = this.matrix.cNgramColDataVectorIndex.get(ngram_col);
                for (int ii = 0; ii < n; ii++)
                    this.matrix.cData.set(indexNgramCol + ii, this.matrix.cData.get(indexNgramCol + ii) + 1);
            } else {
                this.matrix.cNgramColumn.put(ngram, ++column_j);
                ngram_col = column_j;

                /// This here is to keep a dict ngram_col_index : column_position_index to easily and rapidly find the
                /// corresponding position index for a given ngram column index. IOW, a dict that maps the ngram columns to
                /// its corresponding index in the cColumns list.
                matrix.cNgramColDataVectorIndex.put(ngram_col, matrix.cCols.size());

                /// We add to the matrix ijv vectors the new values. We iterate from 0 to n to add the values to the
                /// corresponding lines (words). So, a given trigram will have 1s in each of the three words that formed it.
                for (int j = i; j < i + n; j++) {
                    this.matrix.cRows.add(this.matrix.cTokenRow.get(listTokens.get(j)));
                    this.matrix.cCols.add(ngram_col);
                    this.matrix.cData.add(1);
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


    //TODO: Make a method that receives a string (instead of a file path) and creates a matrix from the text string.
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
            String targetWord = instances.get(0).trim();
//            String twPOStag = targetWordInfo[1];
            System.out.println("Target word: " + ii + targetWord);
            instances.remove(0);
            lTargetWordInstancesMatrices.put(targetWord, new HashMap<>());

            for (String in : instances) {
                this.matrix = new MatrixContainer();

                row_i = 1; ///> The i index for the matrix
                column_j = 0; ///> The j index for the matrix

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
                         *  1.a Add to the row list the current row i. rows[0,0,1,1...] for the i vector of the ijv sparse matrix
                         */

                        if (!matrix.cTokenRow.containsKey(token_pos)) {
                            ///>This is the dict with the pos_tag : row_index
                            matrix.cTokenRow.put(token_pos, row_i);
                            /// Save inverted index
                            matrix.cRowToken.put(row_i, lemma);
                            matrix.cPOSToken.get(posTag).add(row_i);
                            row_i++;

                        }


                        lListTokensPOSSeen.add(token_pos);
                        /**
                         * 2. Using the constituency results, we find all the NPs clauses contained in the phrase.
                         *      We discard, at the beginning, any other type of clause (VP, ADJP, PP, PRP, etc).
                         *
                         */

                        ArrayList<String> clauses = new ArrayList(Arrays.asList(constituency.split(",")));

                        LinkedHashMap<Integer, String> lNPposition = getNPs(clauses);
                        LinkedHashMap<Integer, String> lNPsHashes = getNPhash(clauses);
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
                     * We get the hash of the NP, we get the local indices of the words that build it. Then we get the real matrix indices
                     * for these tokens. Then we check if we already stocked this NP with these same words. If yes, we update the values
                     * of cData. If not, we add it to cData and cRow and cCols
                     */
                    int np_col;
                    for (Map.Entry<Integer, ArrayList<Integer>> entry : lHashTokenIds.entrySet()) { ///> This is per each type of NP i.e., for each NP hashcode
                        int hashNP = entry.getKey();
                        ArrayList<Integer> words = entry.getValue();///> List of local indices (not the matrix indices) that appear in this NP
                        int n = words.size();
                        ArrayList<Integer> wordIndices = new ArrayList<>();
                        ArrayList<String> wordTokens = new ArrayList<>();
                        for (int w = 0; w < n; w++) {
                            wordIndices.add(matrix.cTokenRow.get(lListTokensPOSSeen.get(words.get(w))));
                            wordTokens.add(lListTokensPOSSeen.get(words.get(w)));
                        }
                        /// Here we create a hash id that will identify these particular words as well as the type of NP
                        //                    int keyNP = hashNP + (wordIndices.hashCode() % 1000
                        String keyNP = lNPHashNPName.get(hashNP) + wordTokens.toString();
                        // If this NP type plus these specific tokens have been seen before, we update the count (its value in the matrix)
                        if (matrix.cNPwordsColumn.containsKey(keyNP)) {
                            np_col = matrix.cNPwordsColumn.get(keyNP);
                            int indexNPCol = matrix.cNPColVectorIndex.get(np_col);
                            /// Update values in cData
                            for (int i = 0; i < n; i++)
                                this.matrix.cData.set(indexNPCol + i, this.matrix.cData.get(indexNPCol + i) + 1);

                        } else { ///> Else, we create a new matrix column
                            matrix.cNPwordsColumn.put(keyNP, ++column_j);///> Dict with the word+np hash : column_index.
                            np_col = column_j;

                            matrix.cSubClausesColumns.get(lpreClause.get(lNPHashNPName.get(hashNP))).add(np_col);///> Dict { NP_19:[1,3,5,7], NP_20:[2,6,8,10], ...}
                            lSubClausesColumns.get(lNPHashNPName.get(hashNP)).add(np_col); ///> Same as before, but local
                            matrix.cNPColVectorIndex.put(np_col, matrix.cCols.size()); ///> Dict for fast vector index searching given a column value. answers: Where in the vector is this value??
                            for (int j = 0; j < n; j++) {
                                this.matrix.cRows.add(wordIndices.get(j));
                                this.matrix.cCols.add(np_col);
                                this.matrix.cData.add(1);
                            }
                        }

                    }
                    //Here we add the dependencies
                    addDependenciesColumns(lListAllTokensPOS, lDictDependencyHeadIndex);

                    //Here we add the sentence columns
                    addSentenceColumns(lListTokensPOSSeen, 100000);//1865652

                    //Here we add the ngrams columns
                    //                addNgramsColumns(lListTokensPOSSeen, 3);

                    if (this.matrix.cRows.size() != this.matrix.cCols.size())
                        throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something is wrong...");

                    /** Here we save the information about the columns, and the constituents each phrase is made of
                     * **/
                    for (String cl : lClausesSeen) {
                        for (Map.Entry<String, String> entry : lpreClause.entrySet()) {
                            String clause = entry.getKey();
                            String clauseComponents = entry.getValue();
                            matrix.cClauseSubClauseColumns.get(cl).get(clauseComponents).addAll(lSubClausesColumns.get(clause));
                        }

                    }

                }//for each sentence
                /// Save the inverted indices
                this.matrix.cColumnSubClause = invertMapOfSets(this.matrix.cSubClausesColumns);
                this.matrix.cTokenPOS = invertMapOfLists(this.matrix.cPOSToken);
                /// Save the market format matrix also
//                this.matrix.cMarketFormatMatrix = saveMatrixMarketFormat(pathFolder + "/../matrix/MMMatrix", this.matrix, false);
                ///> Delete the row, cols and data arrays to save space. UPDATE: This is not a good idea. I can generate
                /// the matrix faster with just the arrays.
//                this.matrix.cRows.clear();
//                this.matrix.cCols.clear();
//                this.matrix.cData.clear();
//                lTargetWordInstancesMatrices.get(targetWord).put(instanceID, new JSONGraphContainer(matrix));
                lTargetWordInstancesMatrices.get(targetWord).put(instanceID, this.matrix);
            }//for each instance
            Gson gson = new Gson();
            System.out.print("Saving " + targetWord + " JSON file...");
            String sTargetWordInstancesMatrices = gson.toJson(lTargetWordInstancesMatrices);
            saveTextFile(this.pathFolder + "/../matrix/" + "semeval2007_" + targetWord, sTargetWordInstancesMatrices, ".json");
            System.out.println("Done");
        }// for each targetword
    }

    private void prepareCorpiMatrixNPs(String pathFile, boolean removeLongSentences, boolean onlyStatistics) throws IOException, InvalidLengthsException {
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
                    String token = splittedLine[0];
                    String lemma = splittedLine[1];
                    String posTag = splittedLine[2];
                    String constituency = splittedLine[3];
                    String dependencyHead = splittedLine[4];
                    String dependency = splittedLine[5];
                    String token_pos = lemma + "_" + posTag;
                    listAllTokens.add(token);

                    lListAllTokensPOS.add(token_pos);
                    if (onlyStatistics)
                        continue;
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
                     *  1.a Add to the row list the current row i. rows[0,0,1,1...] for the i vector of the ijv sparse matrix
                     */

                    if (!matrix.cTokenRow.containsKey(token_pos)) {
                        ///>This is the dict with the pos_tag : row_index
                        matrix.cTokenRow.put(token_pos, row_i);
                        /// Save inverted index
                        matrix.cRowToken.put(row_i, lemma);
                        matrix.cPOSToken.get(posTag).add(row_i);
                        row_i++;

                    }


                    lListTokensPOSSeen.add(token_pos);
                    /**
                     * 2. Using the constituency results, we find all the NPs clauses contained in the phrase.
                     *      We discard, at the beginning, any other type of clause (VP, ADJP, PP, PRP, etc).
                     *
                     */

                    ArrayList<String> clauses = new ArrayList(Arrays.asList(constituency.split(",")));

                    LinkedHashMap<Integer, String> lNPposition = getNPs(clauses);
                    LinkedHashMap<Integer, String> lNPsHashes = getNPhash(clauses);
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
                }// end of current line (each line is a token!). Sentence is completely read.
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
                 * We get the hash of the NP, we get the local indices of the words that build it. Then we get the real matrix indices
                 * for these tokens. Then we check if we already stocked this NP with these same words. If yes, we update the values
                 * of cData. If not, we add it to cData and cRow and cCols
                 */
                int np_col;
                for (Map.Entry<Integer, ArrayList<Integer>> entry : lHashTokenIds.entrySet()) { ///> This is per each type of NP i.e., for each NP hashcode
                    int hashNP = entry.getKey();
                    ArrayList<Integer> words = entry.getValue();///> List of local indices (not the matrix indices) that appear in this NP
                    int n = words.size();
                    ArrayList<Integer> wordIndices = new ArrayList<>();
                    ArrayList<String> wordTokens = new ArrayList<>();
                    for (int w = 0; w < n; w++) {
                        wordIndices.add(matrix.cTokenRow.get(lListTokensPOSSeen.get(words.get(w))));
                        wordTokens.add(lListTokensPOSSeen.get(words.get(w)));
                    }
                    /// Here we create a hash id that will identify these particular words as well as the type of NP
//                    int keyNP = hashNP + (wordIndices.hashCode() % 1000
                    String keyNP = lNPHashNPName.get(hashNP) + wordTokens.toString();
                    // If this NP type plus these specific tokens have been seen before, we update the count (its value in the matrix)
                    if (matrix.cNPwordsColumn.containsKey(keyNP)) {
                        np_col = matrix.cNPwordsColumn.get(keyNP);
                        int indexNPCol = matrix.cNPColVectorIndex.get(np_col);
                        /// Update values in cData
                        for (int i = 0; i < n; i++)
                            this.matrix.cData.set(indexNPCol + i, this.matrix.cData.get(indexNPCol + i) + 1);

                    } else { ///> Else, we create a new matrix column
                        matrix.cNPwordsColumn.put(keyNP, ++column_j);///> Dict with the word+np hash : column_index.
                        np_col = column_j;

                        matrix.cSubClausesColumns.get(lpreClause.get(lNPHashNPName.get(hashNP))).add(np_col);///> Dict { NP_19:[1,3,5,7], NP_20:[2,6,8,10], ...}
                        lSubClausesColumns.get(lNPHashNPName.get(hashNP)).add(np_col); ///> Same as before, but local
                        matrix.cNPColVectorIndex.put(np_col, matrix.cCols.size()); ///> Dict for fast vector index searching given a column value. answers: Where in the vector is this value??
                        for (int j = 0; j < n; j++) {
                            this.matrix.cRows.add(wordIndices.get(j));
                            this.matrix.cCols.add(np_col);
                            this.matrix.cData.add(1);
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

                if (this.matrix.cRows.size() != this.matrix.cCols.size())
                    throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something is wrong...");

                /** Here we save the information about the columns, and the constituents each phrase is made of
                 * **/
                for (String cl : lClausesSeen) {
                    for (Map.Entry<String, String> entry : lpreClause.entrySet()) {
                        String clause = entry.getKey();
                        String clauseComponents = entry.getValue();
                        matrix.cClauseSubClauseColumns.get(cl).get(clauseComponents).addAll(lSubClausesColumns.get(clause));
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
     * Each sense from each word is made into a matrix, analog to the matrix created from the
     * background corpus (oanc, wiki, sentences, ...)
     * <p>
     * Results are saved into ../matrix/file.json, as a JSON file which represents a dictionary like this:
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

    public void runParseCorpus(boolean saveMatrix) throws IOException, InvalidLengthsException {
        long start = System.nanoTime();
        ArrayList<String> listPaths = listFiles(pathFolder, "parsed");

        /* big loop with for each file of the wiki stanford-parsed files*/
        int idx = 1;

        ///> This loops goes file by file
//        listPaths = new ArrayList<>(listPaths.subList(0, 100)); ///>DEBUG sublist
        for (String path : listPaths) {
            System.out.println(Integer.toString(idx) + ": " + path);
            ///> This is the function that actually parses the wiki Stanford parse into a graph
            this.prepareCorpiMatrixNPs(path, false, true);
            idx++;
            System.out.flush();
        }
        HashSet<String> typesSet = new HashSet<>(listAllTokens);

        System.out.println("Parse statistics:\n");
        System.out.println("\tNumber of parsed sentences: " + Integer.toString(numSent));
        System.out.println("\tNumber of tokens: " + Integer.toString(listAllTokens.size()));
        System.out.println("\tNumber of types: " + Integer.toString(typesSet.size()));
        System.out.println("\tAverage number of tokens per sentence: " + Double.toString(average(averageLengthSentence)));
        /// Matrix creation done. We save it and display some stats

        if (this.matrix.cRows.size() != this.matrix.cCols.size())
            throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something was wrong...");
        ///>Print some matrix statistics
        String stats = String.format("Number of rows: %d\n" +
                        "Number of columns: %d\n" +
                        "Number of unique types of clauses: %d\n" +
                        "Number of non-zero values: %d\n" +
                        "Sparsity: %f %%\n",
                this.matrix.getNumberRows(), this.matrix.getNumberColumns(), this.matrix.cClauseSubClauseColumns.keySet().size(), this.matrix.getNumberNonZeroElements(),
                this.matrix.sparsity());


        /// Invert the row and column metadata maps to have direct access to rows and columns
        this.matrix.cColumnSubClause = invertMapOfSets(this.matrix.cSubClausesColumns);
        this.matrix.cTokenPOS = invertMapOfLists(this.matrix.cPOSToken);

        if (saveMatrix) {
            /// Create and save matrix to MatrixMarket Format
            saveMatrixMarketFormat(pathFolder + "/../matrix/MMMatrix", this.matrix, true);

            /// Save matrix metadata to JSON format
            saveMetaData(pathFolder + "/../metadata/", this.matrix);
        }
        System.out.println();
        System.out.println(stats);
        long time = System.nanoTime() - start;
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));

    }
}