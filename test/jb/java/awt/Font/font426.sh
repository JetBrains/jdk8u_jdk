#!/bin/bash

#
# Copyright 2000-2017 JetBrains s.r.o.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# @test
# @summary font426.sh checks the Chinese font 华康彩带 (DFCaiDai)
# @run shell font426.sh

OS=`uname -s`
case "$OS" in
  Darwin)
    echo "Detected OS $OS"
    FONT_DIR="~/Library/Fonts"
    ;;
  * )
    echo "PASSED: The test is valid for MacOSX"
    exit 0;
    ;;
esac

if [ -z "${TESTSRC}" ]; then
  echo "TESTSRC undefined: set to ."
  TESTSRC=.
fi

cp -r ${TESTSRC}/fonts/DFCaiDai.ttf ${FONT_DIR}

if [ -z "${TESTCLASSES}" ]; then
  echo "TESTCLASSES undefined: set to ."
  TESTCLASSES=.
fi

if [ -z "${TESTJAVA}" ]; then
  echo "TESTJAVA undefined: testing cancelled"
  exit 1
fi

cd ${TESTSRC}
${TESTJAVA}/bin/javac -d ${TESTCLASSES} Font426.java

${TESTJAVA}/bin/java -cp ${TESTCLASSES} Font426

exit_code=$?

case $exit_code in
0) echo "PASSED"
   ;;
*) echo "FAILED: $exit_code"
   exit 1
   ;;
esac
exit 0
