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

package org.yes.cart.service.domain.impl;

import org.apache.commons.lang.StringUtils;
import org.yes.cart.dao.GenericDAO;
import org.yes.cart.domain.entity.Tax;
import org.yes.cart.domain.entity.TaxConfig;
import org.yes.cart.service.domain.TaxConfigService;
import org.yes.cart.service.domain.TaxService;
import org.yes.cart.utils.HQLUtils;

import java.util.Comparator;
import java.util.List;

/**
 * User: denispavlov
 * Date: 27/10/2014
 * Time: 20:08
 */
public class TaxConfigServiceImpl extends BaseGenericServiceImpl<TaxConfig> implements TaxConfigService {

    private final TaxService taxService;

    public TaxConfigServiceImpl(final GenericDAO<TaxConfig, Long> genericDao,
                                final TaxService taxService) {
        super(genericDao);
        this.taxService = taxService;
    }

    /** {@inheritDoc} */
    @Override
    public Long getTaxIdBy(final String shopCode, final String currency, final String countryCode, final String stateCode, final String productCode) {

        final List<Tax> shopTaxes = taxService.getTaxesByShopCode(shopCode, currency);
        if (shopTaxes.isEmpty()) {
            return null;
        }

        final List<TaxConfig> taxConfigs = getGenericDao().findByNamedQuery("TAXCONFIG.BY.COUNTRYCODE.STATECODE.PRODUCTCODE.IN.TAXES", shopTaxes, countryCode, stateCode, productCode);
        if (taxConfigs.isEmpty()) {
            return null;
        }

        taxConfigs.sort(PRIORITY);

        return taxConfigs.get(0).getTax().getTaxId();

    }

    /**
     * The sorting rule:
     * 1. Product state specific tax
     * 2. Product country specific tax
     * 3. Product specific tax
     * 4. State specific tax
     * 5. Country specific tax
     * 6. Shop specific tax
     */
    static final Comparator<TaxConfig> PRIORITY = new Comparator<TaxConfig>() {

        private int UP = -1;
        private int DOWN = 1;

        @Override
        public int compare(final TaxConfig tc1, final TaxConfig tc2) {

            final String tc1country = StringUtils.isEmpty(tc1.getCountryCode()) ? null : tc1.getCountryCode();
            final String tc1state = StringUtils.isEmpty(tc1.getStateCode()) ? null : tc1.getStateCode();
            final String tc1product = StringUtils.isEmpty(tc1.getProductCode()) ? null : tc1.getProductCode();

            final String tc2country = StringUtils.isEmpty(tc2.getCountryCode()) ? null : tc2.getCountryCode();
            final String tc2state = StringUtils.isEmpty(tc2.getStateCode()) ? null : tc2.getStateCode();
            final String tc2product = StringUtils.isEmpty(tc2.getProductCode()) ? null : tc2.getProductCode();


            if (tc1product != null) { // product specific tax
                if (tc1state != null) { // product state specific tax
                    return UP; // should be only 1 product state specific tax
                } else if (tc1country != null) { // product country specific tax
                    // product state specific tax wins
                    return tc2product != null && tc2state != null ? DOWN : UP;
                } // else product shop specific tax
                return tc2product != null && (tc2country != null || tc2state != null) ? DOWN : UP;
            } else if (tc1state != null) { // state specific tax
                if (tc2product != null) {
                    return DOWN; // product specific tax is higher up
                }
                return UP; // should be only 1 state specific tax
            } else if (tc1country != null) { // country specific tax
                if (tc2product != null || tc2state != null) {
                    return DOWN; // product specific or state specific tax is higher up
                }
                return UP;
            } // else shop specific (all nulls)
            return DOWN;
        }
    };

    /** {@inheritDoc} */
    @Override
    public List<TaxConfig> findByTaxId(final long taxId,
                                       final String countryCode,
                                       final String stateCode,
                                       final String productCode) {

        return getGenericDao().findByNamedQuery(
                "TAXCONFIG.BY.TAX.COUNTRY.STATE.PRODUCT",
                taxId,
                HQLUtils.criteriaEq(countryCode),
                HQLUtils.criteriaEq(stateCode),
                HQLUtils.criteriaEq(productCode)
            );
    }


    private void cleanRegionalTaxCodes(final TaxConfig entity) {
        if (entity.getCountryCode() != null && StringUtils.isBlank(entity.getCountryCode())) {
            entity.setCountryCode(null);
        }
        if (entity.getStateCode() != null && StringUtils.isBlank(entity.getStateCode())) {
            entity.setStateCode(null);
        }
        if (entity.getProductCode() != null && StringUtils.isBlank(entity.getProductCode())) {
            entity.setProductCode(null);
        }
    }

    private void regenerateGuid(final TaxConfig entity) {
        final StringBuilder guid = new StringBuilder();
        if (entity.getTax() != null) {
            guid.append(entity.getTax().getGuid());
        }
        if (entity.getCountryCode() != null) {
            guid.append('_').append(entity.getCountryCode());
        }
        if (entity.getStateCode() != null) {
            guid.append('_').append(entity.getStateCode());
        }
        if (entity.getProductCode() != null) {
            guid.append('_').append(entity.getProductCode());
        }
        entity.setGuid(guid.toString());
    }

    /** {@inheritDoc} */
    @Override
    public TaxConfig create(final TaxConfig instance) {
        cleanRegionalTaxCodes(instance);
        regenerateGuid(instance);
        return super.create(instance);
    }

    /** {@inheritDoc} */
    @Override
    public TaxConfig update(final TaxConfig instance) {
        cleanRegionalTaxCodes(instance);
        regenerateGuid(instance);
        return super.update(instance);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(final TaxConfig instance) {
        super.delete(instance);
    }
}
