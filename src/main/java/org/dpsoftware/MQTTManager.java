/*
  MQTTManager.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/

package org.dpsoftware;

import lombok.NoArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor
public class MQTTManager implements MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(MQTTManager.class);

    MqttClient client;
    Configuration config;
    FastScreenCapture fastScreenCapture;
    boolean connected = false;
    boolean reconnectionThreadRunning = false;

    /**
     * Constructor
     * @param config
     * @param fastScreenCapture
     */
    public MQTTManager(Configuration config, FastScreenCapture fastScreenCapture) {

        try {
            this.config = config;
            attemptReconnect();
            this.fastScreenCapture = fastScreenCapture;
        } catch (MqttException e) {
            connected = false;
            logger.error("Can't connect to MQTT Server");
        }

    }

    /**
     * Reconnect to MQTT Broker
     * @throws MqttException
     */
    void attemptReconnect() throws MqttException {

        client = new MqttClient(config.getMqttServer(), "JavaFastScreenCapture");
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setAutomaticReconnect(true);
        connOpts.setCleanSession(true);
        connOpts.setConnectionTimeout(10);
        connOpts.setUserName(config.getMqttUsername());
        connOpts.setPassword(config.getMqttPwd().toCharArray());
        client.connect(connOpts);
        client.setCallback(this);
        client.subscribe(config.getMqttTopic());
        logger.debug("Connected to MQTT Server");
        connected = true;
        
    }

    /**
     * Publish to a topic
     * @param msg
     */
    public void publishToTopic(String msg) {

        MqttMessage message = new MqttMessage();
        message.setPayload(msg.getBytes());
        try {
            client.publish(config.getMqttTopic(), message);
        } catch (MqttException e) {
            logger.error("Cant't send MQTT msg");
        }

    }

    /**
     * Reconnect on connection lost
     * @param cause
     */
    @Override
    public void connectionLost(Throwable cause) {

        logger.error("Connection Lost");
        connected = false;
        if (!reconnectionThreadRunning) {
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (!connected) {
                    try {
                        client.setCallback(this);
                        client.subscribe(config.getMqttTopic());
                        connected = true;
                        logger.debug("Reconnected");
                    } catch (MqttException e) {
                        logger.error("Disconnected");
                    }
                }
            }, 0, 10, TimeUnit.SECONDS);
        }

    }

    /**
     * Subscribe to the topic to START/STOP screen grabbing
     * @param topic
     * @param message
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {

        logger.debug(String.valueOf(message));
        if (message.toString().contains("START")) {
            fastScreenCapture.getTim().startCapturingThreads();
        } else if (message.toString().contains("STOP")) {
            fastScreenCapture.getTim().stopCapturingThreads(config);
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.debug("delivered");
    }

}
