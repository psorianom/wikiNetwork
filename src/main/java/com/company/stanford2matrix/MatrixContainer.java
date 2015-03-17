package com.company.stanford2matrix;

import com.company.stanford2matrix.Utils.DefaultDict;

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
    public Map<Integer, Integer> cNgramColVectorIndex;
    public Map<Integer, Integer> cNPColVectorIndex;
    //Label containers
    public HashMap<Integer, String> indicesColumns; ///> Contains the col index and the type it corresponds to.
    public DefaultDict<String, HashSet<Integer>> cSubClausesColumns; ///> Contains the rows'indices for each type of POS tag
    public Map<String, Integer> cTokenRow; ///> Contains the col index and the token it corresponds to.
    public DefaultDict<String, ArrayList<Integer>> cPOSToken; ///> Contains the indices of the tokens according to their POS tag: NN:[1,5,7], ...
    public Map<String, Integer> cNPwordsHashColumn; ///> Contains the words and the type of NP (concatenated) as key and the column id as value
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
        cNgramColVectorIndex = new HashMap<>();
        cNPColVectorIndex = new HashMap<>();

        //Labels information
        cTokenRow = new HashMap<>();
        indicesColumns = new HashMap<>();
        cClauseSubClauseColumns = new DefaultDict<>(DefaultDict.class);
        cSubClausesColumns = new DefaultDict<>(HashSet.class);
        cPOSToken = new DefaultDict<>();
        cNPwordsHashColumn = new HashMap<>();
        cNgramColumn = new HashMap();

    }


    public MatrixContainer(ArrayList<Integer> cRows, ArrayList<Integer> cCols, ArrayList<Integer> data,
                           DefaultDict<String, Integer> mapTokenRow, HashMap<Integer, String> indicesColumns,
                           Map<Integer, Integer> cNgramColVectorIndex,
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
        sparsity = this.getNumberNonZeroElements() / ((float) this.getNumberRows() * this.getNumberColumns());
        return sparsity;
    }

}
