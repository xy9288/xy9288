package com.leon.datalink.resource.util.mqtt.client;

import com.leon.datalink.core.evn.EnvUtil;
import com.leon.datalink.core.utils.SSLUtils;
import com.leon.datalink.resource.util.mqtt.MqttClientConfig;
import com.leon.datalink.resource.util.mqtt.MqttClientFactory;
import com.leon.datalink.resource.util.mqtt.entity.MqttMessageEntity;
import com.leon.datalink.resource.util.mqtt.entity.MqttSubParam;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MqttClientV5 implements IMqttClient{

    private MqttClient mqttClient;

    @Override
    public void connect(MqttClientConfig mqttClientConfig) throws Exception {
        mqttClient = new MqttClient(mqttClientConfig.getHostUrl(), UUID.randomUUID().toString(), new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        // 设置连接的用户名
        options.setUserName(mqttClientConfig.getUsername());
        // 设置连接的密码
        options.setPassword(mqttClientConfig.getPassword().getBytes());
        // 设置自动重连
        options.setAutomaticReconnect(mqttClientConfig.getAutoReconnect());
        // 设置超时时间 单位为秒
        options.setConnectionTimeout(mqttClientConfig.getConnectionTimeout());
        // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
        options.setKeepAliveInterval(mqttClientConfig.getKeepAliveInterval());
        options.setCleanStart(mqttClientConfig.getCleanStart());
        options.setSessionExpiryInterval(mqttClientConfig.getSessionExpiryInterval());
        options.setMaximumPacketSize(mqttClientConfig.getMaximumPacketSize());
        options.setReceiveMaximum(mqttClientConfig.getReceiveMaximum());
        options.setTopicAliasMaximum(mqttClientConfig.getTopicAliasMaximum());
        options.setRequestProblemInfo(mqttClientConfig.getRequestProblemInfo());
        options.setRequestResponseInfo(mqttClientConfig.getRequestResponseInfo());
        // 是否开启ssl
        if (mqttClientConfig.getSsl()) {
            InputStream resourceAsStream = MqttClientFactory.class.getClassLoader().getResourceAsStream(EnvUtil.getCaCrtFile());
            options.setSocketFactory(SSLUtils.getSocketFactory(resourceAsStream));
        }
        // 连接服务器
        mqttClient.connect(options);
    }

    @Override
    public void publish(MqttMessageEntity message) throws Exception {
        MqttProperties mqttProperties = new MqttProperties();
        mqttProperties.setPayloadFormat(message.getPayloadFormat());
        mqttProperties.setContentType(message.getContentType());
        mqttProperties.setMessageExpiryInterval(message.getMessageExpiryInterval());
        mqttProperties.setTopicAlias(message.getTopicAlias());
        mqttProperties.setResponseTopic(message.getResponseTopic());
        mqttProperties.setCorrelationData(message.getCorrelationData().getBytes(StandardCharsets.UTF_8));
        mqttProperties.setSubscriptionIdentifiers(message.getSubscriptionIdentifiers());

        Map<String, String> userProperties = message.getUserProperties();
        if (null != userProperties && userProperties.size() > 0) {
            List<UserProperty> userPropertieList = new ArrayList<>();
            userProperties.forEach((key, value) -> userPropertieList.add(new UserProperty(key, value)));
            mqttProperties.setUserProperties(userPropertieList);
        }

        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(message.getPayload());
        mqttMessage.setQos(message.getQos());
        mqttMessage.setRetained(message.getRetain());
        mqttMessage.setProperties(mqttProperties);

        mqttClient.publish(message.getTopic(), mqttMessage);
    }

    @Override
    public void subscribe(MqttSubParam[] subParams) throws Exception {
        MqttSubscription[] mqttSubscriptions = Arrays.stream(subParams)
                .map(subParam -> {
                    MqttSubscription mqttSubscription = new MqttSubscription(subParam.getTopic(), subParam.getQos());
                    mqttSubscription.setNoLocal(subParam.getNoLocal());
                    mqttSubscription.setRetainAsPublished(subParam.getRetainAsPublished());
                    mqttSubscription.setRetainHandling(subParam.getRetainHandling());
                    //缺少 订阅时指定订阅标识符 paho不支持
                    return mqttSubscription;
                }).toArray(MqttSubscription[]::new);
        mqttClient.subscribe(mqttSubscriptions);
    }

    @Override
    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    @Override
    public void disconnect() throws Exception {
        mqttClient.disconnect();
    }

    @Override
    public void close() throws Exception {
        mqttClient.close();
    }

    @Override
    public void setCallback(IMqttCallback mqttCallback) {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
                mqttCallback.exceptionOccurred(mqttDisconnectResponse.getReasonString());
            }

            @Override
            public void mqttErrorOccurred(MqttException e) {
                mqttCallback.exceptionOccurred(e.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                MqttProperties properties = message.getProperties();

                MqttMessageEntity mqttMessageEntity = new MqttMessageEntity();
                mqttMessageEntity.setTopic(topic);
                mqttMessageEntity.setQos(message.getQos());
                mqttMessageEntity.setPayload(message.getPayload());
                mqttMessageEntity.setRetain(message.isRetained());
                mqttMessageEntity.setContentType(properties.getContentType());
                mqttMessageEntity.setSubscriptionIdentifiers(properties.getSubscriptionIdentifiers());
                mqttMessageEntity.setResponseTopic(properties.getResponseTopic());

                byte[] correlationData = properties.getCorrelationData();
                if (null != correlationData) {
                    mqttMessageEntity.setCorrelationData(new String(correlationData, StandardCharsets.UTF_8));
                }

                HashMap<String, String> userProperties = new HashMap<>();
                properties.getUserProperties().forEach(userProperty -> userProperties.put(userProperty.getKey(), userProperty.getValue()));
                mqttMessageEntity.setUserProperties(userProperties);

                mqttCallback.messageArrived(mqttMessageEntity);
            }

            @Override
            public void deliveryComplete(IMqttToken iMqttToken) {

            }

            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void authPacketArrived(int i, MqttProperties mqttProperties) {

            }
        });
    }
}
