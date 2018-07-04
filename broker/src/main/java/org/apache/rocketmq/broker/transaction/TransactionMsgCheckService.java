/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.broker.transaction;

import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.common.ServiceThread;
import org.apache.rocketmq.common.constant.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class TransactionMsgCheckService extends ServiceThread {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.TRANSACTION_LOGGER_NAME);

    private BrokerController brokerController;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public TransactionMsgCheckService(BrokerController brokerController) {
        this.brokerController = brokerController;
    }

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            super.start();
            this.brokerController.getTransactionMsgService().open();
        }
    }

    @Override
    public void shutdown(boolean interrupt) {
        if (started.compareAndSet(true, false)) {
            super.shutdown(interrupt);
            this.brokerController.getTransactionMsgService().close();
            this.brokerController.getTransactionCheckListener().shutDown();
        }
    }

    @Override
    public String getServiceName() {
        return TransactionMsgCheckService.class.getSimpleName();
    }

    @Override
    public void run() {
        log.info("Start transaction service thread!");
        long timeout = brokerController.getBrokerConfig().getTransactionTimeOut();
        int checkMax = brokerController.getBrokerConfig().getTransactionCheckMax();
        long checkInterval = brokerController.getBrokerConfig().getTransactionCheckInterval();
        log.info("Check parameter: transactionCheckMax: {}, transactionTimeOut: {} transactionCheckInterval: {}", checkMax, timeout, checkInterval);
        while (!this.isStopped()) {
            try {
                Thread.sleep(checkInterval);
                this.brokerController.getTransactionMsgService().check(timeout, checkMax, this.brokerController.getTransactionCheckListener());
            } catch (Exception e) {
                log.error("", e);
            }
        }
        log.info("End transaction service thread!");
    }

}
