package com.dianshang.platform.openplatform.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService.ReceiveOpenCallbackCommand;
import com.dianshang.platform.openplatform.model.OpenCallbackReceiveResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenPlatformCallbackController {

    private final OpenPlatformApplicationService openPlatformApplicationService;

    public OpenPlatformCallbackController(OpenPlatformApplicationService openPlatformApplicationService) {
        this.openPlatformApplicationService = openPlatformApplicationService;
    }

    @PostMapping(value = "/api/open/callbacks/{id}", consumes = MediaType.ALL_VALUE)
    public ApiResponse<OpenCallbackReceiveResult> receiveCallback(@PathVariable String id,
                                                                  @RequestHeader("X-Open-Request-Id") String requestId,
                                                                  @RequestHeader("X-Open-Timestamp") String timestamp,
                                                                  @RequestHeader("X-Open-Nonce") String nonce,
                                                                  @RequestHeader("X-Open-Signature") String signature,
                                                                  @RequestBody(required = false) String payload) {
        return ApiResponse.success(
                openPlatformApplicationService.receiveCallback(id, new ReceiveOpenCallbackCommand(
                        requestId,
                        timestamp,
                        nonce,
                        signature,
                        payload == null ? "" : payload,
                        "/api/open/callbacks/" + id
                )),
                TraceIdHolder.get()
        );
    }
}
