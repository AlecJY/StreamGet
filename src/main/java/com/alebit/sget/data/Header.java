package com.alebit.sget.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class Header {
    @Getter
    @Setter
    @JsonProperty("name")
    private String name;

    @Getter
    @Setter
    @JsonProperty("value")
    private String value;
}
