/*
 * Copyright 2009 Denys Pavlov, Igor Azarnyi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.yes.cart.bulkcommon.service.support.impl;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.yes.cart.bulkcommon.model.ImpExDescriptor;
import org.yes.cart.bulkcommon.model.ImpExTuple;
import org.yes.cart.bulkcommon.model.ValueAdapter;
import org.yes.cart.bulkcommon.service.support.LookUpQuery;
import org.yes.cart.bulkcommon.service.support.LookUpQueryParameterStrategy;
import org.yes.cart.bulkcommon.service.support.LookUpQueryParameterStrategyValueProvider;
import org.yes.cart.bulkimport.csv.CsvImportColumn;
import org.yes.cart.bulkimport.csv.CsvImportDescriptor;
import org.yes.cart.bulkimport.model.ImportTuple;
import org.yes.cart.domain.entity.Identifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * User: denispavlov
 * Date: 12-08-08
 * Time: 10:06 AM
 */
public class AbstractByParameterByColumnNameStrategyTest {

    private final Mockery mockery = new JUnit4Mockery();

    @Test
    public void testReplace() {
        final AbstractByParameterByColumnNameStrategy strategy = new AbstractByParameterByColumnNameStrategy() {
            @Override
            protected boolean addParameter(final int index,
                                        final boolean wrappedInQuotes, final Object param,
                                        final StringBuilder query,
                                        final List<Object> params) {
                query.append('?').append(index);
                params.add(param);
                return true;
            }

            @Override
            public LookUpQuery getQuery(final ImpExDescriptor descriptor,
                                        final Object masterObject,
                                        final ImpExTuple tuple,
                                        final ValueAdapter adapter,
                                        final String queryTemplate) {
                return null;
            }
        };

        strategy.setProviders(new HashMap<String, LookUpQueryParameterStrategyValueProvider>() {{
            put(LookUpQueryParameterStrategy.MASTER_ID, new MasterObjectIdLookUpQueryParameterStrategyValueProviderImpl());
        }});
        strategy.setDefaultProvider(new ColumnValueLookUpQueryParameterStrategyValueProviderImpl());

        final Identifiable master = mockery.mock(Identifiable.class, "master");
        final CsvImportDescriptor descriptor = mockery.mock(CsvImportDescriptor.class, "descriptor");
        final CsvImportColumn codeColumn = mockery.mock(CsvImportColumn.class, "codeColumn");
        final ImportTuple tuple = mockery.mock(ImportTuple.class, "tuple");
        final ValueAdapter adapter = mockery.mock(ValueAdapter.class, "adapter");


        mockery.checking(new Expectations() {{
            oneOf(master).getId(); will(returnValue(10L));
            oneOf(descriptor).getColumn("code"); will(returnValue(codeColumn));
            oneOf(tuple).getColumnValue(codeColumn, adapter); will(returnValue("A'''BC"));
        }});

        final StringBuilder query = new StringBuilder();
        final List params = new ArrayList();
        strategy.replaceColumnNamesInTemplate(
                "select * from Entity e where e.parentId = {masterObjectId} and e.code = '{code}' ",
                query, params, descriptor, master, tuple, adapter);

        assertEquals(query.toString(), "select * from Entity e where e.parentId = ?1 and e.code = ?2 ");
        assertEquals(params.size(), 2);
        assertEquals(params.get(0), Long.valueOf(10L));
        assertEquals(params.get(1), "A'''BC"); // escaped value

        mockery.assertIsSatisfied();
    }


}
