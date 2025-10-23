package com.mss301.ragservice.service.impl;

import com.mss301.ragservice.client.FPTTtsClient;
import com.mss301.ragservice.dto.external.FPTTtsResponse;
import com.mss301.ragservice.dto.request.TtsRequest;
import com.mss301.ragservice.dto.response.TtsResponse;
import com.mss301.ragservice.service.TtsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TtsServiceImpl implements TtsService {

    @Autowired
    private FPTTtsClient fptTtsClient;

    @Value("${fptai.api-key}")
    private String FPTAI_API_KEY;

    @Override
    public TtsResponse speak(TtsRequest request) {

        TtsResponse response = new TtsResponse();


        if (request.getText() == null || request.getText().trim().length() < 3) {
            response.setSuccess(Boolean.FALSE);
            response.setMessage("Text must be at least 3 characters and not null.");
            return response;
        }


        try {
            FPTTtsResponse fptTtsResponse = fptTtsClient.synthesize(
                    FPTAI_API_KEY,
                    "banmai", // voice
                    "0",      // speed
                    "mp3",    // format
                    null,     // callback_url
                    request.getText() // text body
            );
            response.setSuccess(Boolean.TRUE);
            response.setMessage(fptTtsResponse.getMessage());
            response.setAudioUrl(fptTtsResponse.getAsync());
            return response;
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            response.setSuccess(Boolean.FALSE);
            return response;
        }
    }
}
