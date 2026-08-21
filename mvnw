#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
M2_HOME="${SCRIPT_DIR}/.mvn/install/apache-maven-3.9.9"
MAVEN_TARBALL_URL="https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz"
MAVEN_TARBALL="${SCRIPT_DIR}/.mvn/install/apache-maven-3.9.9-bin.tar.gz"

if [ ! -d "$M2_HOME" ]; then
    echo "Maven 3.9.9 not found. Downloading..."
    mkdir -p "${SCRIPT_DIR}/.mvn/install"
    curl -fsSL -o "$MAVEN_TARBALL" "$MAVEN_TARBALL_URL"
    tar -xzf "$MAVEN_TARBALL" -C "${SCRIPT_DIR}/.mvn/install/"
    rm "$MAVEN_TARBALL"
fi

exec "$M2_HOME/bin/mvn" "$@"
