package com.debangis;

public class ArrayPair {
	//Combines (encapsulate two arrays as an object)
    private String[] firstArray;
    private String[] secondArray;

    public ArrayPair() {
    	//Just to instantiate an object
    }
    public ArrayPair(String[] firstArray, String[] secondArray) {
        this.firstArray = firstArray;
        this.secondArray = secondArray;
    }

    public String[] getFirstArray() {
        return firstArray;
    }

    public String[] getSecondArray() {
        return secondArray;
    }
    public ArrayPair makeArrayPair(String[] first, String[] second) {
        
        // Create an instance of ArrayPair with the generated arrays
        return new ArrayPair(first, second);
    }
}
