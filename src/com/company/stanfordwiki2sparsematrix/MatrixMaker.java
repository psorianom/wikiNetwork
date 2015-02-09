package com.company.stanfordwiki2sparsematrix;

import java.io.IOException;
import java.util.*;

import static com.company.stanfordwiki2sparsematrix.Utils.*;

public class MatrixMaker {
    private static int nThreads;
    private static String pathFolder;
    private static int column_j = 0;
    private int row_i = 0; ///> The i index for the matrix
    //All this should not be part of the class... it should just have a MatrixContainer object
    private MatrixContainer matrix;


    //Constructor
    MatrixMaker(String pathFolder, int nThreads) {
        MatrixMaker.nThreads = nThreads;
        MatrixMaker.pathFolder = pathFolder;
        row_i = 0; ///> The i index for the matrix
        column_j = 0; ///> The j index for the matrix
        matrix = new MatrixContainer();
    }

    public static void main(String[] args) throws InterruptedException, IOException, InvalidLengthsException {
        MatrixMaker myMaker = new MatrixMaker("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data", 15);
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

    private Map<Integer, String> getNPs(ArrayList<String> clauses) {
        Map<Integer, String> npPosition = new LinkedHashMap<>();

        for (int i = 0; i < clauses.size(); i++)
            if (clauses.get(i).contains("NP"))
                npPosition.put(i + column_j, clauses.get(i));

        return npPosition;
    }

    private void makeMatrix(String pathFile) throws IOException, InvalidLengthsException {
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
                    if (matrix.cMapTokenRow.containsKey(token_pos))
                        matrix.cRows.add(matrix.cMapTokenRow.get(token_pos));
                    else {
                        matrix.cMapTokenRow.put(token_pos, row_i);
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

    private void makeMatrixNPs(String pathFile) throws IOException, InvalidLengthsException {
        String file = readFile(pathFile, true);

        ///DECLARATIONS
        int level = 1 + 1;
        Map<String, Integer> lMapTokenRow = new HashMap<>();
        ArrayList<String> pages = new ArrayList(Arrays.asList(file.split("%%#PAGE ")));
        Map<String, String> lpreClause;
        ArrayList<Map<Integer, String>> lListNPposition;

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
                Set<String> lClausesSeen = new HashSet<>();
                ArrayList<String> lSubClausesSeen = new ArrayList<>();
                lpreClause = new HashMap<>();
                lListNPposition = new ArrayList<>();

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
                    if (matrix.cMapTokenRow.containsKey(token_pos))
                        matrix.cRows.add(matrix.cMapTokenRow.get(token_pos));
                    else {
                        matrix.cMapTokenRow.put(token_pos, row_i);
                        matrix.cRows.add(row_i);
                        row_i++;
                    }

                    /**
                     * 2. Using the constituency results, we find all the NPs clauses contained in the phrase.
                     *      We discard, at the beginning, any other type of clause (VP, ADJP, PP, PRP, etc).
                     *
                     */
                    if (!constituency.contains("NP"))
                        continue;

                    ArrayList<String> clauses = new ArrayList(Arrays.asList(constituency.split(",")));
                    Map<Integer, String> lNPposition = getNPs(clauses);
                    lListNPposition.add(lNPposition);
                    /**
                     * We get the preclauses of each of the NPs this token belongs to
                     */
                    for (Map.Entry<Integer, String> entry : lNPposition.entrySet()) {
                        Integer index_j = entry.getKey();
                        String targetClause = entry.getValue();

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


                    }

                }// end of current line. Sentence is completely read.
                /**
                 * 3. Once the complete pass over the sentence is done, we get what represents each column.
                 * 3.1 We get the columns for each different type of clause. In a dict str:int. Keys are
                 * the clauses, columns are the values: {"NP_18": [1,3,5,7], "VP_70":[2,4], ...}
                 */
                if (lClausesSeen.isEmpty())
                    continue;

                Map<String, ArrayList<Integer>> lSubClausesColumns = new DefaultDict<>();
                for (Map<Integer, String> npPosition : lListNPposition) {
                    for (Map.Entry<Integer, String> entry : npPosition.entrySet()) {
                        int index_j = entry.getKey();
                        String v = entry.getValue();
                        for (String k2 : lpreClause.keySet()) {
                            if (k2.equals(v)) {
                                matrix.cSubClausesColumns.get(v).add(index_j);
                                lSubClausesColumns.get(v).add(index_j);
                            }
                        }

                    }

                    }

                for (String cl : lClausesSeen) {
                    for (Map.Entry<String, String> entry : lpreClause.entrySet()) {
                        String clause = entry.getKey();
                        String clauseComponents = entry.getValue();
                        matrix.cClauseSubClauseColumns.get(cl).get(clauseComponents).addAll(lSubClausesColumns.get(clause));
                    }

                }
                column_j++;

            }
        }
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
            this.makeMatrixNPs(path);
            idx++;
            System.out.flush();
        }

        if (this.matrix.cRows.size() != this.matrix.cCols.size())
            throw new Utils.InvalidLengthsException("The length of vector i and j should be ALWAYS the same. Something is wrong...");
        HashSet<Integer> hasho = new HashSet<>(this.matrix.cCols);
        ///>Print some matrix statistics
        String stats = String.format("Number of rows: %d\nNumber of columns: %d\nNumber of unique types of clauses: %d\nNumber of non-zero values: %d\n",
                this.matrix.cMapTokenRow.keySet().size(), hasho.size(), this.matrix.cClauseSubClauseColumns.keySet().size(), this.matrix.cCols.size());
        System.out.println(stats);
        long time = System.nanoTime() - start;
        System.out.println("Finished all threads");
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));

    }
}