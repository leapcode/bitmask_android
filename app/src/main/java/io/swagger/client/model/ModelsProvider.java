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
import io.swagger.client.model.ModelsProviderService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ModelsProvider
 */

public class ModelsProvider {
  @SerializedName("api_uri")
  private String apiUri = null;

  @SerializedName("api_version")
  private String apiVersion = null;

  @SerializedName("api_versions")
  private List<String> apiVersions = null;

  @SerializedName("ask_for_donations")
  private Boolean askForDonations = null;

  @SerializedName("ca_cert_fingerprint")
  private String caCertFingerprint = null;

  @SerializedName("ca_cert_uri")
  private String caCertUri = null;

  @SerializedName("country_code_lookup_url")
  private String countryCodeLookupUrl = null;

  @SerializedName("default_language")
  private String defaultLanguage = null;

  @SerializedName("description")
  private Map<String, String> description = null;

  @SerializedName("domain")
  private String domain = null;

  @SerializedName("donate_period")
  private String donatePeriod = null;

  @SerializedName("donate_url")
  private String donateUrl = null;

  @SerializedName("info_url")
  private String infoUrl = null;

  @SerializedName("languages")
  private List<String> languages = null;

  @SerializedName("motd_url")
  private String motdUrl = null;

  @SerializedName("name")
  private Map<String, String> name = null;

  @SerializedName("service")
  private ModelsProviderService service = null;

  @SerializedName("services")
  private List<String> services = null;

  @SerializedName("stun_servers")
  private List<String> stunServers = null;

  @SerializedName("tos_url")
  private String tosUrl = null;

  public ModelsProvider apiUri(String apiUri) {
    this.apiUri = apiUri;
    return this;
  }

   /**
   * URL of the API endpoints
   * @return apiUri
  **/
  @ApiModelProperty(value = "URL of the API endpoints")
  public String getApiUri() {
    return apiUri;
  }

  public void setApiUri(String apiUri) {
    this.apiUri = apiUri;
  }

