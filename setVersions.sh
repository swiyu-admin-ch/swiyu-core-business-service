#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: $0 <new-version> [-c|-f|-a <message>]"
    echo ""
    echo "Options:"
    echo "  -c <message>   Add a 'Changed' changelog entry"
    echo "  -f <message>   Add a 'Fixed' changelog entry"
    echo "  -a <message>   Add an 'Added' changelog entry"
    exit 1
fi

NEW_VERSION=$1
shift

CHANGE_TYPE=""
MESSAGE=""

while getopts "c:f:a:" opt; do
    case $opt in
        c) CHANGE_TYPE="Changed"; MESSAGE="$OPTARG" ;;
        f) CHANGE_TYPE="Fixed";   MESSAGE="$OPTARG" ;;
        a) CHANGE_TYPE="Added";   MESSAGE="$OPTARG" ;;
        *) echo "Unknown option. Usage: $0 <new-version> [-c|-f|-a <message>]"; exit 1 ;;
    esac
done

[[ "$NEW_VERSION" != *-SNAPSHOT ]] && NEW_VERSION="${NEW_VERSION}-SNAPSHOT"

BASE_VERSION="${NEW_VERSION%-SNAPSHOT}"
CHANGELOG="$(dirname "$0")/CHANGELOG.md"

if ! grep -q "^## ${BASE_VERSION}$" "$CHANGELOG"; then
    if [ -n "$CHANGE_TYPE" ] && [ -n "$MESSAGE" ]; then
        [[ "$MESSAGE" != -* ]] && MESSAGE="- ${MESSAGE}"
        sed -i "0,/^## /{s|^## |## ${BASE_VERSION}\n\n### ${CHANGE_TYPE}\n\n${MESSAGE}\n\n## |}" "$CHANGELOG"
    else
        echo "No changelog entry found for ${BASE_VERSION} — adding TODO placeholder."
        sed -i "0,/^## /{s|^## |## ${BASE_VERSION}\n\n### Changed\n\n- TODO: Add changelog entry for ${BASE_VERSION}\n\n## |}" "$CHANGELOG"
    fi
fi

# Update version in all modules (parent and children)
mvn versions:set -DnewVersion="$NEW_VERSION" -DprocessAllModules

# Remove backup files
mvn versions:commit

echo "Version updated to $NEW_VERSION in all modules"
