#!/bin/bash

# Copyright (c) 2022 LEAP Encryption Access Project and contributers
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.


function quit {
    echo -e "${RED}Task failed. Exit value: $?.${NC}"
    clean_up
    exit 1
}

function clean_up {
    if [[ -f ${ALIGNED_UNSIGNED_APK} ]]; then
        rm ${ALIGNED_UNSIGNED_APK}
    fi
    if [[ -f ${ALIGNED_SIGNED_APK} ]]; then
        rm ${ALIGNED_SIGNED_APK}
    fi
}

function sign {
    #---- ALIGN AND JARSIGN APK  -----
    if [[ -z $FILE_NAME_STRING ]]
    then
        FILE_NAME_STRING=$1
        FILE_NAME=${FILE_NAME_STRING##*/} #remove everything till the last '/'
        FILE_DIR=${FILE_NAME_STRING%/*} #remove everything after the last '/'
    fi

    if [[ $FILE_NAME == *.aab ]]; then
      sign_bundle $FILE_NAME $FILE_DIR
    else
      sign_apk $FILE_NAME $FILE_DIR
    fi

    clean_up

    gpg_sign $FILE_NAME_STRING
}

function sign_apk {
    FILE_NAME=$1
    FILE_DIR=$2
    FINAL_APK="${FILE_DIR}/${FILE_NAME}"
    echo -e "${GREEN} -> apksign ${FINAL_APK}${NC}"
    ${ANDROID_BUILD_TOOLS}/apksigner sign --ks "${KEY_STORE_STRING}" --out ${FINAL_APK} ${FINAL_APK} || quit
    verify_signed $FINAL_APK
}

function sign_bundle {
    FILENAME=$1
    FILE_DIR=$2
    FINAL_AAB="${FILE_DIR}/${FILE_NAME}"
    echo -e "${GREEN} -> jarsign ${FINAL_AAB}${NC}"
    if [[ -z $(which jarsigner) ]]; then
        echo -e "${RED}ERROR: jarsigner not in path. Please add it to your path variable. jarsigner is part of your JDK installation. Exiting.${NC}"
        exit
    fi;

    if [[ -z $KEYSTORE_ALIAS ]]; then
        echo -e "${RED}ERROR: keystore alias is missing. Please add flag -ka <yourkeystorealias>. Exiting.${NC}"
        exit
    fi;
    echo "jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore "KEY_STORE_STRING" $FINAL_AAB $KEYSTORE_ALIAS
"
    jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore "${KEY_STORE_STRING}" $FINAL_AAB $KEYSTORE_ALIAS || quit
    verify_signed $FINAL_AAB
}

function verify_signed {
    FINAL_FILE=$1
    FINGERPRINT=$(unzip -p ${FINAL_FILE} META-INF/*.RSA | keytool -printcert | grep "SHA256" | tr -d '[:space:]') || quit

    if [[ ${FINGERPRINT} == ${EXPECTED_FINGERPRINT} ]]; then
        echo "Certificate fingerprint matches: ${FINGERPRINT}"
    else
        echo -e "${RED}Certificate fingerprint \n${FINGERPRINT} \ndid not match expected fingerprint \n\t${EXPECTED_FINGERPRINT}${NC}"
        quit
    fi
}

function gpg_sign {
    FINAL_APK=$1
    if [[ -z ${GPG_KEY} && -z ${GPG_KEY_USER} ]]; then
        echo -e "${ORANGE}WARNING: Could not do gpg signing!${NC}"
        exit
    fi

    if [[ ${GPG_KEY} ]]; then
        echo -e "${GREEN} -> gpg sign using key ${GPG_KEY}${NC}"
        gpg --default-key ${GPG_KEY} --armor --output "${FINAL_APK}.sig" --detach-sign ${FINAL_APK} || quit
    else
        echo -e "${GREEN} -> gpg sign using key of user ${GPG_KEY_USER}${NC}"
        gpg -u ${GPG_KEY_USER} --armor --output "${FINAL_APK}.sig" --detach-sign ${FINAL_APK} || quit
    fi

    echo -e "${GREEN} -> gpg verify ${FINAL_APK}${NC}"
    gpg --verify "${FINAL_APK}.sig" || quit
}

function base_dir {
    echo "$(dirname "$0")/.."
}

function script_dir {
    echo "$(dirname "$0")"
}

# ----Main-----

DO_BUILD=false
BUILD_SPLIT=false
BUILD_FAT=false
BUILD_WEB=false
DO_SIGN=false
BETA=false
NO_TAG=false
STACKTRACE=""
FLAVOR="Normal"
FLAVOR_LOWERCASE="normal"
FLAVOR2=""
FLAVOR2_LOWERCASE=""
EXPECTED_FINGERPRINT="SHA256:9C:94:DB:F8:46:FD:95:97:47:57:17:2A:6A:8D:9A:9B:DF:8C:40:21:A6:6C:15:11:28:28:D1:72:39:1B:81:AA"
GREEN='\033[0;32m'
RED='\033[0;31m'
ORANGE='\033[0;33m'
NC='\033[0m'

export GREEN=${GREEN}
export RED=${RED}
export ORANGE=${ORANGE}
export NC=${NC}
export EXPECTED_FINGERPRINT=${EXPECTED_FINGERPRINT}
export -f sign
export -f quit
export -f clean_up
export -f sign_apk
export -f sign_bundle
export -f verify_signed
export -f gpg_sign


# init parameters
for ((i=1;i<=$#;i++)); 
do
    if [[ ${!i} = "b" || ${!i} = "build" ]]; then
        DO_BUILD=true
        
    elif [[ ${!i} = "s" || ${!i} = "sign" ]]; then
        DO_SIGN=true
        
    elif [[ ${!i} = "-f" || ${!i} = "-file" ]]; then
        ((i++)) 
        FILE_NAME_STRING=${!i}
        FILE_NAME=${FILE_NAME_STRING##*/} #remove everything till the last '/'
        FILE_DIR=${FILE_NAME_STRING%/*} #remove everything after the last '/'

    elif [[ ${!i} = "-d" || ${!i} = "-dir" ]]; then
        ((i++))
        FILE_DIR=${!i}
        MULTIPLE_APKS=true
    elif [[ ${!i} = "-ks" || ${!i} = "-keystore" ]]; then
        ((i++)) 
        KEY_STORE_STRING=${!i};
        KEY_STORE_NAME=${KEY_STORE_STRING##*/}
        KEY_STORE_DIR=${KEY_STORE_STRING%/*}
        export KEY_STORE_STRING=${KEY_STORE_STRING}
    elif [[ ${!i} = "-ka" || ${!i} = "-keystorealias" ]]; then
        ((i++))
        export KEYSTORE_ALIAS=${!i};
    elif [[ ${!i} = "-v" || ${!i} = "-version" ]]; then
        ((i++)) 
        VERSION_NAME=${!i};
        if [[ -z $(git tag --list | grep -w ${VERSION_NAME}) ]] 
        then
            echo -e "${RED}ERROR: Version name has to be a git tag!${NC}"
            exit
        fi
    elif [[ ${!i} = "-k" || ${!i} = "-key" ]]; then
        ((i++)) 
        GPG_KEY=${!i}
        export GPG_KEY=${GPG_KEY}
    elif [[ ${!i} = "-u" || ${!i} = "-user" ]]; then
        ((i++)) 
        GPG_KEY_USER=${!i}
        export GPG_KEY_USER=${GPG_KEY_USER}
    elif [[ ${!i} = "-b" || ${!i} = "-beta" ]]; then
        BETA=true
    elif [[ ${!i} = "-no-tag" ]]; then
        NO_TAG=true
    elif [[ ${!i} = "-s" || ${!i} = "-stacktrace" ]]; then
        STACKTRACE="--stacktrace"
    elif [[  ${!i} = "-c" || ${!i} = "-custom" ]]; then
        if [[ -z ${FLAVOR2} ]]; then
          FLAVOR="Custom"
          FLAVOR_LOWERCASE="custom"
        fi
    elif [[  ${!i} = "-af" || ${!i} = "-allflavors" ]]; then
        FLAVOR="Normal"
        FLAVOR_LOWERCASE="normal"
        FLAVOR2="Custom"
        FLAVOR2_LOWERCASE="custom"
    elif [[ ${!i} = "-aab" ]]; then
        BUILD_BUNDLE=true
    elif [[ ${!i} = "-apk" ]]; then
        ((i++))
        if [[ ${!i} != "fat" && ${!i} != "split" &&  ${!i} != "web" && ${!i} != "all" ]]; then
          echo -e "${RED}ERROR: -apk requires fat, web, split or all ${NC}"
          exit
        fi
        BUILD_APK=true
        if [[ ${!i} == "split" || ${!i} == "all" ]]; then BUILD_SPLIT=true; fi;
        if [[ ${!i} == "fat" || ${!i} == "all" ]]; then BUILD_FAT=true; fi;
        if [[ ${!i} == "web" || ${!i} == "all" ]]; then BUILD_WEB=true; fi;
    elif [[ ${!i} = "-h" || ${!i} = "-help" ]]; then
        echo -e "
        sign [-ks -ka -fp -f -u -k]           sign a given apk (both app signing and GPG signing)
        -ks / -keystore [path] -------------- define path to keystore for signing (required)
        -ka / -keystorealias ---------------- keystore alias (required if aab is signed)
        -fp / -fingerprint [fingerprint] ---- define the fingerprint for the app (required for non-LEAP
                                              signed apps)
        -f / -file [inputfile] -------------- define path to apk going to be signed
        -d / -dir [path] -------------------- define path to directory including apks to be signed
        -u / -user [gpguser] ---------------- define the gpg user whose key will be used for GPG signing
                                              (optional)
        -k / -key [gpgkey] ------------------ define the key used for GPG signing. Using this option,
                                              -u will be ignored (optional)                           
        
        
        build [-v, -c, -b, -no-tag, -apk, -aab, -s]
        -v / -version [gittag] -------------- define the git version tag that needs to be checked out 
                                              for building. It's also part of the resulting apk file 
                                              name. (required if you don't use -no-tag)
        -c / -custom ------------------------ build custom Bitmask client instead of main Bitmask client 
                                              (optional)
        -b / -beta -------------------------- build beta version with .beta appended to applicationId (optional)
        -apk [all, split, fat, web] --------- build only apk(s)
        -aab -------------------------------- build only android app bundle
        -no-tag ----------------------------- force to build current checked out git commit instead of an
                                              official release version
        -s / -stacktrace -------------------- show verbose debug output

        
        -h / -help                            print out this help
        
        
        example Usages:
        ---------------
        
        ${GREEN}* jarsign only:${NC}
        ./prepareForDistribution.sh sign -f app/build/outputs/apk/app-production-beta.apk -ks ~/path/to/bitmask-android.keystore
        ${GREEN}* jarsign and gpg sign only:${NC}
        ./prepareForDistribution.sh sign -f app/build/outputs/apk/app-production-beta.apk -ks ~/path/to/bitmask-android.keystore -u GPG_USER
        ${GREEN}* jarsign and gpg sign all apks in directory:${NC}
        ./prepareForDistribution.sh sign -d currentReleases/ -ks ~/path/to/bitmask-android.keystore -u GPG_USER
        ${GREEN}* build custom stable:${NC}
        ./prepareForDistribution.sh build -v 0.9.7 -c
        ${GREEN}* build and sign custom stable (apks and aab):${NC}
        ./prepareForDistribution.sh build sign -ks ~/path/to/bitmask-android.keystore -u GPG_USER -c -v 0.9.7
        ${GREEN}* build and sign custom beta (apks and aab):${NC}
        ./prepareForDistribution.sh build sign -ks ~/path/to/bitmask-android.keystore -u GPG_USER -c -b -v 0.9.7RC2
        ${GREEN}* build and sign stable (apks and aab):${NC}
        ./prepareForDistribution.sh build sign -ks ~/path/to/bitmask-android.keystore -u GPG_USER -v 0.9.7
        ${GREEN}* build and sign apks and bundle for current git HEAD:${NC}
        ./prepareForDistribution.sh build sign -ks ~/path/to/bitmask-android.keystore -u GPG_USER -no-tag
        ${GREEN}* build and sign bundle only for current git HEAD:${NC}
        ./prepareForDistribution.sh build -aab sign -ks ~/path/to/bitmask-android.keystore -ka exampleAlias -u GPG_USER -no-tag"
        exit

    else
        echo -e "${RED}Invalid argument: ${!i}${NC}"
        exit
    fi

done;


# check what to do
if [[ ${DO_BUILD} == false && ${DO_SIGN} == false ]]; then
    echo -e "${RED}ERROR: No action set. Please check  ./prepareForDistribution -help!${NC}"
    exit
fi

if [[ ${DO_BUILD} == true ]]; then
    if [[ ${NO_TAG} == false && -z ${VERSION_NAME} ]]; then
        echo -e "${RED}ERROR: You didn't enter the version (git tag) to be built. If you really want to force building the current checked out commit, use -no-tag.${NC}"
        quit
    fi
    if [[ ${BUILD_APK} == true && ${BUILD_AAB} == true ]]; then
        echo -e "${RED}ERROR: the flags -apk and -aab are mutually exclusive. To build apks and aab, please remove both flags.${NC}"
        quit
    fi
    if [[ ${DO_SIGN} == true && -z ${BUILD_APK} && -z ${BUILD_AAB} && -z ${KEYSTORE_ALIAS} ]]; then
        echo -e "${RED}ERROR: keystore alias is missing. Please add flag -ka <yourkeystorealias>. Exiting.${NC}"
        quit
    fi
    if [[ ${DO_SIGN} == true && ${BUILD_AAB} && -z ${KEYSTORE_ALIAS} ]]; then
        echo -e "${RED}ERROR: keystore alias is missing. Please add flag -ka <yourkeystorealias>. Exiting.${NC}"
        quit
    fi

    if [[ ${NO_TAG} == false ]]; then
        #---- COMPARE TAG COMMIT WITH CURRENT COMMIT AND CHECK OUT TAG COMMIT IF NECESSARY ----
        TAG_COMMIT=$(git log -n 1 ${VERSION_NAME} --format="%H")
        CURRENT_COMMIT=$(git log -n 1 --format="%H")
        if [[ ${TAG_COMMIT} != ${CURRENT_COMMIT} ]]
        then
            echo "CHECKING OUT VERSION: ${VERSION_NAME} ..."
            git checkout ${VERSION_NAME} || quit
        fi
    fi

    $(script_dir)/cleanProject.sh || quit
    $(script_dir)/build_deps.sh || quit
    $(script_dir)/fix_gradle_lock.sh || quit
    
    cd $(base_dir)
    BASE_APK_OUTPUT_DIR="./app/build/outputs/apk"
    BASE_BUNDLE_OUTPUT_DIR="./app/build/outputs/bundle"
    RELEASES_FILE_DIR="./currentReleases"
    if [[ ! -d $RELEASES_FILE_DIR ]]; then
        mkdir $RELEASES_FILE_DIR
    fi
    rm -rf $RELEASES_FILE_DIR/*

    if [[ ${BETA} == true ]]; then
        if [[ -z ${BUILD_BUNDLE} ]]; then
          if [[ -z ${FLAVOR2} ]]; then
            echo "${GREEN} -> build beta releases (.apk) for flavor ${FLAVOR}${NC}"
          else
            echo "${GREEN} -> build beta releases (.apk) for flavor ${FLAVOR} and ${FLAVOR2}${NC}"
          fi
          if [[ ${BUILD_WEB} == true ]]; then
            echo -e "${RED}ERROR: There are no beta apks for web build flavor. Exiting.${NC}"
            quit
          fi
          if [[ ${BUILD_FAT} == true ]]; then
            ./gradlew clean assemble${FLAVOR}ProductionFatBeta $STACKTRACE || quit
   #        echo "copy file:  $(ls $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionFat/beta/*.apk)"
            cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionFat/beta/*.apk $RELEASES_FILE_DIR/.
           if [[ -n ${FLAVOR2} ]]; then
            ./gradlew clean assemble${FLAVOR2}ProductionFatBeta $STACKTRACE || quit
            cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionFat/beta/*.apk $RELEASES_FILE_DIR/.
           fi
          fi
          if [[ ${BUILD_SPLIT} == true ]]; then
            # custom builds might have disabled split apks -> check if build task exist
            if [[ $(./gradlew tasks --console plain | grep ${FLAVOR}ProductionX86Beta) ]]; then
                ./gradlew clean assemble${FLAVOR}ProductionX86Beta $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionX86/beta/*.apk $RELEASES_FILE_DIR/.
            fi
            if [[  $(./gradlew tasks --console plain | grep ${FLAVOR}ProductionX86_64Beta) ]]; then
                ./gradlew clean assemble${FLAVOR}ProductionX86_64Beta $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionX86_64/beta/*.apk $RELEASES_FILE_DIR/.
            fi
            if [[ $(./gradlew tasks --console plain | grep ${FLAVOR}ProductionArmv7Beta) ]]; then
                ./gradlew clean assemble${FLAVOR}ProductionArmv7Beta $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionArmv7/beta/*.apk $RELEASES_FILE_DIR/.
            fi
            if [[ $(./gradlew tasks --console plain | grep ${FLAVOR}ProductionArmv7Beta) ]]; then
                ./gradlew clean assemble${FLAVOR}ProductionArm64Beta $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionArm64/beta/*.apk $RELEASES_FILE_DIR/.
            fi
            if [[ -n ${FLAVOR2} ]]; then
              if [[ $(./gradlew tasks --console plain | grep ${FLAVOR2}ProductionX86Beta) ]]; then
                ./gradlew clean assemble${FLAVOR2}ProductionX86Beta $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionX86/beta/*.apk $RELEASES_FILE_DIR/.
              fi
              if [[  $(./gradlew tasks --console plain | grep ${FLAVOR2}ProductionX86_64Beta) ]]; then
                ./gradlew clean assemble${FLAVOR22}ProductionX86_64Beta $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionX86_64/beta/*.apk $RELEASES_FILE_DIR/.
              fi
              if [[ $(./gradlew tasks --console plain | grep ${FLAVOR2}ProductionArmv7Beta) ]]; then
                ./gradlew clean assemble${FLAVOR2}ProductionArmv7Beta $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionArmv7/beta/*.apk $RELEASES_FILE_DIR/.
              fi
              if [[ $(./gradlew tasks --console plain | grep ${FLAVOR2}ProductionArmv7Beta) ]]; then
                ./gradlew clean assemble${FLAVOR2}ProductionArm64Beta $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionArm64/beta/*.apk $RELEASES_FILE_DIR/.
              fi
            fi
          fi
        fi
        if [[ -z ${BUILD_APK} ]]; then
          if [[ -z ${FLAVOR2} ]]; then
            echo "${GREEN} -> build beta release (.aab) for flavor ${FLAVOR}${NC}"
          else
            echo "${GREEN} -> build beta releases (.aab) for flavor ${FLAVOR} and ${FLAVOR2}${NC}"
          fi
          ./gradlew clean bundle${FLAVOR}ProductionFatBeta $STACKTRACE || quit
          cp $BASE_BUNDLE_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionFatBeta/*.aab $RELEASES_FILE_DIR/.
          if [[ -n ${FLAVOR2} ]]; then
            ./gradlew clean bundle${FLAVOR2}ProductionFatBeta $STACKTRACE || quit
            cp $BASE_BUNDLE_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionFatBeta/*.aab $RELEASES_FILE_DIR/.
          fi
        fi
    else
        # default: neither -aab nor -apk is passed: build apks
        if ([[ -z ${BUILD_BUNDLE} ]] && [[ -z ${BUILD_APK} ]]) || ([[ -n ${BUILD_APK} ]]); then
          if [[ -z ${FLAVOR2} ]]; then
            echo -e "${GREEN} -> build stable releases (.apk) for flavor ${FLAVOR}${NC}"
          else
            echo -e "${GREEN} -> build stable releases (.apk) for flavor ${FLAVOR} and ${FLAVOR2}${NC}"
          fi
          if [[ ${BUILD_FAT} == true ]]; then
            ./gradlew clean assemble${FLAVOR}ProductionFatRelease $STACKTRACE || quit
            cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionFat/release/*.apk $RELEASES_FILE_DIR/.
            if [[ -n ${FLAVOR2} ]]; then
              ./gradlew clean assemble${FLAVOR2}ProductionFatRelease $STACKTRACE || quit
              cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionFat/release/*.apk $RELEASES_FILE_DIR/.
            fi
          fi
          if [[ ${BUILD_WEB} == true ]]; then
            ./gradlew clean assemble${FLAVOR}ProductionFatwebRelease $STACKTRACE || quit
            cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionFatweb/release/*.apk $RELEASES_FILE_DIR/.
            if [[ -n ${FLAVOR2} ]]; then
              ./gradlew clean assemble${FLAVOR2}ProductionFatwebRelease $STACKTRACE || quit
              cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionFatweb/release/*.apk $RELEASES_FILE_DIR/.
            fi
          fi
          if [[ ${BUILD_SPLIT} == true ]]; then
            # custom builds might have disabled split apks -> check if build task exist
            if [[ $(./gradlew tasks --console plain | grep ${FLAVOR}ProductionX86Release) ]]; then
                ./gradlew clean assemble${FLAVOR}ProductionX86Release $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionX86/release/*.apk $RELEASES_FILE_DIR/.
            fi
            if [[ $(./gradlew tasks --console plain | grep ${FLAVOR}ProductionX86_64Release) ]]; then
                ./gradlew clean assemble${FLAVOR}ProductionX86_64Release $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionX86_64/release/*.apk $RELEASES_FILE_DIR/.
            fi
            if [[ $(./gradlew tasks --console plain | grep ${FLAVOR}ProductionArmv7Release) ]]; then
                ./gradlew clean assemble${FLAVOR}ProductionArmv7Release $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionArmv7/release/*.apk $RELEASES_FILE_DIR/.
            fi
            if [[ $(./gradlew tasks --console plain | grep ${FLAVOR}ProductionArm64Release) ]]; then
                ./gradlew clean assemble${FLAVOR}ProductionArm64Release $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionArm64/release/*.apk $RELEASES_FILE_DIR/.
            fi
            if [[ -n ${FLAVOR2} ]]; then
              if [[ $(./gradlew tasks --console plain | grep ${FLAVOR2}ProductionX86Release) ]]; then
                ./gradlew clean assemble${FLAVOR2}ProductionX86Release $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionX86/release/*.apk $RELEASES_FILE_DIR/.
              fi
              if [[ $(./gradlew tasks --console plain | grep ${FLAVOR2}ProductionX86_64Release) ]]; then
                ./gradlew clean assemble${FLAVOR2}ProductionX86_64Release $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionX86_64/release/*.apk $RELEASES_FILE_DIR/.
              fi
              if [[ $(./gradlew tasks --console plain | grep ${FLAVOR2}ProductionArmv7Release) ]]; then
                ./gradlew clean assemble${FLAVOR2}ProductionArmv7Release $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionArmv7/release/*.apk $RELEASES_FILE_DIR/.
              fi
              if [[ $(./gradlew tasks --console plain | grep ${FLAVOR2}ProductionArm64Release) ]]; then
                ./gradlew clean assemble${FLAVOR2}ProductionArm64Release $STACKTRACE || quit
                cp $BASE_APK_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionArm64/release/*.apk $RELEASES_FILE_DIR/.
              fi
            fi
          fi
        fi
        if [[ -n ${BUILD_BUNDLE} ]]; then
          if [[ -z ${FLAVOR2} ]]; then
            echo -e "${GREEN} -> build stable release (.aab) for flavor ${FLAVOR}${NC}"
          else
            echo -e "${GREEN} -> build stable release (.aab) for flavor ${FLAVOR} and ${FLAVOR2}${NC}"
          fi
          ./gradlew clean bundle${FLAVOR}ProductionFatRelease $STACKTRACE || quit
          cp $BASE_BUNDLE_OUTPUT_DIR/${FLAVOR_LOWERCASE}ProductionFatRelease/*.aab $RELEASES_FILE_DIR/.
          if [[ -n ${FLAVOR2} ]]; then
            ./gradlew clean bundle${FLAVOR2}ProductionFatRelease $STACKTRACE || quit
            cp $BASE_BUNDLE_OUTPUT_DIR/${FLAVOR2_LOWERCASE}ProductionFatRelease/*.aab $RELEASES_FILE_DIR/.
          fi
        fi
    fi
    
    cd -
fi

if [[ ${DO_SIGN} == true ]]
then
    cd $(base_dir)
    # check global vars
    if [[ -z ${ANDROID_BUILD_TOOLS} ]]; then
        echo -e "${RED}ERROR: Environment variable ANDROID_BUILD_TOOLS not set! Please add it to your environment variables. Exiting.${NC}"
        exit 
    fi

    if [[ -z ${FILE_NAME} && -z ${FILE_DIR} && ${DO_BUILD} == false ]]; then
        echo -e "${RED}ERROR: Sign only needs a file name or a directory. Please check ./prepareForDistribution -help!${NC}"
        exit
    fi
    if [[ -z ${KEY_STORE_NAME} ]]; then
        echo -e "${RED}ERROR: Key store not set. Please check ./prepareForDistribution -help${NC}"
        exit
    fi
    if [[ -n ${FILE_NAME_STRING} && ${DO_BUILD} == true ]]; then
        echo -e "${ORANGE}WARNING: Ignoring parameter -file. Built APK will be used instead.${NC}"
    fi
    
    #---- OPT: SELECT APK FROM LAST BUILD ----
    if [[ ${DO_BUILD} == true ]]; then
        FILE_DIR=$RELEASES_FILE_DIR
        echo -e "${GREEN} -> sign apks:${NC}"
        ls -w 1 $FILE_DIR/*\.apk | xargs -I {} echo {}
        xargs -I _ -ra <(ls -w 1 $FILE_DIR/*\.apk) bash -c 'sign _'
        echo -e "${GREEN} -> sign aab:${NC}"
        ls -w 1 $FILE_DIR/*\.aab | xargs -I {} echo {}
        xargs -I _ -ra <(ls -w 1 $FILE_DIR/*\.aab) bash -c 'sign _'
    elif [[ ${MULTIPLE_APKS} == true ]]; then
        echo -e "${GREEN} -> sign apks:${NC}"
        ls -w 1 $FILE_DIR/*\.apk | xargs -I {} echo {}
        xargs -I _ -ra <(ls -w 1 $FILE_DIR/*\.apk) bash -c 'sign _'
        echo -e "${GREEN} -> sign aab:${NC}"
        ls -w 1 $FILE_DIR/*\.aab | xargs -I {} echo {}
        xargs -I _ -ra <(ls -w 1 $FILE_DIR/*\.aab) bash -c 'sign _'
    else
        echo -e "${GREEN} -> sign apk/aab: ${FILE_NAME_STRING}${NC}"
        sign $FILE_NAME_STRING
    fi
    cd -
fi
