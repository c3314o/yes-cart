/*
 * Copyright 2009 - 2016 Denys Pavlov, Igor Azarnyi
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

package org.yes.cart.service.order.impl;

import org.yes.cart.domain.dto.CustomerOrderDeliveryDTO;
import org.yes.cart.domain.misc.Result;
import org.yes.cart.service.dto.DtoCustomerOrderService;
import org.yes.cart.service.order.OrderFlowAction;

import java.util.List;
import java.util.Map;

/**
 * User: denispavlov
 * Date: 01/09/2016
 * Time: 17:43
 */
public class GenericDeliveryTransitionAction implements OrderFlowAction {

    private final DtoCustomerOrderService dtoCustomerOrderService;
    private final String targetState;

    public GenericDeliveryTransitionAction(final DtoCustomerOrderService dtoCustomerOrderService,
                                           final String targetState) {
        this.dtoCustomerOrderService = dtoCustomerOrderService;
        this.targetState = targetState;
    }

    @Override
    public Result doTransition(final String deliverynum, final Object params) {

        final Map<String, String> map = (Map<String, String>) params;

        final String ordernum = map.get("ordernum");
        String currentState = "";

        try {
            final List<CustomerOrderDeliveryDTO> deliveries = dtoCustomerOrderService.findDeliveryByOrderNumber(ordernum, deliverynum);
            currentState = !deliveries.isEmpty() ? deliveries.get(0).getDeliveryStatus() : "";
        } catch (Exception e) {
            // do  nothing
        }

        return dtoCustomerOrderService.updateDeliveryStatus(map.get("ordernum"), deliverynum, currentState, targetState);
    }
}
