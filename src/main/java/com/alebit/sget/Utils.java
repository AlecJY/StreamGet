package com.alebit.sget;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

public class Utils {
    @Getter
    private static ObjectMapper jsonObjectMapper = new ObjectMapper(new JsonFactory());
}
