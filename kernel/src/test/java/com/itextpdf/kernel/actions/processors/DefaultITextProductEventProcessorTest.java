/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2021 iText Group NV
    Authors: iText Software.

    This program is offered under a commercial and under the AGPL license.
    For commercial licensing, contact us at https://itextpdf.com/sales.  For AGPL licensing, see below.

    AGPL licensing:
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.itextpdf.kernel.actions.processors;

import com.itextpdf.kernel.PdfException;
import com.itextpdf.kernel.actions.session.ClosingSession;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.type.UnitTest;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category(UnitTest.class)
public class DefaultITextProductEventProcessorTest extends ExtendedITextTest {
    @Rule
    public ExpectedException junitExpectedException = ExpectedException.none();

    @Test
    public void constructorWithNullProductNameTest() {
        junitExpectedException.expect(IllegalArgumentException.class);
        junitExpectedException.expectMessage(PdfException.ProductNameCannotBeNull);
        new DefaultITextProductEventProcessor(null);
    }

    @Test
    public void buildFirstLineOfProducerTest() {
        DefaultITextProductEventProcessor processor = new DefaultITextProductEventProcessor("test-product");
        ClosingSession session = new ClosingSession(null);

        processor.aggregationOnClose(session);

        Assert.assertNotNull(session.getProducer());
        Assert.assertEquals(1, session.getProducer().size());
        Assert.assertEquals("test-product", session.getProducer().get(0));

        processor.completionOnClose(session);

        Assert.assertNull(session.getProducer());

    }

    @Test
    public void buildSecondLineOfProducerTest() {
        DefaultITextProductEventProcessor processor = new DefaultITextProductEventProcessor("test-product");
        ClosingSession session = new ClosingSession(null);
        List<String> producer = new ArrayList<>();
        producer.add("some producer");
        session.setProducer(producer);

        processor.aggregationOnClose(session);

        Assert.assertNotNull(session.getProducer());
        Assert.assertEquals(2, session.getProducer().size());
        Assert.assertEquals("some producer", session.getProducer().get(0));
        Assert.assertEquals("test-product", session.getProducer().get(1));

        processor.completionOnClose(session);

        Assert.assertNull(session.getProducer());

    }
}
