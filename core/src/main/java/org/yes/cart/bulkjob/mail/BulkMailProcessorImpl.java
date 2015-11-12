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

package org.yes.cart.bulkjob.mail;

import org.slf4j.Logger;
import org.springframework.mail.javamail.JavaMailSender;
import org.yes.cart.domain.entity.Mail;
import org.yes.cart.service.domain.MailService;
import org.yes.cart.service.domain.RuntimeAttributeService;
import org.yes.cart.service.domain.SystemService;
import org.yes.cart.service.mail.JavaMailSenderFactory;
import org.yes.cart.service.mail.MailComposer;
import org.yes.cart.util.ShopCodeContext;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * User: denispavlov
 * Date: 10/11/2013
 * Time: 13:56
 */
public class BulkMailProcessorImpl implements Runnable {

    private static final String PAUSE_PREF = "JOB_SEND_MAIL_PAUSE";

    private final MailService mailService;
    private final MailComposer mailComposer;
    private final JavaMailSenderFactory javaMailSenderFactory;
    private final SystemService systemService;
    private final RuntimeAttributeService runtimeAttributeService;

    private long delayBetweenEmailsMs;
    private int cycleExceptionsThreshold;

    private boolean pauseInitialised = false;

    public BulkMailProcessorImpl(final MailService mailService,
                                 final MailComposer mailComposer,
                                 final JavaMailSenderFactory javaMailSenderFactory,
                                 final SystemService systemService,
                                 final RuntimeAttributeService runtimeAttributeService) {
        this.mailService = mailService;
        this.mailComposer = mailComposer;
        this.javaMailSenderFactory = javaMailSenderFactory;
        this.systemService = systemService;
        this.runtimeAttributeService = runtimeAttributeService;
    }

    /** {@inheritDoc} */
    @Override
    public void run() {

        if (!pauseInitialised) {
            if (!systemService.getAttributeValues().keySet().contains(PAUSE_PREF)) {
                synchronized (SystemService.class) {
                    runtimeAttributeService.create(PAUSE_PREF, "SYSTEM", "Boolean");
                    systemService.updateAttributeValue(PAUSE_PREF, Boolean.FALSE.toString());
                }
            }
            pauseInitialised = true;
        }

        final String paused = systemService.getAttributeValue(PAUSE_PREF);
        if (Boolean.valueOf(paused)) {
            return;
        }

        final Logger log = ShopCodeContext.getLog(this);

        final long start = System.currentTimeMillis();

        log.info("Bulk send mail");

        final Map<String, Integer> exceptionsThresholdsByShop = new HashMap<String, Integer>();

        Long lastFailedEmailId = null;
        Mail mail = mailService.findOldestMail(lastFailedEmailId);
        while (mail != null) {

            log.info("Preparing mail object {}/{} for {} with subject {}",
                    new Object[] { mail.getMailId(), mail.getShopCode(), mail.getRecipients(), mail.getSubject() });

            final String shopCode = mail.getShopCode();

            if (!exceptionsThresholdsByShop.containsKey(shopCode)) {
                exceptionsThresholdsByShop.put(shopCode, this.cycleExceptionsThreshold);
            }

            int exceptionsThreshold = exceptionsThresholdsByShop.get(shopCode);
            if (exceptionsThreshold <= 0) {
                lastFailedEmailId = mail.getMailId();
                log.info("Skipping send mail as exception threshold is exceeded for shop {}", shopCode);
            } else {

                final JavaMailSender javaMailSender = javaMailSenderFactory.getJavaMailSender(shopCode);
                if (javaMailSender == null) {
                    log.info("No mail sender configured for {}", shopCode);
                    lastFailedEmailId = mail.getMailId();
                } else {

                    final MimeMessage mimeMessage = javaMailSender.createMimeMessage();

                    boolean sent = false;
                    try {
                        mailComposer.convertMessage(mail, mimeMessage);
                        javaMailSender.send(mimeMessage);
                        sent = true;
                        log.info("Sent mail to {} with subject {}", mail.getRecipients(), mail.getSubject());
                        mailService.delete(mail);
                    } catch (Exception exp) {
                        log.error("Unable to send mail " + mail.getMailId() + " for shop " + shopCode, exp);
                        lastFailedEmailId = mail.getMailId();
                        exceptionsThresholdsByShop.put(shopCode, exceptionsThreshold - 1);

                    }

                    if (sent && delayBetweenEmailsMs > 0) {
                        try {
                            Thread.sleep(delayBetweenEmailsMs);
                        } catch (InterruptedException e) {
                            // resume
                        }
                    }
                }
            }

            mail = mailService.findOldestMail(lastFailedEmailId);

        }

        final long finish = System.currentTimeMillis();

        final long ms = (finish - start);

        log.info("Bulk send mail ... completed in {}s", (ms > 0 ? ms / 1000 : 0));

    }

    /**
     * Setting to allow delay interval between sending mail. This is useful to prevent
     * bulk message be treated as spam.
     *
     * @param delayBetweenEmailsMs delay in millisecond between each email
     */
    public void setDelayBetweenEmailsMs(final long delayBetweenEmailsMs) {
        this.delayBetweenEmailsMs = delayBetweenEmailsMs;
    }

    /**
     * Setting to set limit of exceptions during single job cycle. Prevents exhausting
     * the server with send mail job that fails all the time.
     *
     * @param cycleExceptionsThreshold number of exceptions allowed in each job cycle.
     */
    public void setCycleExceptionsThreshold(final int cycleExceptionsThreshold) {
        this.cycleExceptionsThreshold = cycleExceptionsThreshold;
    }


}
