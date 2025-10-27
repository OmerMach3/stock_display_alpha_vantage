package com.finansal.finansal_deneme.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class AlphaVantageDailyResponseDto {
    @JsonProperty("Meta Data")
    private MetaDataDto metaData;

    @JsonProperty("Time Series (Daily)")
    private Map<String, TimeSeriesEntryDto> dailyTimeSeries;

    public MetaDataDto getMetaData() {
        return metaData;
    }

    public Map<String, TimeSeriesEntryDto> getDailyTimeSeries() {
        return dailyTimeSeries;
    }
}
