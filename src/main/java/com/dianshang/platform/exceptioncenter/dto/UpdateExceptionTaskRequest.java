package com.dianshang.platform.exceptioncenter.dto;

public record UpdateExceptionTaskRequest(
        String action,
        String operatorId,
        String remark
) {
}
