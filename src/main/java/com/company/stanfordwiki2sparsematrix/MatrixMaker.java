package com.company.stanfordwiki2sparsematrix;

import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.SparseMatrix;

import java.io.IOException;
import java.util.*;

import static com.company.stanfordwiki2sparsematrix.Utils.*;

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
//        String dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/wikidata";
        String dataPath = "/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/sentencedata";
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
                int indexNgramCol = this.matrix.cNgramColIndex.get(ngram_col);
                for (int ii = 0; ii < n; ii++)
                    this.matrix.cData.set(indexNgramCol + ii, this.matrix.cData.get(indexNgramCol + ii) + 1);
            } else {
                this.matrix.cNgramColumn.put(ngram, ++column_j);
                ngram_col = column_j;

                /// This here is to keep a dict ngram_col_index : column_position_index to easily and rapidly find the
                /// corresponding position index for a given ngram column index. IOW, a dict that maps the ngram columns to
                /// its corresponding index in the cColumns list.
                matrix.cNgramColIndex.put(ngram_col, matrix.cCols.size());

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
                System.out.println("\t\tsentence: " + sentenceID);
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
        System.out.println("Done.");
        System.out.println(matrix.getQuick(0, 0) * matrix.getQuick(2, 2));
        return matrix;
    }


    private void prepareMatrixNPs(String pathFile) throws IOException, InvalidLengthsException {
        String file = readFile(pathFile, true);

        ///DECLARATIONS
        Map<String, Integer> lMapTokenRow = new HashMap<>();
        ArrayList<String> pages = new ArrayList(Arrays.asList(file.split("%%#PAGE ")));
        Map<String, String> lpreClause;
        ArrayList<LinkedHashMap<Integer, String>> lListNPposition;
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
                System.out.println("\t\tsentence: " + sentenceID);
                lines.remove(0); ///> We remove the sentence ID from the list of lines
                Set<String> lClausesSeen = new HashSet<>(); ///> List that contains which clauses (NP) are found during the iteration
                lpreClause = new HashMap<>(); ///> Map that contains the subclauses and what prenodes compose them. NP_18:"DT_NN"
                lListNPposition = new ArrayList<>(); ///> List that contains dictionaries containing the position of the NPs in each word constituency string [{1:NP_18}]
                lListConstituencies = new ArrayList<>();
                lListTokensPOSSeen = new ArrayList<>();
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
                    lListNPposition.add(lNPposition);

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


                Map<Integer, Integer> temporalIndices = new HashMap<>();
                DefaultDict<String, HashSet<Integer>> lSubClausesColumns = new DefaultDict<>(HashSet.class);
                ArrayList<Integer> lConstituenciesSeen = new ArrayList<>();
                int innerLoopIdx = 0;
                int outerLoopIdx = 0;
                for (Map<Integer, String> npPosition : lListNPposition) { ///> This is i, per word
                    String currentWord = lListTokensPOSSeen.get(outerLoopIdx);
                    int currentWordRow = matrix.cTokenRow.get(currentWord);
                    for (Map.Entry<Integer, String> entry : npPosition.entrySet()) {
                        int index_NP = entry.getKey();
                        String subClause = entry.getValue();

                        /// We deal with row (words) indices here
                        matrix.cRows.add(currentWordRow); ///> We add the i index to the i vector of the ijv matrix
                        matrix.cData.add(1); ///> We add one to the data matrix
                        /// Now we deal with column indices
                        int tempIndex = 0;
                        /// Here I was under the impression that the index of the NP on the constituency list (2 in the case [NP_20, VP_70, S_61,..])
                        /// was enough to identify if a word belonged to the same NP, and thus put the value in the corresponding matrix.
                        /// I was wrong, the index of the NP may be the same and the NP may be a completely different one. So what else can
                        /// assure that two NPs are the same? The ancestors. So if the ancestor has been seen  and the position (second part of the
                        /// following if) then we can assure it is the same NP and thus corresponds to the same column,  no need to create a new
                        /// column.
                        /// If it is a seen before column, we get this seen column index
                        if (temporalIndices.containsKey(index_NP) && lConstituenciesSeen.contains(lListConstituencies.get(innerLoopIdx)))
                            tempIndex = temporalIndices.get(index_NP);
                        else ///> if this is a new NP, we add a new column
                            tempIndex = ++column_j;
//                        System.out.println(column_j);
                        temporalIndices.put(index_NP, tempIndex);
                        matrix.cCols.add(tempIndex);
                        matrix.cSubClausesColumns.get(subClause).add(temporalIndices.get(index_NP));
                        lSubClausesColumns.get(subClause).add(temporalIndices.get(index_NP));
                        lConstituenciesSeen.add(lListConstituencies.get(innerLoopIdx));
                        innerLoopIdx++;
                    }
                    outerLoopIdx++;
                }//Add NPs loop; i loop

                //Here we add the ngrams
                computeNgrams(lListTokensPOSSeen, 3);

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


//    public  void run() throws InterruptedException {
//        long start = System.nanoTime();
//        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
//        ArrayList<String> listPaths = listFiles(pathFolder, "txt");
////        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/AA/wiki_00");
//        for (String path : listPaths) {
//            System.out.println("still working...");
//            Runnable worker = new MatrixThread(path);
//            //worker will execute its "run" function
//            executor.execute(worker);
//        }
//        // This will make the executor accept no new threads
//        // and finish all existing threads in the queue
//        executor.shutdown();
//        // Wait until all threads are finish
//        executor.awaitTermination(10, TimeUnit.DAYS);
//        long time = System.nanoTime() - start;
//        System.out.println("Finished all threads");
//        System.out.printf("Tasks took %.3f m to run%n", time/(60*1e9));
//    }

    /**
     * This function runs the stanfordwiki2sparsematrix conversion in a single thread
     */
    public void runSingle() throws IOException, InvalidLengthsException {
        long start = System.nanoTime();
        ArrayList<String> listPaths = listFiles(pathFolder, "txt");

        /* big loop with for each file of the wiki stanford-parsed files*/
        int idx = 1;

        ///> This loops goes file by file
        for (String path : listPaths) {
            System.out.println(Integer.toString(idx) + ": " + path);
            this.prepareMatrixNPs(path);
            idx++;
            System.out.flush();
        }

        if (this.matrix.cRows.size() != this.matrix.cCols.size())
            throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something is wrong...");
        ///>Print some matrix statistics
        String stats = String.format("Number of rows: %d\n" +
                        "Number of columns: %d\n" +
                        "Number of unique types of clauses: %d\n" +
                        "Number of non-zero values: %d\n" +
                        "Sparsity: %f %%\n",
                this.matrix.getNumberRows(), this.matrix.getNumberColumns(), this.matrix.cClauseSubClauseColumns.keySet().size(), this.matrix.getNumberNonZeroElements(),
                this.matrix.sparsity());


//        System.out.printf("rows max: %d\n", Collections.max(this.matrix.cRows));
//        System.out.printf("cols max: %d\n", Collections.max(this.matrix.cCols));

        //Create mahout SparseMatrix
        Matrix myMatrix = makeMahoutSparseMatrix(matrix.cRows, matrix.cCols, matrix.cData);
        System.out.println(stats);
        long time = System.nanoTime() - start;
        System.out.println("Finished all threads");
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));

    }
}