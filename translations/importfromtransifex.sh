#!/bin/bash

function quit {
    echo "Task failed. Exit value: $?."
    cleanUp
    exit 1
}


# init parameters
if [[ -z $1 ]]
then
    echo -e "Invalid argument: no file. Check importfromtransifex.sh -h"
    exit
fi

for ((i=1;i<=$#;i++)); 
do
    if [[ ${!i} = "-h" || ${!i} = "-help" ]];
    then 
        echo -e "
        1. copy downloaded resource zip to this folder: `pwd`
        2. cd to `pwd`
        3. run ./importfromtransifex.sh <zipfile>
        "
        exit
    else
        FILE_NAME=${!i}
        if [[ ! -f $FILE_NAME ]]
        then 
            echo "Invalid argument: ${!i}"
            exit
        elif [[ ! $FILE_NAME == *.zip ]]
        then
            echo "Invalid argument: File seems not to be a zip file."
            exit
        fi
    fi
done;

if [[ -d strings ]]
then
    rm -r strings
fi

BITMASK_RESOURCES=../app/src/main/res
unzip ${FILE_NAME} -d strings
for STRING_FILE in strings/*; do
    if [[ ${STRING_FILE} == "strings/strings_en.xml" ]]
    then
        cp $STRING_FILE $BITMASK_RESOURCES/values/strings.xml
        continue
    fi
    
    LOCALE=`cut -d '_' -f2 <<< ${STRING_FILE}`
    SUBLOCALE=`cut -d '_' -f3 <<< ${STRING_FILE}`
    
    if [[ $LOCALE == *.xml ]]
    then
        LOCALE=${LOCALE/.xml/}
    else 
        SUBLOCALE=${SUBLOCALE/.xml/}
    fi
    
    # generalize sub localization for example for pt_PT -> pt
    if [[ `tr '[:upper:]' '[:lower:]' <<< $SUBLOCALE` == `tr '[:upper:]' '[:lower:]' <<< $LOCALE` ]]
    then
        SUBLOCALE=""
    fi
    
    #echo "copying $LOCALE $SUBLOCALE..."    
    
    if [[ ${SUBLOCALE} == "" ]] 
    then
        if [[ ! -d ${BITMASK_RESOURCES}/values-${LOCALE} ]]
        then
            echo "NEW LANGUAGE: ${LOCALE}"
            mkdir ${BITMASK_RESOURCES}/values-${LOCALE}
        fi
        cp ${STRING_FILE} ${BITMASK_RESOURCES}/values-${LOCALE}/strings.xml
    else
        if [[ ! -d ${BITMASK_RESOURCES}/values-${LOCALE}-r${SUBLOCALE} ]]
        then
            echo "NEW LANGUAGE: ${LOCALE} ${SUBLOCALE}"
            mkdir ${BITMASK_RESOURCES}/values-${LOCALE}-r${SUBLOCALE}
        fi
        cp ${STRING_FILE} ${BITMASK_RESOURCES}/values-${LOCALE}-r${SUBLOCALE}/strings.xml
    fi
done

rm -r strings