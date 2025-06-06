package com.leon.datalink.runtime.entity;

import com.google.common.collect.Lists;
import com.leon.datalink.core.evn.EnvUtil;
import com.leon.datalink.runtime.constants.RuntimeStatusEnum;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 运行时资源或转换
 */
public class RuntimeEntity implements Serializable {

    // 当前状态（初始化、正常、异常）
    private RuntimeStatusEnum status;

    // 成功统计
    private long successCount;

    // 失败统计
    private long failCount;

    // 每个节点上的统计 key为memberName
    private final Map<String, RuntimeMember> runtimeMemberList;

    private final LinkedList<RuntimeData> runtimeDataList;

    public RuntimeEntity() {
        status = RuntimeStatusEnum.INIT;
        runtimeDataList = Lists.newLinkedList();
        runtimeMemberList = new HashMap<>();
    }

    public void addDataRecord(RuntimeData runtimeData) {
        this.runtimeDataList.addFirst(runtimeData);
        if (this.runtimeDataList.size() > EnvUtil.getRuntimeRecordLimit()) {
            this.runtimeDataList.removeLast();
        }
        if (runtimeData.isError()) {
            failCount++;
        } else {
            successCount++;
        }

        String memberName = runtimeData.getMemberName();
        RuntimeMember runtimeMember = runtimeMemberList.get(memberName);
        if (null == runtimeMember) {
            runtimeMember = new RuntimeMember(memberName);
            runtimeMember.addDataRecord(runtimeData);
            runtimeMemberList.put(memberName, runtimeMember);
        } else {
            runtimeMember.addDataRecord(runtimeData);
        }
    }

    public void updateStatus(RuntimeStatus runtimeStatus) {
        this.status = runtimeStatus.getStatus();

        String memberName = runtimeStatus.getMemberName();
        RuntimeMember runtimeMember = runtimeMemberList.get(memberName);
        if (null == runtimeMember) {
            runtimeMember = new RuntimeMember(memberName);
            runtimeMember.updateStatus(runtimeStatus);
            runtimeMemberList.put(memberName, runtimeMember);
        } else {
            runtimeMember.updateStatus(runtimeStatus);
        }
    }

    public void initStatus() {
        this.status = RuntimeStatusEnum.INIT;
        runtimeMemberList.values().forEach(runtimeMember -> runtimeMember.setStatus(RuntimeStatusEnum.INIT));
    }

    public RuntimeStatusEnum getStatus() {
        return status;
    }

    public void setStatus(RuntimeStatusEnum status) {
        this.status = status;
    }

    public LinkedList<RuntimeData> getRuntimeDataList() {
        return runtimeDataList;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailCount() {
        return failCount;
    }

    public void setFailCount(long failedCount) {
        this.failCount = failedCount;
    }

    public Map<String, RuntimeMember> getRuntimeMemberList() {
        return runtimeMemberList;
    }
}
