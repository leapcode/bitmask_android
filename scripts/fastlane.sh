#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

function setHeadsupNotifications {
    echo -e "${GREEN}---------------------------------------${NC}"
    echo -e "${GREEN}-- Setting head-up notifications: $1 ---${NC}"
    echo -e "${GREEN}---------------------------------------${NC}"
    adb devices | grep -v -i "list" | sed 's/\t/ /' | cut -d ' ' -f 1 | xargs -I {} adb -s {} shell settings put global heads_up_notifications_enabled $1
}

# init parameters
if [[ ${1} = "custom" ]]; then
  BUILD_CUSTOM=true
elif [[ ! -z ${1} ]]; then
  echo -e """${RED}Failed due to wrong arguments.${NC}
  Usage:
  ======
  ${GREEN}create screenshots for Bitmask:${NC}
  ./fastlane.sh

  ${GREEN}create screenshots for your custom build${NC} (please adopt the environment variables in './fastlane/.env.custom'):
  ./fastlane.sh custom
  """
  exit 1
fi;

#screengrab related environment variables can be found in ./fastlane/.env.*
SCRIPT_DIR=$(dirname "$0")
BASE_DIR="$SCRIPT_DIR/.."

cd $BASE_DIR
setHeadsupNotifications 0
if [[ -z $BUILD_CUSTOM ]]; then
   echo -e "${GREEN}--     Screenshotting Bitmask       ---${NC}"
   fastlane --verbose  android bitmask_screenshots
else
   echo -e "${GREEN}--    Screenshotting custom build   ---${NC}"
   fastlane android custom_build_screenshots --env custom
fi;
setHeadsupNotifications 1
cd -
