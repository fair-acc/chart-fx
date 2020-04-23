#/bin/bash

#enforces .clang-format style guide prior to committing to the git repository

CLANG_MIN_VERSION="9.0.0"

set -e

CLANG_FORMAT="$(command -v clang-format)"
CLANG_VERSION="$(${CLANG_FORMAT} --version | sed '/^clang-format version /!d;s///;s/-.*//;s///g')"

compare_version () {
    echo " "
    if [[ $1 == $2 ]]
    then
        CLANG_MIN_VERSION_MATCH="="
        return
    fi
    local IFS=.
    local i ver1=($1) ver2=($2)
    # fill empty fields in ver1 with zeros
    for ((i=${#ver1[@]}; i<${#ver2[@]}; i++))
    do
        ver1[i]=0
    done
    for ((i=0; i<${#ver1[@]}; i++))
    do
        if [[ -z ${ver2[i]} ]]
        then
            # fill empty fields in ver2 with zeros
            ver2[i]=0
        fi
        if ((10#${ver1[i]} > 10#${ver2[i]}))
        then
            CLANG_MIN_VERSION_MATCH="<"
            return
        fi
        if ((10#${ver1[i]} < 10#${ver2[i]}))
        then
            CLANG_MIN_VERSION_MATCH=">"
            return
        fi
    done
    CLANG_MIN_VERSION_MATCH="="
    return
}

compare_version ${CLANG_MIN_VERSION} ${CLANG_VERSION}
git reset HEAD~1 --soft

files=$((git diff --name-only --cached | grep -Ei "\.(c|cc|cpp|cxx|c\+\+|h|hh|hpp|hxx|h\+\+|java)$") || true)
if [ -n "${files}" ]; then

    if [ -n "${CLANG_FORMAT}" ] && [ "$CLANG_MIN_VERSION_MATCH" != "<" ]; then
        spaced_files=$(echo "$files" | paste -s -d " " -)
        echo "reformatting ${spaced_files}"
        "${CLANG_FORMAT}" -style=file -i $spaced_files >/dev/null
        git --no-pager diff
        git add ${spaced_files}
    fi
fi
git commit -C ORIG_HEAD
