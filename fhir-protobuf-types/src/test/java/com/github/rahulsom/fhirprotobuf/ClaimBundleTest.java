package com.github.rahulsom.fhirprotobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.fhir.stu3.*;
import org.junit.Test;

public class ClaimBundleTest {
    @Test
    public void testClaimBundle() throws InvalidProtocolBufferException {
        Bundle bundle = Bundle.newBuilder()
                .setType(Bundle_type.Bundle_type_collection)
                .addEntry(Bundle_Entry.newBuilder()
                        .setResource(ResourceList.newBuilder()
                                .setClaim(Claim.newBuilder()
                                        .setOrganization(Reference.newBuilder()
                                                .setReference("Hello")))))
                .build();

        byte[] protobufByteArray = bundle.toByteArray();

        Bundle bundleFromBinary = Bundle.parseFrom(protobufByteArray);

        String protobufJson = JsonFormat.printer().print(bundle);
        System.out.println(protobufJson);

        Bundle.Builder jsonPractitionerBuilder = Bundle.newBuilder();
        JsonFormat.parser().merge(protobufJson, jsonPractitionerBuilder);
        Bundle practitionerFromJson = jsonPractitionerBuilder.build();

    }
}
