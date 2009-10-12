/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jdkim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.james.jdkim.exceptions.FailException;
import org.apache.james.mime4j.parser.MimeTokenStream;

/**
 * Creates a TestSuite running the test for each .msg file in the test resouce
 * folder. Allow running of a single test from Unit testing GUIs
 */
public class PerlDKIMTest extends TestCase {

    private File file;
    private MockPublicKeyRecordRetriever pkr;

    public PerlDKIMTest(String testName) throws IOException {
        this(testName, PerlDKIMTestSuite.getFile(testName),
                getPublicRecordRetriever());
    }

    public PerlDKIMTest(String name, File testFile,
            MockPublicKeyRecordRetriever pkr) {
        super(name);
        this.file = testFile;
        this.pkr = pkr;
    }

    public static MockPublicKeyRecordRetriever getPublicRecordRetriever()
            throws IOException {
        MockPublicKeyRecordRetriever pkr = new MockPublicKeyRecordRetriever();
        BufferedReader fakeDNSlist = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(
                                "main\\src\\test\\resources\\org\\apache\\james\\jdkim\\Mail-DKIM\\FAKE_DNS.dat")));
        String line;
        while ((line = fakeDNSlist.readLine()) != null) {
            if (!line.startsWith("#")) {
                int pDK = line.indexOf("._domainkey.");
                int pSp = line.indexOf(" ");

                if (line.charAt(pSp + 1) == ' ') {
                    pkr.addRecord(line.substring(0, pDK), line.substring(pDK
                            + "._domainkey.".length(), pSp), line
                            .substring(pSp + 2));
                } else {
                    if (line.substring(pSp + 1).startsWith("~~")) {
                        pkr.addRecord(line.substring(0, pDK), line.substring(
                                pDK + "._domainkey.".length(), pSp), null);
                    } else {
                        // NXDOMAIN can be ignored
                    }
                }
            }
        }
        return pkr;
    }

    protected void runTest() throws Throwable {
        MimeTokenStream stream = new MimeTokenStream();
        stream.setRecursionMode(MimeTokenStream.M_FLAT);
        // String checkFile =
        // "/org/apache/james/jdkim/bago/default_gfkresearch.com.eml";

        InputStream is = new FileInputStream(file);
        // String msgoutFile = file.getAbsolutePath().substring(0,
        // file.getAbsolutePath().lastIndexOf('.')) + ".out";

        pkr = getPublicRecordRetriever();

        boolean expectFailure = false;
        // DomainKey files
        if (getName().indexOf("dk_") != -1)
            expectFailure = true;
        // older spec version
        else if (getName().indexOf("_ietf") != -1)
            expectFailure = true;
        else if (getName().startsWith("multiple_1"))
            expectFailure = true;
        else if (getName().startsWith("no_body"))
            expectFailure = true;
        // invalid or inapplicable
        else if (getName().startsWith("badkey_"))
            expectFailure = true;
        else if (getName().startsWith("ignore_"))
            expectFailure = true;
        else if (getName().startsWith("bad_"))
            expectFailure = true;

        try {
            new DKIMVerifier(pkr).verify(is);
            if (expectFailure)
                fail("Failure expected!");
        } catch (FailException e) {
            if (!expectFailure)
                fail(e.getMessage());
        }
    }

    public static Test suite() throws IOException {
        return new PerlDKIMTestSuite();
    }

    static class PerlDKIMTestSuite extends TestSuite {

        private static final File TESTS_FOLDER = new File(
                "main\\src\\test\\resources\\org\\apache\\james\\jdkim\\Mail-DKIM\\corpus");

        public PerlDKIMTestSuite() throws IOException {
            super();
            File dir = TESTS_FOLDER;
            File[] files = dir.listFiles();

            if (files != null)
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    if (f.getName().toLowerCase().endsWith(".txt")) {
                        addTest(new PerlDKIMTest(f.getName().substring(0,
                                f.getName().length() - 4), f,
                                getPublicRecordRetriever()));
                    }
                }
        }

        public static File getFile(String name) {
            return new File(TESTS_FOLDER.getAbsolutePath() + File.separator
                    + name + ".txt");
        }

    }
}