package com.github.rahulsom.fhirprotobuf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.protobuf.util.JsonFormat;
import org.fhir.stu3.Bundle;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

public class ConverterTest {
    private void testForFile(String filename, String expectFilename) throws IOException {

        FhirContext context = FhirContext.forDstu3();
        IParser parser = context.newJsonParser();

        String inputString = readFile(filename);

        parser.parseResource(org.hl7.fhir.dstu3.model.Bundle.class, inputString);

        String protoJson = Converter
                .fromFhirJson(inputString);

        Bundle.Builder builder = Bundle.newBuilder();

        JsonFormat
                .parser()
                .merge(protoJson, builder);

        Bundle protobufObject = builder.build();

        String rebuiltProtoJson = JsonFormat
                .printer()
                .print(protobufObject);

        String fhirJson = Converter
                .toFhirJson(rebuiltProtoJson);

        parser.parseResource(org.hl7.fhir.dstu3.model.Bundle.class, fhirJson);

        assertEquals(readFile(expectFilename), fhirJson);
    }

    private String readFile(String filename) throws IOException {
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream(filename);

        BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(resourceAsStream));

        StringBuilder stringBuilder = new StringBuilder();

        while (bufferedReader.ready()) {
            stringBuilder.append(bufferedReader.readLine());
            if (bufferedReader.ready()) {
                stringBuilder.append("\n");
            }
            ;
        }

        return stringBuilder.toString();
    }

    @Test
    public void testSmallFile() throws IOException {
        testForFile("small.json", "small-expect.json");
    }

    @Test
    public void testLargeFile() throws IOException {
        testForFile("large.json", "large-expect.json");
    }
}
