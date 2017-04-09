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

package org.yes.cart.service.image.impl;

import org.yes.cart.constants.AttributeNamesKeys;
import org.yes.cart.constants.Constants;
import org.yes.cart.dao.GenericDAO;
import org.yes.cart.domain.entity.AttrValueBrand;
import org.yes.cart.service.misc.LanguageService;

import java.util.List;

/**
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 09-May-2011
 * Time: 14:12:54
 */
public class BrandImageNameStrategyImpl extends AbstractImageNameStrategyImpl {

    private static final String ATTR_CODE_LIKE = AttributeNamesKeys.Brand.BRAND_IMAGE_PREFIX + '%';

    private final GenericDAO<AttrValueBrand, Long> attrValueBrandDao;

    /**
     * Construct image name strategy
     *
     * @param relativeInternalRootDirectory  internal image relative path root directory without {@link java.io.File#separator}. E.g. "brand"
     * @param attrValueBrandDao              brand attribute dao
     * @param languageService                language service
     */
    public BrandImageNameStrategyImpl(final String relativeInternalRootDirectory,
                                      final GenericDAO<AttrValueBrand, Long> attrValueBrandDao,
                                      final LanguageService languageService) {
        super(Constants.BRAND_IMAGE_REPOSITORY_URL_PATTERN, relativeInternalRootDirectory, languageService);
        this.attrValueBrandDao = attrValueBrandDao;
    }

    /**
     * {@inheritDoc}
     */
    protected String resolveObjectCodeInternal(final String url) {

        final String val = resolveFileName(url);

        final List<Object[]> nameAndGuid = (List) attrValueBrandDao.findQueryObjectByNamedQuery("BRAND.NAME.AND.GUID.BY.IMAGE.NAME", val, ATTR_CODE_LIKE);

        if (nameAndGuid != null && !nameAndGuid.isEmpty()) {
            final Object[] nameAndGuidPair = nameAndGuid.get(0);
            if (nameAndGuidPair[0] instanceof String) {
                return (String) nameAndGuidPair[0];
            } else if (nameAndGuidPair[1] instanceof String) {
                return (String) nameAndGuidPair[1];
            }
        }

        return null;

    }

}
