/*
 * Menshen API
 * This is a LEAP VPN Service API
 *
 * OpenAPI spec version: 0.5.2
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * ModelsLocation
 */

public class ModelsLocation {
  @SerializedName("country_code")
  private String countryCode = null;

  @SerializedName("display_name")
  private String displayName = null;

  @SerializedName("has_bridges")
  private Boolean hasBridges = null;

  @SerializedName("healthy")
  private Boolean healthy = null;

  @SerializedName("hemisphere")
  private String hemisphere = null;

  @SerializedName("label")
  private String label = null;

  @SerializedName("lat")
  private String lat = null;

  @SerializedName("lon")
  private String lon = null;

  @SerializedName("region")
  private String region = null;

  @SerializedName("timezone")
  private String timezone = null;

  public ModelsLocation countryCode(String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

   /**
   * CountryCode is the two-character country ISO identifier (uppercase).
   * @return countryCode
  **/
  @ApiModelProperty(value = "CountryCode is the two-character country ISO identifier (uppercase).")
  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public ModelsLocation displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

   /**
   * DisplayName is the user-facing string for a given location.
   * @return displayName
  **/
  @ApiModelProperty(value = "DisplayName is the user-facing string for a given location.")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ModelsLocation hasBridges(Boolean hasBridges) {
    this.hasBridges = hasBridges;
    return this;
  }

   /**
   * Any location that has at least one bridge configured will set this to true.
   * @return hasBridges
  **/
  @ApiModelProperty(value = "Any location that has at least one bridge configured will set this to true.")
  public Boolean isHasBridges() {
    return hasBridges;
  }

  public void setHasBridges(Boolean hasBridges) {
    this.hasBridges = hasBridges;
  }

  public ModelsLocation healthy(Boolean healthy) {
    this.healthy = healthy;
    return this;
  }

   /**
   * TODO Not used right now, but intended to signal when a location has all of their nodes overwhelmed.
   * @return healthy
  **/
  @ApiModelProperty(value = "TODO Not used right now, but intended to signal when a location has all of their nodes overwhelmed.")
  public Boolean isHealthy() {
    return healthy;
  }

  public void setHealthy(Boolean healthy) {
    this.healthy = healthy;
  }

  public ModelsLocation hemisphere(String hemisphere) {
    this.hemisphere = hemisphere;
    return this;
  }

   /**
   * Hemisphere is a legacy label for a gateway. The rationale was once intended to be to allocate gateways for an hemisphere with certain regional \&quot;fairness\&quot;, even if they&#39;re geographically located in a different region. We might want to set this on the Gateway or Bridge, not in the Location itself...
   * @return hemisphere
  **/
  @ApiModelProperty(value = "Hemisphere is a legacy label for a gateway. The rationale was once intended to be to allocate gateways for an hemisphere with certain regional \"fairness\", even if they're geographically located in a different region. We might want to set this on the Gateway or Bridge, not in the Location itself...")
  public String getHemisphere() {
    return hemisphere;
  }

  public void setHemisphere(String hemisphere) {
    this.hemisphere = hemisphere;
  }

  public ModelsLocation label(String label) {
    this.label = label;
    return this;
  }

   /**
   * Label is the short representation of a location, used internally.
   * @return label
  **/
  @ApiModelProperty(value = "Label is the short representation of a location, used internally.")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public ModelsLocation lat(String lat) {
    this.lat = lat;
    return this;
  }

   /**
   * Lat is the latitude for the location.
   * @return lat
  **/
  @ApiModelProperty(value = "Lat is the latitude for the location.")
  public String getLat() {
    return lat;
  }

  public void setLat(String lat) {
    this.lat = lat;
  }

  public ModelsLocation lon(String lon) {
    this.lon = lon;
    return this;
  }

   /**
   * Lon is the longitude for the location.
   * @return lon
  **/
  @ApiModelProperty(value = "Lon is the longitude for the location.")
  public String getLon() {
    return lon;
  }

  public void setLon(String lon) {
    this.lon = lon;
  }

  public ModelsLocation region(String region) {
    this.region = region;
    return this;
  }

   /**
   * Region is the continental region this gateway is assigned to. Not used at the moment, intended to use a label from the 7-continent model.
   * @return region
  **/
  @ApiModelProperty(value = "Region is the continental region this gateway is assigned to. Not used at the moment, intended to use a label from the 7-continent model.")
  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public ModelsLocation timezone(String timezone) {
    this.timezone = timezone;
    return this;
  }

   /**
   * Timezone is the TZ for the location (-1, 0, +1, ...)
   * @return timezone
  **/
  @ApiModelProperty(value = "Timezone is the TZ for the location (-1, 0, +1, ...)")
  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModelsLocation modelsLocation = (ModelsLocation) o;
    return Objects.equals(this.countryCode, modelsLocation.countryCode) &&
        Objects.equals(this.displayName, modelsLocation.displayName) &&
        Objects.equals(this.hasBridges, modelsLocation.hasBridges) &&
        Objects.equals(this.healthy, modelsLocation.healthy) &&
        Objects.equals(this.hemisphere, modelsLocation.hemisphere) &&
        Objects.equals(this.label, modelsLocation.label) &&
        Objects.equals(this.lat, modelsLocation.lat) &&
        Objects.equals(this.lon, modelsLocation.lon) &&
        Objects.equals(this.region, modelsLocation.region) &&
        Objects.equals(this.timezone, modelsLocation.timezone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(countryCode, displayName, hasBridges, healthy, hemisphere, label, lat, lon, region, timezone);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ModelsLocation {\n");
    
    sb.append("    countryCode: ").append(toIndentedString(countryCode)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    hasBridges: ").append(toIndentedString(hasBridges)).append("\n");
    sb.append("    healthy: ").append(toIndentedString(healthy)).append("\n");
    sb.append("    hemisphere: ").append(toIndentedString(hemisphere)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
    sb.append("    lon: ").append(toIndentedString(lon)).append("\n");
    sb.append("    region: ").append(toIndentedString(region)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
