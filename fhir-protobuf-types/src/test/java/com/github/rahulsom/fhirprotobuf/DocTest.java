package com.github.rahulsom.fhirprotobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.fhir.stu3.HumanName;
import org.fhir.stu3.Identifier;
import org.fhir.stu3.Practitioner;
import org.fhir.stu3.Practitioner_gender;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DocTest {

    @Test
    public void testParsingWorks() throws InvalidProtocolBufferException {
        Practitioner practitioner = Practitioner.newBuilder().
                setId("Practitioner_1").
                addName(HumanName.newBuilder().
                        addGiven("Bob").
                        setFamily("Kelso")).
                setGender(Practitioner_gender.Practitioner_gender_male).
                addIdentifier(Identifier.newBuilder().
                        setSystem("https://stmarys.com/practitioners").
                        setValue("BK001")).
                build();

        assertNotNull(practitioner);

        byte[] protobufByteArray = practitioner.toByteArray();

        Practitioner practitionerFromBinary = Practitioner.parseFrom(protobufByteArray);

        assertEquals("Practitioner_1", practitionerFromBinary.getId());
        assertEquals(1, practitionerFromBinary.getNameCount());
        assertEquals("Bob", practitionerFromBinary.getName(0).getGiven(0));

        String protobufJson = JsonFormat.printer().print(practitioner);

        Practitioner.Builder jsonPractitionerBuilder = Practitioner.newBuilder();
        JsonFormat.parser().merge(protobufJson, jsonPractitionerBuilder);
        Practitioner practitionerFromJson = jsonPractitionerBuilder.build();

        assertEquals("Practitioner_1", practitionerFromJson.getId());
        assertEquals(1, practitionerFromJson.getNameCount());
        assertEquals("Bob", practitionerFromJson.getName(0).getGiven(0));

    }
}
