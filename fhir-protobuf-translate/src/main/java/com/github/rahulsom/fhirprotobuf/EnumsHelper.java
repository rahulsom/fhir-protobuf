package com.github.rahulsom.fhirprotobuf;

import com.google.common.collect.ImmutableList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class EnumsHelper {
    public ImmutableList<String[]> getEnums() {
        return enums;
    }

    static class Holder {
        static EnumsHelper INSTANCE = new EnumsHelper();
    }

    private ImmutableList<String[]> enums;

    private EnumsHelper() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("enums.csv");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        List<String[]> localEnums = new ArrayList<>();

        try {
            while(bufferedReader.ready()) {
                localEnums.add(bufferedReader.readLine().split(","));
            }
        } catch (IOException ignore) {
        }

        enums = ImmutableList.copyOf(localEnums);
    }
}