  public ModelsProvider apiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
    return this;
  }

   /**
   * oldest supported api version deprecated: kept for backwards compatibility. Replaced by api_versions.
   * @return apiVersion
  **/
  @ApiModelProperty(value = "oldest supported api version deprecated: kept for backwards compatibility. Replaced by api_versions.")
  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public ModelsProvider apiVersions(List<String> apiVersions) {
    this.apiVersions = apiVersions;
    return this;
  }

  public ModelsProvider addApiVersionsItem(String apiVersionsItem) {
    if (this.apiVersions == null) {
      this.apiVersions = new ArrayList<String>();
    }
    this.apiVersions.add(apiVersionsItem);
    return this;
  }

   /**
   * all API versions the provider supports
   * @return apiVersions
  **/
  @ApiModelProperty(value = "all API versions the provider supports")
  public List<String> getApiVersions() {
    return apiVersions;
  }

  public void setApiVersions(List<String> apiVersions) {
    this.apiVersions = apiVersions;
  }

  public ModelsProvider askForDonations(Boolean askForDonations) {
    this.askForDonations = askForDonations;
    return this;
  }

   /**
   * Flag indicating whether to show regularly a donation reminder
   * @return askForDonations
  **/
  @ApiModelProperty(value = "Flag indicating whether to show regularly a donation reminder")
  public Boolean isAskForDonations() {
    return askForDonations;
  }

  public void setAskForDonations(Boolean askForDonations) {
    this.askForDonations = askForDonations;
  }

  public ModelsProvider caCertFingerprint(String caCertFingerprint) {
    this.caCertFingerprint = caCertFingerprint;
    return this;
  }

   /**
   * fingerprint of CA cert used to setup TLS sessions during VPN setup (and up to API version 3 for API communication) deprecated: kept for backwards compatibility
   * @return caCertFingerprint
  **/
  @ApiModelProperty(value = "fingerprint of CA cert used to setup TLS sessions during VPN setup (and up to API version 3 for API communication) deprecated: kept for backwards compatibility")
  public String getCaCertFingerprint() {
    return caCertFingerprint;
  }

  public void setCaCertFingerprint(String caCertFingerprint) {
    this.caCertFingerprint = caCertFingerprint;
  }

  public ModelsProvider caCertUri(String caCertUri) {
    this.caCertUri = caCertUri;
    return this;
  }

   /**
   * URL to fetch the CA cert used to setup TLS sessions during VPN setup (and up to API version 3 for API communication) deprecated: kept for backwards compatibility
   * @return caCertUri
  **/
  @ApiModelProperty(value = "URL to fetch the CA cert used to setup TLS sessions during VPN setup (and up to API version 3 for API communication) deprecated: kept for backwards compatibility")
  public String getCaCertUri() {
    return caCertUri;
  }

  public void setCaCertUri(String caCertUri) {
    this.caCertUri = caCertUri;
  }

  public ModelsProvider countryCodeLookupUrl(String countryCodeLookupUrl) {
    this.countryCodeLookupUrl = countryCodeLookupUrl;
    return this;
  }

   /**
   * URL of a service that returns a country code for an ip address. If empty, OONI backend is used
   * @return countryCodeLookupUrl
  **/
  @ApiModelProperty(value = "URL of a service that returns a country code for an ip address. If empty, OONI backend is used")
  public String getCountryCodeLookupUrl() {
    return countryCodeLookupUrl;
  }

  public void setCountryCodeLookupUrl(String countryCodeLookupUrl) {
    this.countryCodeLookupUrl = countryCodeLookupUrl;
  }

  public ModelsProvider defaultLanguage(String defaultLanguage) {
    this.defaultLanguage = defaultLanguage;
    return this;
  }

   /**
   * Default language this provider uses to show infos and provider messages
   * @return defaultLanguage
  **/
  @ApiModelProperty(value = "Default language this provider uses to show infos and provider messages")
  public String getDefaultLanguage() {
    return defaultLanguage;
  }

  public void setDefaultLanguage(String defaultLanguage) {
    this.defaultLanguage = defaultLanguage;
  }

  public ModelsProvider description(Map<String, String> description) {
    this.description = description;
    return this;
  }

  public ModelsProvider putDescriptionItem(String key, String descriptionItem) {
    if (this.description == null) {
      this.description = new HashMap<String, String>();
    }
    this.description.put(key, descriptionItem);
    return this;
  }

   /**
   * Short description about the provider
   * @return description
  **/
  @ApiModelProperty(value = "Short description about the provider")
  public Map<String, String> getDescription() {
    return description;
  }

  public void setDescription(Map<String, String> description) {
    this.description = description;
  }

  public ModelsProvider domain(String domain) {
    this.domain = domain;
    return this;
  }

   /**
   * Domain of the provider
   * @return domain
  **/
  @ApiModelProperty(value = "Domain of the provider")
  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public ModelsProvider donatePeriod(String donatePeriod) {
    this.donatePeriod = donatePeriod;
    return this;
  }

   /**
   * Number of days until a donation reminder reappears
   * @return donatePeriod
  **/
  @ApiModelProperty(value = "Number of days until a donation reminder reappears")
  public String getDonatePeriod() {
    return donatePeriod;
  }

  public void setDonatePeriod(String donatePeriod) {
    this.donatePeriod = donatePeriod;
  }

  public ModelsProvider donateUrl(String donateUrl) {
    this.donateUrl = donateUrl;
    return this;
  }

   /**
   * URL to the donation website
   * @return donateUrl
  **/
  @ApiModelProperty(value = "URL to the donation website")
  public String getDonateUrl() {
    return donateUrl;
  }

  public void setDonateUrl(String donateUrl) {
    this.donateUrl = donateUrl;
  }

  public ModelsProvider infoUrl(String infoUrl) {
    this.infoUrl = infoUrl;
    return this;
  }

   /**
   * URL to general provider website
   * @return infoUrl
  **/
  @ApiModelProperty(value = "URL to general provider website")
  public String getInfoUrl() {
    return infoUrl;
  }

  public void setInfoUrl(String infoUrl) {
    this.infoUrl = infoUrl;
  }

  public ModelsProvider languages(List<String> languages) {
    this.languages = languages;
    return this;
  }

  public ModelsProvider addLanguagesItem(String languagesItem) {
    if (this.languages == null) {
      this.languages = new ArrayList<String>();
    }
    this.languages.add(languagesItem);
    return this;
  }

   /**
   * Languages the provider supports to show infos and provider messages
   * @return languages
  **/
  @ApiModelProperty(value = "Languages the provider supports to show infos and provider messages")
  public List<String> getLanguages() {
    return languages;
  }

  public void setLanguages(List<String> languages) {
    this.languages = languages;
  }

  public ModelsProvider motdUrl(String motdUrl) {
    this.motdUrl = motdUrl;
    return this;
  }

   /**
   * URL to the message of the day service
   * @return motdUrl
  **/
  @ApiModelProperty(value = "URL to the message of the day service")
  public String getMotdUrl() {
    return motdUrl;
  }

  public void setMotdUrl(String motdUrl) {
    this.motdUrl = motdUrl;
  }

  public ModelsProvider name(Map<String, String> name) {
    this.name = name;
    return this;
  }

  public ModelsProvider putNameItem(String key, String nameItem) {
    if (this.name == null) {
      this.name = new HashMap<String, String>();
    }
    this.name.put(key, nameItem);
    return this;
  }

   /**
   * Provider name
   * @return name
  **/
  @ApiModelProperty(value = "Provider name")
  public Map<String, String> getName() {
    return name;
  }

  public void setName(Map<String, String> name) {
    this.name = name;
  }

  public ModelsProvider service(ModelsProviderService service) {
    this.service = service;
    return this;
  }

   /**
   * Get service
   * @return service
  **/
  @ApiModelProperty(value = "")
  public ModelsProviderService getService() {
    return service;
  }

  public void setService(ModelsProviderService service) {
    this.service = service;
  }

  public ModelsProvider services(List<String> services) {
    this.services = services;
    return this;
  }

  public ModelsProvider addServicesItem(String servicesItem) {
    if (this.services == null) {
      this.services = new ArrayList<String>();
    }
    this.services.add(servicesItem);
    return this;
  }

   /**
   * List of services the provider offers, currently only openvpn
   * @return services
  **/
  @ApiModelProperty(value = "List of services the provider offers, currently only openvpn")
  public List<String> getServices() {
    return services;
  }

  public void setServices(List<String> services) {
    this.services = services;
  }

  public ModelsProvider stunServers(List<String> stunServers) {
    this.stunServers = stunServers;
    return this;
  }

  public ModelsProvider addStunServersItem(String stunServersItem) {
    if (this.stunServers == null) {
      this.stunServers = new ArrayList<String>();
    }
    this.stunServers.add(stunServersItem);
    return this;
  }

   /**
   * list of STUN servers (format: ip/hostname:port) servers to get current ip address can consist of self hosted STUN servers, public ones or a combination of both. GeolocationLookup is only done when the list of STUNServers is not empty
   * @return stunServers
  **/
  @ApiModelProperty(value = "list of STUN servers (format: ip/hostname:port) servers to get current ip address can consist of self hosted STUN servers, public ones or a combination of both. GeolocationLookup is only done when the list of STUNServers is not empty")
  public List<String> getStunServers() {
    return stunServers;
  }

  public void setStunServers(List<String> stunServers) {
    this.stunServers = stunServers;
  }

  public ModelsProvider tosUrl(String tosUrl) {
    this.tosUrl = tosUrl;
    return this;
  }

   /**
   * URL to Terms of Service website
   * @return tosUrl
  **/
  @ApiModelProperty(value = "URL to Terms of Service website")
  public String getTosUrl() {
    return tosUrl;
  }

  public void setTosUrl(String tosUrl) {
    this.tosUrl = tosUrl;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModelsProvider modelsProvider = (ModelsProvider) o;
    return Objects.equals(this.apiUri, modelsProvider.apiUri) &&
        Objects.equals(this.apiVersion, modelsProvider.apiVersion) &&
        Objects.equals(this.apiVersions, modelsProvider.apiVersions) &&
        Objects.equals(this.askForDonations, modelsProvider.askForDonations) &&
        Objects.equals(this.caCertFingerprint, modelsProvider.caCertFingerprint) &&
        Objects.equals(this.caCertUri, modelsProvider.caCertUri) &&
        Objects.equals(this.countryCodeLookupUrl, modelsProvider.countryCodeLookupUrl) &&
        Objects.equals(this.defaultLanguage, modelsProvider.defaultLanguage) &&
        Objects.equals(this.description, modelsProvider.description) &&
        Objects.equals(this.domain, modelsProvider.domain) &&
        Objects.equals(this.donatePeriod, modelsProvider.donatePeriod) &&
        Objects.equals(this.donateUrl, modelsProvider.donateUrl) &&
        Objects.equals(this.infoUrl, modelsProvider.infoUrl) &&
        Objects.equals(this.languages, modelsProvider.languages) &&
        Objects.equals(this.motdUrl, modelsProvider.motdUrl) &&
        Objects.equals(this.name, modelsProvider.name) &&
        Objects.equals(this.service, modelsProvider.service) &&
        Objects.equals(this.services, modelsProvider.services) &&
        Objects.equals(this.stunServers, modelsProvider.stunServers) &&
        Objects.equals(this.tosUrl, modelsProvider.tosUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiUri, apiVersion, apiVersions, askForDonations, caCertFingerprint, caCertUri, countryCodeLookupUrl, defaultLanguage, description, domain, donatePeriod, donateUrl, infoUrl, languages, motdUrl, name, service, services, stunServers, tosUrl);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ModelsProvider {\n");
    
    sb.append("    apiUri: ").append(toIndentedString(apiUri)).append("\n");
    sb.append("    apiVersion: ").append(toIndentedString(apiVersion)).append("\n");
    sb.append("    apiVersions: ").append(toIndentedString(apiVersions)).append("\n");
    sb.append("    askForDonations: ").append(toIndentedString(askForDonations)).append("\n");
    sb.append("    caCertFingerprint: ").append(toIndentedString(caCertFingerprint)).append("\n");
    sb.append("    caCertUri: ").append(toIndentedString(caCertUri)).append("\n");
    sb.append("    countryCodeLookupUrl: ").append(toIndentedString(countryCodeLookupUrl)).append("\n");
    sb.append("    defaultLanguage: ").append(toIndentedString(defaultLanguage)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    donatePeriod: ").append(toIndentedString(donatePeriod)).append("\n");
    sb.append("    donateUrl: ").append(toIndentedString(donateUrl)).append("\n");
    sb.append("    infoUrl: ").append(toIndentedString(infoUrl)).append("\n");
    sb.append("    languages: ").append(toIndentedString(languages)).append("\n");
    sb.append("    motdUrl: ").append(toIndentedString(motdUrl)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    service: ").append(toIndentedString(service)).append("\n");
    sb.append("    services: ").append(toIndentedString(services)).append("\n");
    sb.append("    stunServers: ").append(toIndentedString(stunServers)).append("\n");
    sb.append("    tosUrl: ").append(toIndentedString(tosUrl)).append("\n");
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
