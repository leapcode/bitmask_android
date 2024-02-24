# Custom branding
## Feature flags
You can customize Bitmask and create provider specific branded version of it. 
There's a section called "Configurations for custom branded app." in `app/build.gradle` that contains various build properties to alter the behavior of the app. Descriptions of the properties are provided inline.

## Bootstrapping

You need to specify a couple of URLs required for bootstrapping. 
In `app/src/custom/assets/urls`, you need to create a json file with the naming scheme `<domain_name>.url`.
The json *needs* to contain the field `"main_url" : "<main entry point of your leap provider>"`. The main entry point is the domain where your publicly available provider.json and CA cert is served.

*Optional* fields are:
| key | value | 
|----------|-------------|
| geoip_url | URL your [menshen](https://0xacab.org/leap/menshen) service points to | 
| motd_url | URL you serve a [message of the day json](https://0xacab.org/leap/motd). Motd is not yet integrated in Lillypad and needs to be deployed separately |
| provider_ip | IP under which your provider.json and CA cert are provided. This allows to circumvent DNS blockings |
| provider_api_ip | IP under which your eip-service.json and VPN credentials are provided. This allows to circumvent DNS blockings |

Additionally you can preship your your `provider.json` and the PEM formatted CA cert used for the communication to the API. It allows certificate pinning without an trust on first use model and improves slightly the security for the API communication. The files need to be located in `app/src/custom/assets/` and named as `<domainname>.json` and `<domainname>.pem`.

## Design
In order to adapt the **color scheme** of the app, you need to replace the values in `app/src/custom/values/custom-theme.xml`. Descriptions of the resource keys are provided inline.

There are a couple of images you should replace to customize your app. It is important that all assets you replace keep the exact same file name, otherwise the app won't use them. Please be aware that assets might occour more than once in different `drawable*` directories. All of them need to be replaced to avoid inconsistencies across different devices. 

The following table shows relevant asset names in `app/src/custom/res` to change animations and images. Since it's is possible that assets of the same resource have different file endings, e.g. .png or .xml, they are shown without file endings in the table.

| asset | resource | comment |
|----------|-------------| -----|
| launcher icon | `ic_launcher` | |
| message of the day icon | `ic_motd` | |
| donation reminder icon | `logo_square` | |
| navigation drawer foreground logo | `drawer_logo` | can be omitted if your drawer logo doesn't consist of a foreground and background image |
| navigation drawer background image | `background_drawer` | |
| rotating VPN connection progress animation | `rotate_progress_image` | shows progress at the border of the on/off button |
| VPN connected image | `state_connected`| |
| VPN connecting image | `state_connecting` | an example for an animated image can be found in `drawable-anydpi-v24/state_connecting.xml` |
| VPN disconnected image | `state_disconnected` | |
| background image connected state | `bg_connected` ||
| background image connecting state | `bg_connecting` ||
| background image disconnected state | `bg_disconnected` ||
| transition animation connected - disconnected | `state_transition_connected_disconnected` ||
| spash view (until Android 11) | `ic_splash_background` ||
| splash view icon (Android 12+) | `splash_icon` ||
| splash view branding (Android 12+) | `splash_branding` | is shown at the bottom of the splash view |

## Donations
You can enable a donation reminder for your custom branded app:

1. Adapt the following fields in build.gradle:
```gradle
  //This is the donation URL and should be set to the relevant donation page.
      buildConfigField 'String', 'donation_url', '"https://riseup.net/vpn/donate"'
      //The field to enable donations in the app.
      buildConfigField 'boolean', 'enable_donation', 'true'
      //The field to enable donation reminder popup in the app if enable_donation is set to 'false' this will be disabled.
      buildConfigField 'boolean', 'enable_donation_reminder', 'true'
      //The duration in days to trigger the donation reminder
      buildConfigField 'int', 'donation_reminder_duration', '7'
```
2. Adapt the donation reminder icon (see table above)
3. replace all `donate_message` strings in `app/src/custom/res/values*/strings.xml`. If you don't have a translation for a language, remove the `donate_message` entry from the corresponding `strings.xml`

## Terms of Service
There's a string resource `terms_of_service` in `app/src/custom/res/values*/strings.xml` that you should adapt for your provider. 

## Compiling

Please make sure you have checked out the [submodules](../../../README.md#submodules) and built the [dependencies](../../../README.md#just-build-it) first.

In order to build a debug version of your custom branded app run:
```bash
./gradlew assembleCustomProductionFatDebug
```
