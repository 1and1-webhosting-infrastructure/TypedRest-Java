package com.oneandone.typedrest.sample.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.*;

/**
 * A single log event.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEvent {

    /**
     * Indicates when this event occurred.
     */
    private LocalDateTime timestamp;
    
    /**
     * A human-readable message describing what happened.
     */
    private String message;
}