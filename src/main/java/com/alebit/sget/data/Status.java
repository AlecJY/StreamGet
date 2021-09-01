package com.alebit.sget.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class Status {
    @Getter
    @Setter
    @JsonProperty("SHA256")
    private byte[] sha256;

    @Getter
    @Setter
    @JsonProperty("Progress")
    private int progress;

    @Getter
    @Setter
    @Accessors(fluent = true)
    @JsonProperty("Key")
    private boolean hasKey;
}
