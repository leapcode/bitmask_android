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
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ModelsBridge
 */

public class ModelsBridge {
  @SerializedName("auth")
  private String auth = null;

  @SerializedName("bucket")
  private String bucket = null;

  @SerializedName("experimental")
  private Boolean experimental = null;

  @SerializedName("healthy")
  private Boolean healthy = null;

  @SerializedName("host")
  private String host = null;

  @SerializedName("ip_addr")
  private String ipAddr = null;

  @SerializedName("ip6_addr")
  private String ip6Addr = null;

  @SerializedName("load")
  private BigDecimal load = null;

  @SerializedName("location")
  private String location = null;

  @SerializedName("options")
  private Map<String, Object> options = null;

  @SerializedName("overloaded")
  private Boolean overloaded = null;

  @SerializedName("port")
  private Integer port = null;

  @SerializedName("transport")
  private String transport = null;

  @SerializedName("type")
  private String type = null;

  public ModelsBridge auth(String auth) {
    this.auth = auth;
    return this;
  }

   /**
   * Any authentication method needed for connect to the bridge, &#x60;none&#x60; otherwise.
   * @return auth
  **/
  @ApiModelProperty(value = "Any authentication method needed for connect to the bridge, `none` otherwise.")
  public String getAuth() {
    return auth;
  }

  public void setAuth(String auth) {
    this.auth = auth;
  }

  public ModelsBridge bucket(String bucket) {
    this.bucket = bucket;
    return this;
  }

   /**
   * Bucket is a \&quot;bucket\&quot; tag that connotes a resource group that a user may or may not have access to. An empty bucket string implies that it is open access
   * @return bucket
  **/
  @ApiModelProperty(value = "Bucket is a \"bucket\" tag that connotes a resource group that a user may or may not have access to. An empty bucket string implies that it is open access")
  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public ModelsBridge experimental(Boolean experimental) {
    this.experimental = experimental;
    return this;
  }

   /**
   * An experimental bridge flags any bridge that, for whatever reason, is not deemed stable. The expectation is that clients have to opt-in to experimental bridges (and gateways too).
   * @return experimental
  **/
  @ApiModelProperty(value = "An experimental bridge flags any bridge that, for whatever reason, is not deemed stable. The expectation is that clients have to opt-in to experimental bridges (and gateways too).")
  public Boolean isExperimental() {
    return experimental;
  }

  public void setExperimental(Boolean experimental) {
    this.experimental = experimental;
  }

  public ModelsBridge healthy(Boolean healthy) {
    this.healthy = healthy;
    return this;
  }

   /**
   * Healthy indicates whether this bridge can be used normally.
   * @return healthy
  **/
  @ApiModelProperty(value = "Healthy indicates whether this bridge can be used normally.")
  public Boolean isHealthy() {
    return healthy;
  }

  public void setHealthy(Boolean healthy) {
    this.healthy = healthy;
  }

  public ModelsBridge host(String host) {
    this.host = host;
    return this;
  }

   /**
   * Host is a unique identifier for the bridge.
   * @return host
  **/
  @ApiModelProperty(value = "Host is a unique identifier for the bridge.")
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public ModelsBridge ipAddr(String ipAddr) {
    this.ipAddr = ipAddr;
    return this;
  }

   /**
   * IPAddr is the IPv4 address
   * @return ipAddr
  **/
  @ApiModelProperty(value = "IPAddr is the IPv4 address")
  public String getIpAddr() {
    return ipAddr;
  }

  public void setIpAddr(String ipAddr) {
    this.ipAddr = ipAddr;
  }

  public ModelsBridge ip6Addr(String ip6Addr) {
    this.ip6Addr = ip6Addr;
    return this;
  }

   /**
   * IP6Addr is the IPv6 address
   * @return ip6Addr
  **/
  @ApiModelProperty(value = "IP6Addr is the IPv6 address")
  public String getIp6Addr() {
    return ip6Addr;
  }

  public void setIp6Addr(String ip6Addr) {
    this.ip6Addr = ip6Addr;
  }

  public ModelsBridge load(BigDecimal load) {
    this.load = load;
    return this;
  }

   /**
   * Load is the fractional load - but for now menshen agent is not measuring load in the bridges.
   * @return load
  **/
  @ApiModelProperty(value = "Load is the fractional load - but for now menshen agent is not measuring load in the bridges.")
  public BigDecimal getLoad() {
    return load;
  }

