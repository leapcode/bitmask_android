#!/bin/bash


function quit {
    echo "Task failed. Exit value: $?."
    cleanUp
    exit 1
}

function cleanUp {
    if [[ -f ${ALIGNED_UNSIGNED_APK} ]]
    then
        rm ${ALIGNED_UNSIGNED_APK}
    fi
    if [[ -f ${ALIGNED_SIGNED_APK} ]]
    then
        rm ${ALIGNED_SIGNED_APK}
    fi
}

# ----Main-----

DO_BUILD=false
DO_SIGN=false
BETA=false
NO_TAG=false
APP_NAME="Bitmask"
FLAVOR="Normal"
FLAVOR_LOWERCASE="normal"
EXPECTED_FINGERPRINT="SHA256:9C:94:DB:F8:46:FD:95:97:47:57:17:2A:6A:8D:9A:9B:DF:8C:40:21:A6:6C:15:11:28:28:D1:72:39:1B:81:AA"



# check global vars
if [[ -z ${ANDROID_BUILD_TOOLS} ]] 
then
    echo "ERROR: Environment variable ANDROID_BUILD_TOOLS not set! Please add it to your .bashrc file. Exiting."
    exit 
fi

# init parameters
for ((i=1;i<=$#;i++)); 
do
    if [[ ${!i} = "b" || ${!i} = "build" ]] 
    then 
        DO_BUILD=true
        
    elif [[ ${!i} = "s" || ${!i} = "sign" ]] 
    then 
        DO_SIGN=true
        
    elif [[ ${!i} = "-f" || ${!i} = "-file" ]] 
    then 
        ((i++)) 
        FILE_NAME_STRING=${!i}
        FILE_NAME=${FILE_NAME_STRING##*/} #remove everything till the last '/'
        FILE_DIR=${FILE_NAME_STRING%/*} #remove everything after the last '/'

    elif [[ ${!i} = "-ks" || ${!i} = "-keystore" ]] 
    then 
        ((i++)) 
        KEY_STORE_STRING=${!i};
        KEY_STORE_NAME=${KEY_STORE_STRING##*/}
        KEY_STORE_DIR=${KEY_STORE_STRING%/*}
        
    elif [[ ${!i} = "-v" || ${!i} = "-version" ]] 
    then 
        ((i++)) 
        VERSION_NAME=${!i};
        if [[ -z $(git tag --list | grep -w ${VERSION_NAME}) ]] 
        then
            echo "ERROR: Version name has to be a git tag!"
            exit
        fi
    elif [[ ${!i} = "-k" || ${!i} = "-key" ]];
    then 
        ((i++)) 
        GPG_KEY=${!i}    
    elif [[ ${!i} = "-k" || ${!i} = "-key" ]];
    then 
        ((i++)) 
        GPG_KEY=${!i}
    elif [[ ${!i} = "-u" || ${!i} = "-user" ]];
    then 
        ((i++)) 
        GPG_KEY_USER=${!i}
    elif [[ ${!i} = "-b" || ${!i} = "-beta" ]];
    then 
        BETA=true
    elif [[ ${!i} = "-no-tag" ]];
    then 
        NO_TAG=true
    elif [[  ${!i} = "-c" || ${!i} = "-custom" ]]
    then
        ((i++))
        APP_NAME=${!i}
        FLAVOR="Custom"
        FLAVOR_LOWERCASE="custom"
    elif [[ ${!i} = "-h" || ${!i} = "-help" ]];
    then 
        echo -e "
        sign [-ks -fp -f -b -c -u -k]         sign a given apk (both app signing and GPG signing)
        -b / -beta -------------------------- add 'Beta' to filename of resulting apk (optional)
        -c / -custom [appName] -------------- mark build as custom branded Bitmask client and add 
                                              custom app name to resulting apk (required for custom
                                              branded builds)
        -ks / -keystore [path] -------------- define path to keystore for signing (required)
        -fp / -fingerprint [fingerprint] ---- define the fingerprint for the app (required for non-LEAP
                                              signed apps)
        -f / -file [inputfile] -------------- define path to apk going to be signed (required if 
                                              sign command is not used in conjuction with build)
        -v / -version [gittag] -------------- define app version as part of the resulting apk file
                                              name. If not used, 'latest' will be added instead
        -u / -user [gpguser] ---------------- define the gpg user whose key will be used for GPG signing
                                              (optional)
        -k / -key [gpgkey] ------------------ define the key used for GPG signing. Using this option,
                                              -u will be ignored (optional)                           
        
        
        build [-v, -c, -b, -no-tag]
        -v / -version [gittag] -------------- define the git version tag that needs to be checked out 
                                              for building. It's also part of the resulting apk file 
                                              name. (required if you don't use -no-tag)
        -c / -custom ------------------------ build custom Bitmask client instead of main Bitmask client 
                                              (optional)
        -b / -beta -------------------------- add 'Beta' to filename (optional)
        -no-tag ----------------------------- force to build current checked out git commit instead of an
                                              official release version

        
        -h / -help                            print out this help
        
        
        example Usages:
        ---------------
        
        * jarsign only:
        ./prepareForDistribution.sh sign -f app/build/outputs/apk/app-production-beta.apk -ks ~/path/to/bitmask-android.keystore -v 0.9.7
        
        * jarsign and gpg sign only:
        ./prepareForDistribution.sh sign -f app/build/outputs/apk/app-production-beta.apk -ks ~/path/to/bitmask-android.keystore -u GPG_USER -v 0.9.7
        
        * build custom stable
        ./prepareForDistribution.sh build -v 0.9.7 -c RiseupVPN
        
        * build and sign custom stable:
        ./prepareForDistribution.sh build sign -ks ~/path/to/bitmask-android.keystore -u GPG_USER -v 0.9.7 -c RiseupVPN
        
        * build and sign custom beta:
        ./prepareForDistribution.sh build sign -ks ~/path/to/bitmask-android.keystore -u GPG_USER -b -v 0.9.7RC2 -c RiseupVPN

        * build and sign stable:
        ./prepareForDistribution.sh build sign -ks ~/path/to/bitmask-android.keystore -u GPG_USER -v 0.9.7
        
        * build and sign current git HEAD
        ./prepareForDistribution.sh build sign -ks ~/path/to/bitmask-android.keystore -u GPG_USER -no-tag"
        exit

    else
        echo "Invalid argument: ${!i}"
        exit
    fi

done;


# check what to do
if [[ ${DO_BUILD} == false && ${DO_SIGN} == false ]]
then
    echo "ERROR: No action set. Please check  ./prepareForDistribution -help!"
    exit
fi

if [[ ${DO_BUILD} == true ]]
then
    if [[ ${NO_TAG} == false && -z ${VERSION_NAME} ]]
    then
        echo "ERROR: You didn't enter the version (git tag) to be built. If you really want to force building the current checked out commit, use -no-tag."
        quit
    fi
    if [[ ${NO_TAG} == false ]] 
    then
        #---- COMPARE TAG COMMIT WITH CURRENT COMMIT AND CHECK OUT TAG COMMIT IF NECESSARY ----
        TAG_COMMIT=$(git log -n 1 ${VERSION_NAME} --format="%H")
        CURRENT_COMMIT=$(git log -n 1 --format="%H")
        if [[ ${TAG_COMMIT} != ${CURRENT_COMMIT} ]]
        then
            echo "CHECKING OUT VERSION: ${VERSION_NAME} ..."
            git checkout ${VERSION_NAME} || quit
        fi
    fi

    ./cleanProject.sh || quit
    ./build_deps.sh || quit
    
    if [[ ${BETA} == true ]]
    then
        ./gradlew clean assemble${FLAVOR}ProductionBeta --stacktrace || quit
    else
        ./gradlew clean assemble${FLAVOR}ProductionRelease --stacktrace || quit
    fi
fi

if [[ ${DO_SIGN} == true ]]
then
    if [[ -z ${FILE_NAME} && ${DO_BUILD} == false ]] 
    then
        echo -e "ERROR: Sign only needs a file name. Please check ./prepareForDistribution -help!"
        exit
    fi
    if [[ -z ${KEY_STORE_NAME} ]] 
    then
        echo "ERROR: Key store not set. Please check ./prepareForDistribution -help"
        exit
    fi
    if [[ -n ${FILE_NAME_STRING} && ${DO_BUILD} == true ]]
    then
        echo "WARNING: Ignoring parameter -file. Built APK will be used instead."
    fi
    
    #---- OPT: SELECT APK FROM LAST BUILD ----
    if [[ ${DO_BUILD} == true ]]
    then
        BASE_FILE_DIR="$(pwd)/app/build/outputs/apk"
        if [[ ${BETA} == true ]]
        then
            FILE_NAME="app-${FLAVOR_LOWERCASE}-production-beta.apk"
            BUILD_TYPE_DIR="beta"
        else
            FILE_NAME="app-${FLAVOR_LOWERCASE}-production-release.apk"
            BUILD_TYPE_DIR="release"
        fi
        if [[ ${FLAVOR_LOWERCASE} == "normal" ]] 
        then
            FLAVOR_DIR="normalProduction"
        else
            FLAVOR_DIR="customProduction"
        fi
        FILE_DIR="${BASE_FILE_DIR}/${FLAVOR_DIR}/${BUILD_TYPE_DIR}"
    fi
    
    #---- ALIGN AND JARSIGN APK  -----
    ALIGNED_UNSIGNED_APK="${FILE_DIR}/aligned-${FILE_NAME}"
    ALIGNED_SIGNED_APK="${FILE_DIR}/aligned-signed-${FILE_NAME}"

    ${ANDROID_BUILD_TOOLS}/zipalign -v -p 4 "${FILE_DIR}/${FILE_NAME}" ${ALIGNED_UNSIGNED_APK} || quit
    ${ANDROID_BUILD_TOOLS}/apksigner sign --ks "${KEY_STORE_STRING}" --out ${ALIGNED_SIGNED_APK} ${ALIGNED_UNSIGNED_APK} || quit
    rm ${ALIGNED_UNSIGNED_APK}
    
    FINGERPRINT=$(unzip -p ${ALIGNED_SIGNED_APK} META-INF/*.RSA | keytool -printcert | grep "SHA256" | tr -d '[:space:]') || quit
    
    if [[ ${FINGERPRINT} == ${EXPECTED_FINGERPRINT} ]] 
    then
        echo "Certificate fingerprint matches: ${FINGERPRINT}"
    else 
        echo -e "Certificate fingerprint \n${FINGERPRINT} \ndid not match expected fingerprint \n\t${EXPECTED_FINGERPRINT}"
        quit
    fi
    
    #---- RENAME TO VERSION_NAME ----
    FINAL_APK=${ALIGNED_SIGNED_APK}
    if [[ -z ${VERSION_NAME} ]] 
    then
        VERSION_NAME="latest"
    fi

    if [[ ${BETA} == true ]]
    then
        FINAL_FILE_NAME="${APP_NAME}-Android-Beta-${VERSION_NAME}.apk"
    else 
        FINAL_FILE_NAME="${APP_NAME}-Android-${VERSION_NAME}.apk"
    fi
    FINAL_APK="${FILE_DIR}/${FINAL_FILE_NAME}"
    cp ${ALIGNED_SIGNED_APK} ${FINAL_APK} || quit
    cleanUp

    
    #---- GPG SIGNING ----
    if [[ -z ${GPG_KEY} && -z ${GPG_KEY_USER} ]] 
    then
        echo "WARNING: Could not do gpg signing!"
        exit
    fi
    
    if [[ ${GPG_KEY} ]] 
    then
        gpg --default-key ${GPG_KEY} --armor --output "${FINAL_APK}.sig" --detach-sign ${FINAL_APK} || quit
        #gpg -u ${GPG_KEY} -sab --output ${FINAL_APK} || quit
    else 
        GPG_KEY=$(gpg --list-keys $GPG_KEY_USER | grep pub | cut -d '/' -f 2 | cut -d ' ' -f 1) || quit
        #gpg -u ${GPG_KEY} -sab --output ${FINAL_APK} || quit
        gpg --default-key ${GPG_KEY} --armor --output "${FINAL_APK}.sig" --detach-sign ${FINAL_APK} || quit
    fi
    
    gpg --verify "${FINAL_APK}.sig" || quit
    
fi