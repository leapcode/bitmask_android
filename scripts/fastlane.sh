#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

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
if [[ -z $BUILD_CUSTOM ]]; then
  fastlane android bitmask_screenshots
else
  fastlane android custom_build_screenshots --env custom
fi;
cd -

