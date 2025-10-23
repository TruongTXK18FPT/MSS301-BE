package com.mss301.ragservice.client;

import com.mss301.ragservice.config.FptTtsConfig;
import com.mss301.ragservice.dto.external.FPTTtsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "fptTtsClient",
        url = "${fptai.url}",
        configuration = FptTtsConfig.class
)
@Service
public interface FPTTtsClient {

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    FPTTtsResponse synthesize(
            @RequestHeader("api_key") String apiKey,
            @RequestHeader(value = "voice", required = false) String voice,
            @RequestHeader(value = "speed", required = false) String speed,
            @RequestHeader(value = "format", required = false) String format,
            @RequestHeader(value = "callback_url", required = false) String callbackUrl,
            @RequestBody String text
    );
}
