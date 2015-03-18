package com.company.stanford2matrix;

import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.SparseMatrix;

import java.io.IOException;
import java.util.*;

import static com.company.stanford2matrix.Utils.*;

public class MatrixMaker {
    private static int nThreads;
    private static String pathFolder;
    private static int column_j = 0;
    private static int row_i = 0; ///> The i index for the matrix
    //All this should not be part of the class... it should just have a MatrixContainer object
    private MatrixContainer matrix;


    //Constructor
    MatrixMaker(String pathFolder, int nThreads) {
        MatrixMaker.nThreads = nThreads;
        MatrixMaker.pathFolder = pathFolder;
        row_i = 0; ///> The i index for the matrix
        column_j = -1; ///> The j index for the matrix
        matrix = new MatrixContainer();
    }

    public static void main(String[] args) throws InterruptedException, IOException, InvalidLengthsException {
//        String dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/wikidata";
//        String dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/sentencedata";
        String dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/oanc";
        MatrixMaker myMaker = new MatrixMaker(dataPath, 15);
        myMaker.runSingle();


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


    private final Map<String, ArrayList<Integer>> computeNgrams(ArrayList<String> listTokens, int n) {
        /**
         * From a list of tokens, calculates its n ngrams and adds them to the matrix.
         * It computes the ngrams and then updates the ijv vectors containing the matrix, according to
         * the existence or not of the ngram in the matrix.
         */
        Map<String, ArrayList<Integer>> ngramIndices = new DefaultDict<>();
        ArrayList<List<String>> nose = new ArrayList<>();
        String ngram;
        Integer ngram_col;
        for (int i = 0; i < listTokens.size() - (n - 1); i++) {
            ngram = String.join("__", listTokens.subList(i, i + n));

            if (this.matrix.cNgramColumn.containsKey(ngram)) {
                ngram_col = this.matrix.cNgramColumn.get(ngram);
                int indexNgramCol = this.matrix.cNgramColVectorIndex.get(ngram_col);
                for (int ii = 0; ii < n; ii++)
                    this.matrix.cData.set(indexNgramCol + ii, this.matrix.cData.get(indexNgramCol + ii) + 1);
            } else {
                this.matrix.cNgramColumn.put(ngram, ++column_j);
                ngram_col = column_j;

                /// This here is to keep a dict ngram_col_index : column_position_index to easily and rapidly find the
                /// corresponding position index for a given ngram column index. IOW, a dict that maps the ngram columns to
                /// its corresponding index in the cColumns list.
                matrix.cNgramColVectorIndex.put(ngram_col, matrix.cCols.size());

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

    private void prepareMatrix(String pathFile) throws IOException, InvalidLengthsException {
        String file = readFile(pathFile, true);

        ///DECLARATIONS
        int level = 1 + 1;
        Map<String, Integer> lMapTokenRow = new HashMap<>();
        Map<String, String> lpreClause;
        ArrayList<String> pages = new ArrayList(Arrays.asList(file.split("%%#PAGE ")));
        if (pages.get(0).equals(""))
            pages.remove(0);

        for (String p : pages) {
            ArrayList<String> sentences = new ArrayList(Arrays.asList(p.split("%%#SEN ")));
            String pageTitle = sentences.get(0).trim();
            System.out.println("page: " + pageTitle);
            sentences.remove(0);

            for (String s : sentences) {
                ArrayList<String> lines = new ArrayList(Arrays.asList(s.split("\n")));
                String sentenceID = lines.get(0).trim();
//                System.out.println("\t\tsentence: " + sentenceID);
                lines.remove(0);
                ArrayList<String> lClausesSeen = new ArrayList<>();
                ArrayList<String> lSubClausesSeen = new ArrayList<>();
                lpreClause = new HashMap<>();
                for (String l : lines) {
                    String[] splittedLine = l.split("\t");
                    String token = splittedLine[0];
                    String lemma = splittedLine[1];
                    String posTag = splittedLine[2];
                    String constituency = splittedLine[3];
                    String dependencyHead = splittedLine[4];
                    String dependency = splittedLine[5];
                    String token_pos = lemma + "_" + posTag;
                    if (dependency.equals("PUNCT"))
                        continue;
                    /***
                     * 1. Get the token and store it in a dictionary string:int, with its row index as value:
                     *      {"the_DT":0, "car_NN":1, ...}
                     *  1.a Add to the row list the current row i. rows[0,0,1,1...] for the i vector of the ijv sparse matrix
                     */
                    if (matrix.cTokenRow.containsKey(token_pos))
                        matrix.cRows.add(matrix.cTokenRow.get(token_pos));
                    else {
                        matrix.cTokenRow.put(token_pos, row_i);
                        matrix.cRows.add(row_i);
                        row_i++;
                    }

                    /**
                     * 2. Using the constituency results, we determine the clause level we are interested in (Level 1, 2,..).
                     *      The level is taken from the constituency string, starting from left to right.
                     *      The desired clause tag is stored in targetClause
                     */
                    ArrayList<String> clauses = new ArrayList(Arrays.asList(constituency.split(",")));
                    if (level > clauses.size())
                        level = clauses.size();
                    String targetClause = clauses.get(clauses.size() - level);
                    String clauseInitials = targetClause.split("_")[0];
                    lClausesSeen.add(clauseInitials);
                    lSubClausesSeen.add(targetClause);

                    /**
                     * 2.1 We get the tags of what constitutes the targetClause. Such that:
                     * a dictionary str:str with clauseTarget as key and a key describing its components as values:
                     * {"NP_18":"DET_NN_JJ', 'VP_70':'VBZ_NP',...}
                     *
                     *
                     */
                    //TODO: I could store this preClause structure for later use.
                    String tempPreClause;
                    if (clauses.size() >= level + 1) ///> There is actually a preClause identificator (e.g., a PRP for a NP)
                        tempPreClause = clauses.get(clauses.size() - (level + 1));
                    else
                        tempPreClause = posTag; ///> There is no preClause. We take the POS tag.

                    if (lpreClause.containsKey(targetClause)) {
                        if (!lpreClause.get(targetClause).contains(tempPreClause))
                            lpreClause.put(targetClause, lpreClause.get(targetClause) + ":" + tempPreClause);
                    } else
                        lpreClause.put(targetClause, tempPreClause);


                }// end of current line. Sentence is completely read.
                /**
                 * 3. Once the complete pass over the sentence is done, we get what represent each column.
                 * 3.1 We get the columns for each different type of clause. In a dict str:int. Keys are
                 * the clauses, columns are the values: {"NP_18": [1,3,5,7], "VP_70":[2,4], ...}
                 */
                if (lClausesSeen.isEmpty())
                    continue;
                Map<String, ArrayList<Integer>> lSubClausesColumns = clauseToIndices(lSubClausesSeen);
                for (Map.Entry<String, ArrayList<Integer>> entry : lSubClausesColumns.entrySet()) {
                    String clause = entry.getKey();
                    ArrayList value_j = entry.getValue();
                    matrix.cSubClausesColumns.get(clause).addAll(value_j);
                }

                if (lSubClausesColumns.size() != lpreClause.size())
                    throw new Utils.InvalidLengthsException("These lengths should be equal!!");

                /**
                 * 3.2 We get a dict str:dict<str:list<int>> to map for desired clause columns.
                 * Such as this: {"NP":{"DET_NN_JJ":[1,3,5,7]}}
                 */
                Set<String> lSetClausesSeen = new HashSet<>(lClausesSeen);
                for (String clause : lSetClausesSeen) {
                    for (Map.Entry<String, String> entry : lpreClause.entrySet()) {
                        String subClause = entry.getKey();
                        String subClauseComponents = entry.getValue();
                        if (subClause.contains(clause + "_"))
                            matrix.cClauseSubClauseColumns.get(clause).get(subClauseComponents).addAll(lSubClausesColumns.get(subClause));
                    }
                }
                column_j++;

            }
        }
        System.out.println();


    }

    private Matrix makeMahoutSparseMatrix(ArrayList<Integer> rows, ArrayList<Integer> columns, ArrayList<Integer> data) {
        int numElements = rows.size();
        Matrix matrix = new SparseMatrix(this.matrix.getNumberRows(), this.matrix.getNumberColumns());
        System.out.print("\nCreating mahout SparseMatrix...");
        for (int i = 0; i < numElements; i++)
            matrix.setQuick(rows.get(i), columns.get(i), data.get(i));

//        for (int row = 0; row < matrix.rowSize(); row++) {
//            for (int col = 0; col < matrix.columnSize(); col++) {
//                matrix.setQuick(row, col, values[row][col]);
//            }
        System.out.println("Done.\n\n");
        System.out.println(matrix.getQuick(0, 0) * matrix.getQuick(2, 2));
        return matrix;
    }


    private void prepareMatrixNPs(String pathFile) throws IOException, InvalidLengthsException {
        String file = readFile(pathFile, true);

        ///DECLARATIONS
        ArrayList<String> pages = new ArrayList(Arrays.asList(file.split("%%#PAGE ")));
        Map<String, String> lpreClause;
        ArrayList<LinkedHashMap<Integer, String>> lListNPposition;
        ArrayList<LinkedHashMap<Integer, String>> lListHashNP;
        ArrayList<Integer> lListConstituencies;
        ArrayList<String> lListTokensPOSSeen;
        if (pages.get(0).equals(""))
            pages.remove(0);

        for (String p : pages) {
            ArrayList<String> sentences = new ArrayList(Arrays.asList(p.split("%%#SEN ")));
            String pageTitle = sentences.get(0).trim();
            System.out.println("page: " + pageTitle);
            sentences.remove(0);

            for (String s : sentences) {
                ArrayList<String> lines = new ArrayList(Arrays.asList(s.split("\n"))); ///> List of lines that compose each sentence
                String sentenceID = lines.get(0).trim(); ///> We get the first line which is the sentence ID
//                System.out.println("\t\tsentence: " + sentenceID);
                lines.remove(0); ///> We remove the sentence ID from the list of lines
                Set<String> lClausesSeen = new HashSet<>(); ///> List that contains which clauses (NP) are found during the iteration
                lpreClause = new HashMap<>(); ///> Map that contains the subclauses and what prenodes compose them. NP_18:"DT_NN"
                lListNPposition = new ArrayList<>(); ///> List that contains dictionaries containing the position of the NPs in each word constituency string [{1:NP_18}]
                lListHashNP = new ArrayList<>();
                lListConstituencies = new ArrayList<>();
                lListTokensPOSSeen = new ArrayList<>();
                Map<Integer, String> lHashNP = new HashMap<>();
                for (String l : lines) {///>Each line is a word
                    String[] splittedLine = l.split("\t");
                    String token = splittedLine[0];
                    String lemma = splittedLine[1];
                    String posTag = splittedLine[2];
                    String constituency = splittedLine[3];
                    String dependencyHead = splittedLine[4];
                    String dependency = splittedLine[5];
                    String token_pos = lemma + "_" + posTag;


                    // HERE WE START. If the word is not a punctuation mark (PUNCT) or it is not part of a NP
                    if (dependency.equals("PUNCT") || !constituency.contains("NP"))
                        continue;
                    /***
                     * 1. Get the token and store it in a dictionary string:int, with its row index as value:
                     *      {"the_DT":0, "car_NN":1, ...}
                     *  1.a Add to the row list the current row i. rows[0,0,1,1...] for the i vector of the ijv sparse matrix
                     */

                    if (!matrix.cTokenRow.containsKey(token_pos)) {
                        ///>This is the dict with the pos_tag : row_index
                        matrix.cTokenRow.put(token_pos, row_i);
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
                        if (!lHashNP.containsKey(e.getKey()))
                            lHashNP.put(e.getKey(), e.getValue());

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
                         * a dictionary str:str with clauseTarget as key and a key describing its components as values:
                         * {"NP_18":"DET_NN_JJ', 'VP_70':'VBZ_NP',...}
                         *
                         *
                         */
                        //TODO: I could store this preClause structure for later use.
                        String tempPreClause;
                        if (clauses.size() > index_j + 1) ///> There is actually a preClause identificator (e.g., a PRP or a NP)
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
                /// Convert the list of NP:Hash into a dict hah:token_id

                Map<Integer, ArrayList<Integer>> lHashTokenIds = hashNP2hashToken(lListHashNP);
                DefaultDict<String, HashSet<Integer>> lSubClausesColumns = new DefaultDict<>(HashSet.class);

                /**
                 * Here we iterate for each different NP (different even if they are the same NP_18, NP_18, they still have different words)
                 * We get the hash of the NP, we get the local indices of the words that build it. Then we get the real matrix indices
                 * for these tokens. Then we check if we already stocked this NP with this same words. If yes, we update the values
                 * of cData. If not, we add it to the seen
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
                    String keyNP = lHashNP.get(hashNP) + wordTokens.toString();
                    // If this NP type plus these specific tokens have been seen before, we update the cound (its value in the matrix)
                    if (matrix.cNPwordsColumn.containsKey(keyNP)) {
                        np_col = matrix.cNPwordsColumn.get(keyNP);
                        int indexNPCol = matrix.cNPColVectorIndex.get(np_col);
                        /// Update values in cData
                        for (int i = 0; i < n; i++)
                            this.matrix.cData.set(indexNPCol + i, this.matrix.cData.get(indexNPCol + i) + 1);

                    } else { ///> Else, we create a new matrix
                        matrix.cNPwordsColumn.put(keyNP, ++column_j);///> Dict with the word+np hash : column_index.
                        np_col = column_j;

                        matrix.cSubClausesColumns.get(lHashNP.get(hashNP)).add(np_col);///> Dict { NP_19:[1,3,5,7], NP_20:[2,6,8,10], ...}
                        lSubClausesColumns.get(lHashNP.get(hashNP)).add(np_col); ///> Same as before, but local
                        matrix.cNPColVectorIndex.put(np_col, matrix.cCols.size()); ///> Dict for fast vector index searching given a column value. answers: Where in the vector is this value??
                        for (int j = 0; j < n; j++) {
                            this.matrix.cRows.add(wordIndices.get(j));
                            this.matrix.cCols.add(np_col);
                            this.matrix.cData.add(1);
                        }
                    }

                }
                //Here we add the ngrams
//                computeNgrams(lListTokensPOSSeen, 3);

                if (this.matrix.cRows.size() != this.matrix.cCols.size())
                    throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something is wrong...");
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
     * This function runs the stanford2matrix conversion in a single thread
     */
    public void runSingle() throws IOException, InvalidLengthsException {
        long start = System.nanoTime();
        ArrayList<String> listPaths = listFiles(pathFolder, "parsed.txt");

        /* big loop with for each file of the wiki stanford-parsed files*/
        int idx = 1;

        ///> This loops goes file by file
        listPaths = new ArrayList<>(listPaths.subList(0, 100)); ///>DEBUG sublist
        for (String path : listPaths) {
            System.out.println(Integer.toString(idx) + ": " + path);
            this.prepareMatrixNPs(path);
            idx++;
            System.out.flush();
        }

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
        ///> Save matrix to MatrixMarket Format
//        saveMatrixMarketFormat(pathFolder + "/MMMatrix", this.matrix);
        saveMetaData(pathFolder + "/metadata/", this.matrix);

//        Matrix myMatrix = makeMahoutSparseMatrix(matrix.cRows, matrix.cCols, matrix.cData);

        System.out.println(stats);
        long time = System.nanoTime() - start;
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));

    }
}