package com.finansal.finansal_deneme.dto.external;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
@Data
public class TimeSeriesEntryDto {

@JsonProperty("1. open")
private BigDecimal open;
@JsonProperty("2. high")
private BigDecimal high;
@JsonProperty("3. low")
private BigDecimal low;
@JsonProperty("4. close")
private BigDecimal close;
@JsonProperty("5. volume")
private Long volume;

}
