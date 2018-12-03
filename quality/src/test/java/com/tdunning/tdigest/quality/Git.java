/*
 * Licensed to Ted Dunning under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tdunning.tdigest.quality;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Functions for probing Git. Handy for marking test results against git hashes.
 */
class Git {
    private static boolean isGitClean() {
        try {
            return new ProcessBuilder("git", "diff-index", "--quiet", "HEAD", "--")
                    .redirectOutput(new File("/dev/null"))
                    .start()
                    .exitValue() == 0;
        } catch (IOException e) {
            return false;
        }
    }

    static String getHash(boolean force) throws IOException {
        if (force || isGitClean()) {
            Process p = new ProcessBuilder("git", "log", "-1")
                    .start();
            BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // output should look like "commit 01ea144ca865361be6786fd502bb554c75105e3c"
            return stdout.readLine().substring(7);
        } else {
            throw new IOException("Source directory has changes that need to be committed");
        }
    }
}
