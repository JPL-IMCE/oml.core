/*
 * Copyright 2016 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * License Terms
 */

package test.java.oml.tables;

import org.junit.Test;
import org.junit.Assert;

import gov.nasa.jpl.imce.oml.tables.PlainLiteralScalarRestriction;
import gov.nasa.jpl.imce.oml.tables.PlainLiteralScalarRestrictionHelper;

public class PlainLiteralScalarRestrictionTest {

    @Test
    public void creationTest() {
        String tboxUUID = "01234-abcde-4569-fehi";
        String uuid = "12345-BCDEF-6789A-012345";
        String name = "abc";
        String restrictedRangeUUID = "1245-ABCD-2345-4567";

        PlainLiteralScalarRestriction w1 = new PlainLiteralScalarRestriction(uuid, tboxUUID, restrictedRangeUUID, name)
                .withLength(5)
                .withMaxLength(50)
                .withMinLength(1)
                .withPattern("yyy")
                .withLangRange("en");

        String s1 = PlainLiteralScalarRestrictionHelper.toJSON(w1);

        String lan_s = "[\"" + w1.langRange().get() + "\"]";

        String ls = "[" + w1.length().get() + "]";

        String maxL = "[" + w1.maxLength().get() + "]";

        String minL = "[" + w1.minLength().get() + "]";

        String ps = "[\"" + w1.pattern().get() + "\"]";

        String t1 = String.format(
                "{\"uuid\":\"%s\",\"tboxUUID\":\"%s\",\"restrictedRangeUUID\":\"%s\",\"length\":%s,\"minLength\":%s,\"maxLength\":%s,\"name\":\"%s\",\"langRange\":%s,\"pattern\":%s}",
                uuid, tboxUUID, restrictedRangeUUID, ls, minL, maxL, name, lan_s, ps);
        Assert.assertEquals(t1, s1);

        PlainLiteralScalarRestriction r1 = PlainLiteralScalarRestrictionHelper.fromJSON(s1);
        Assert.assertEquals(w1.tboxUUID(), r1.tboxUUID());
        Assert.assertEquals(w1.uuid(), r1.uuid());
        Assert.assertEquals(w1.name(), r1.name());
        Assert.assertEquals(w1.length(), r1.length());
        Assert.assertEquals(w1.maxLength(), r1.maxLength());
        Assert.assertEquals(w1.minLength(), r1.minLength());
        Assert.assertEquals(w1.pattern(), r1.pattern());
        Assert.assertEquals(w1.restrictedRangeUUID(), r1.restrictedRangeUUID());
        Assert.assertTrue(w1.equals(r1));
    }
}
