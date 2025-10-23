package com.mss301.ragservice.strategy;

import com.mss301.ragservice.enums.ResponseMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseStrategyFactory {

    private final Map<ResponseMode, ResponseStrategy> strategies;

    public ResponseStrategy getStrategy(ResponseMode mode) {
        log.debug("Getting response strategy for mode: {}", mode);

        ResponseStrategy strategy = strategies.get(mode);

        if (strategy == null) {
            log.error("Unsupported response mode: {}", mode);
            throw new IllegalArgumentException("Unsupported response mode: " + mode);
        }

        log.debug("Using strategy: {}", strategy.getClass().getSimpleName());
        return strategy;
    }
}
