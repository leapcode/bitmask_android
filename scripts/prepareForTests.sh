#!/bin/bash

# Copyright (c) 2022 LEAP Encryption Access Project and contributors
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

SCRIPT_DIR=$(dirname "$0")
BASE_DIR="$SCRIPT_DIR/.."

git checkout -- \*
git checkout -- \.\*
rm -r $BASE_DIR/bitmaskcore/lib/*
git submodule foreach --recursive git reset --hard HEAD
git submodule sync --recursive
git submodule update --init --recursive

BUILD_TOR=false BUILD_OPENVPN_LIBS=false ./scripts/build_deps.sh