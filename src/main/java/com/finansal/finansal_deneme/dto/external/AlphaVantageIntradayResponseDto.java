package com.finansal.finansal_deneme.dto.external;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlphaVantageIntradayResponseDto {
@JsonProperty("Meta Data")
private MetaDataDto metaData;
@JsonProperty("Time Series (5min)")
private Map<String, TimeSeriesEntryDto> timeSeries;


public MetaDataDto getMetaData() {
    return metaData;
}

public Map<String, TimeSeriesEntryDto> getTimeSeries() {
    return timeSeries;
}
}