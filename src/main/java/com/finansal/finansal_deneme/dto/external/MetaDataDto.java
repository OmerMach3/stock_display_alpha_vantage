package com.finansal.finansal_deneme.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MetaDataDto {
@JsonProperty("1. Information")
private String information;
@JsonProperty("2. Symbol")
private String symbol;
@JsonProperty("3. Last Refreshed")
private String lastRefreshed;
@JsonProperty("4. Time Zone")
private String timeZone;
    
}