  public void setLoad(BigDecimal load) {
    this.load = load;
  }

  public ModelsBridge location(String location) {
    this.location = location;
    return this;
  }

   /**
   * Location refers to the location to which this bridge points to
   * @return location
  **/
  @ApiModelProperty(value = "Location refers to the location to which this bridge points to")
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public ModelsBridge options(Map<String, Object> options) {
    this.options = options;
    return this;
  }

  public ModelsBridge putOptionsItem(String key, Object optionsItem) {
    if (this.options == null) {
      this.options = new HashMap<String, Object>();
    }
    this.options.put(key, optionsItem);
    return this;
  }

   /**
   * Options contain the map of options that will be passed to the client. It usually contains authentication credentials.
   * @return options
  **/
  @ApiModelProperty(value = "Options contain the map of options that will be passed to the client. It usually contains authentication credentials.")
  public Map<String, Object> getOptions() {
    return options;
  }

  public void setOptions(Map<String, Object> options) {
    this.options = options;
  }

  public ModelsBridge overloaded(Boolean overloaded) {
    this.overloaded = overloaded;
    return this;
  }

   /**
   * Overloaded should be set to true if the fractional load is above threshold.
   * @return overloaded
  **/
  @ApiModelProperty(value = "Overloaded should be set to true if the fractional load is above threshold.")
  public Boolean isOverloaded() {
    return overloaded;
  }

  public void setOverloaded(Boolean overloaded) {
    this.overloaded = overloaded;
  }

  public ModelsBridge port(Integer port) {
    this.port = port;
    return this;
  }

   /**
   * For some protocols (like hopping) port is undefined.
   * @return port
  **/
  @ApiModelProperty(value = "For some protocols (like hopping) port is undefined.")
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public ModelsBridge transport(String transport) {
    this.transport = transport;
    return this;
  }

   /**
   * TCP, UDP or KCP. This was called \&quot;protocol\&quot; before.
   * @return transport
  **/
  @ApiModelProperty(value = "TCP, UDP or KCP. This was called \"protocol\" before.")
  public String getTransport() {
    return transport;
  }

  public void setTransport(String transport) {
    this.transport = transport;
  }

  public ModelsBridge type(String type) {
    this.type = type;
    return this;
  }

   /**
   * Type of bridge.
   * @return type
  **/
  @ApiModelProperty(value = "Type of bridge.")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModelsBridge modelsBridge = (ModelsBridge) o;
    return Objects.equals(this.auth, modelsBridge.auth) &&
        Objects.equals(this.bucket, modelsBridge.bucket) &&
        Objects.equals(this.experimental, modelsBridge.experimental) &&
        Objects.equals(this.healthy, modelsBridge.healthy) &&
        Objects.equals(this.host, modelsBridge.host) &&
        Objects.equals(this.ipAddr, modelsBridge.ipAddr) &&
        Objects.equals(this.ip6Addr, modelsBridge.ip6Addr) &&
        Objects.equals(this.load, modelsBridge.load) &&
        Objects.equals(this.location, modelsBridge.location) &&
        Objects.equals(this.options, modelsBridge.options) &&
        Objects.equals(this.overloaded, modelsBridge.overloaded) &&
        Objects.equals(this.port, modelsBridge.port) &&
        Objects.equals(this.transport, modelsBridge.transport) &&
        Objects.equals(this.type, modelsBridge.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(auth, bucket, experimental, healthy, host, ipAddr, ip6Addr, load, location, options, overloaded, port, transport, type);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ModelsBridge {\n");
    
    sb.append("    auth: ").append(toIndentedString(auth)).append("\n");
    sb.append("    bucket: ").append(toIndentedString(bucket)).append("\n");
    sb.append("    experimental: ").append(toIndentedString(experimental)).append("\n");
    sb.append("    healthy: ").append(toIndentedString(healthy)).append("\n");
    sb.append("    host: ").append(toIndentedString(host)).append("\n");
    sb.append("    ipAddr: ").append(toIndentedString(ipAddr)).append("\n");
    sb.append("    ip6Addr: ").append(toIndentedString(ip6Addr)).append("\n");
    sb.append("    load: ").append(toIndentedString(load)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
    sb.append("    overloaded: ").append(toIndentedString(overloaded)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    transport: ").append(toIndentedString(transport)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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
