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
package com.itextpdf.kernel.actions.producer;

import com.itextpdf.io.util.MessageFormatUtil;
import com.itextpdf.kernel.PdfException;
import com.itextpdf.kernel.actions.events.ITextProductEventWrapper;

import java.util.List;

/**
 * Class is used to populate <code>copyrightSince</code> placeholder. The resulting string is a
 * representation of the first year of copyright years range. Among all products involved into
 * product creation the earliest <code>copyrightSince</code> year is picked as a resulting value.
 */
class CopyrightSincePlaceholderPopulator implements IPlaceholderPopulator {

    /**
     * Builds a replacement for a placeholder <code>copyrightSince</code> in accordance with the
     * registered events.
     *
     * @param events is a list of event involved into document processing. It is expected that it
     *               is not empty as such cases should be handled by {@link ProducerBuilder} without
     *               calling any {@link IPlaceholderPopulator}
     * @param parameter is a parameter for the placeholder. It should be <code>null</code> as
     *                  <code>copyrightSince</code> as the placeholder is not configurable
     *
     * @return the earliest copyright year
     *
     * @throws IllegalArgumentException if <code>parameter</code> is not <code>null</code>
     */
    @Override
    public String populate(List<ITextProductEventWrapper> events, String parameter) {

        if (parameter != null) {
            throw new IllegalArgumentException(
                    MessageFormatUtil.format(PdfException.InvalidUsageConfigurationForbidden, "copyrightSince")
            );
        }

        // initial value, will be overwritten with product value
        int earliestYear = Integer.MAX_VALUE;
        for (ITextProductEventWrapper event : events) {
            int currentYear = event.getEvent().getProductData().getSinceCopyrightYear();
            if (currentYear < earliestYear) {
                earliestYear = currentYear;
            }
        }
        return String.valueOf(earliestYear);
    }
}
