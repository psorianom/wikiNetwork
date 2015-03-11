package com.company.stanfordwiki2sparsematrix;

import com.company.stanfordwiki2sparsematrix.Utils.DefaultDict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by pavel on 28/01/15.
 */
public class MatrixContainer {
    /**
     * This class contains the necessary arrays to hold a sparse matrix along with the information of its
     * rows and columns (what do each row and column actually means). The sparse matrix is stored in a COO-matrix,
     * that is, the data[i] is stored in (rows[i], cCols[i]), so we have three integer arrays.
     */
    //Sparse matrix container
    public ArrayList<Integer> cRows; ///> Contains row indices
    public ArrayList<Integer> cCols; ///> Contains columns indices
    public ArrayList<Integer> cData; ///> Contains the data

    //Intermediary containers and values
    public Map<Integer, Integer> cNgramColIndex;
    //Label containers
    public HashMap<Integer, String> indicesColumns; ///> Contains the col index and the type it corresponds to.
    public DefaultDict<String, HashSet<Integer>> cSubClausesColumns; ///> Contains the rows'indices for each type of POS tag
    public HashMap<String, ArrayList<Integer>> clauseColumns; ///> Contains the rows'indices for each type of clause
    public Map<String, Integer> cTokenRow; ///> Contains the col index and the token it corresponds to.
    public DefaultDict<String, ArrayList<Integer>> cPOSToken; ///> Contains the indices of the tokens according to their POS tag: NN:[1,5,7], ...
    /**
     * Contains the type of column there are (NP, VP, PRP, MODIF, ...) and the subtype, if present, ("DT_JJ_NN","DT_NN",etc).
     * Then the columns indices that correspond to each case. Something like {'NP':{"DT_NN":[1,2,3]}, ...}
     */
    public DefaultDict<String, DefaultDict<String, ArrayList<Integer>>> cClauseSubClauseColumns;
    public Map<String, Integer> cNgramColumn;
    private float sparsity;
    private int numColumns;

    /**
     * Simple constructor
     */
    public MatrixContainer() {
        //Matrix information
        cRows = new ArrayList<>();
        cData = new ArrayList<>();
        cCols = new ArrayList<>();


        //Intermediary structures and values
        cNgramColIndex = new HashMap<>();


        //Labels information
        cTokenRow = new HashMap<>();
        indicesColumns = new HashMap<>();
        cClauseSubClauseColumns = new DefaultDict<>(DefaultDict.class);
        cSubClausesColumns = new DefaultDict<>(HashSet.class);
        clauseColumns = new HashMap<>();
        cPOSToken = new DefaultDict<>();
        cNgramColumn = new HashMap();

    }


    public MatrixContainer(ArrayList<Integer> cRows, ArrayList<Integer> cCols, ArrayList<Integer> data,
                           DefaultDict<String, Integer> mapTokenRow, HashMap<Integer, String> indicesColumns,
                           Map<Integer, Integer> cNgramColIndex,
                           DefaultDict<String, DefaultDict<String, ArrayList<Integer>>> cClauseSubClauseColumns,
                           DefaultDict<String, HashSet<Integer>> cSubClausesColumns,
                           DefaultDict<String, ArrayList<Integer>> cPOSToken,
                           Map<String, Integer> cNgramColumn) {
        //Matrix information
        this.cRows = cRows;
        this.cData = data;
        this.cCols = cCols;

        //Labels information
        this.cTokenRow = mapTokenRow;
        this.indicesColumns = indicesColumns;
        this.cClauseSubClauseColumns = cClauseSubClauseColumns;
        this.cSubClausesColumns = cSubClausesColumns;
        this.cPOSToken = cPOSToken;
        this.cNgramColumn = cNgramColumn;
    }

    /**
     * This function converts the columns labels into a single label separated with underscores.
     *
     * @return Column label info concatenated
     */
    public String infoToText() {

        return "a";
    }

    public int getNumberRows() {
        return this.cTokenRow.keySet().size();
    }

    public int getNumberColumns() {
        HashSet<Integer> hasho;
        if (this.numColumns == 0) {
            hasho = new HashSet<>(this.cCols);
            this.numColumns = hasho.size();
        }

        return this.numColumns;
    }

    public int getNumberNonZeroElements() {
        return this.cRows.size();
    }

    public float sparsity() {
        return this.getNumberNonZeroElements() / ((float) this.getNumberRows() * this.getNumberColumns());
    }

}
