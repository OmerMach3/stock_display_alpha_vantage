package com.finansal.finansal_deneme.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class AlphaVantageMonthlyResponseDto {

    @JsonProperty("Meta Data")
    private MetaDataDto metaData;

    @JsonProperty("Monthly Time Series")
    private Map<String, TimeSeriesEntryDto> monthlyTimeSeries;
}