/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.appsecurity.cts;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.ZipUtil;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Tests for APEX signature verification to ensure preloaded APEXes
 * DO NOT signed with well-known keys.
 */
@RunWith(DeviceJUnit4ClassRunner.class)
public class ApexSignatureVerificationTest extends BaseHostJUnit4Test {

    private static final String TEST_BASE = "ApexSignatureVerificationTest";
    private static final String TEST_APEX_SOURCE_DIR_PREFIX = "tests-apex_";
    private static final String APEX_PUB_KEY_NAME = "apex_pubkey";

    private static final Pattern WELL_KNOWN_PUBKEY_PATTERN = Pattern.compile(
            "^apexsigverify\\/.*.avbpubkey");

    private static boolean mHasTestFailure;

    private static File mBasePath;
    private static File mWellKnownKeyStorePath;
    private static File mArchiveZip;

    private static Map<String, String> mPreloadedApexPathMap = new HashMap<>();
    private static Map<String, File> mLocalApexFileMap = new HashMap<>();
    private static Map<String, File> mExtractedTestDirMap = new HashMap<>();
    private static List<File> mWellKnownKeyFileList = new ArrayList<>();
    private ITestDevice mDevice;

    @Before
    public void setUp() throws Exception {
        mDevice = getDevice();
        if (mBasePath == null && mWellKnownKeyStorePath == null
                && mExtractedTestDirMap.size() == 0) {
            mBasePath = FileUtil.createTempDir(TEST_BASE);
            mBasePath.deleteOnExit();
            mWellKnownKeyStorePath = FileUtil.createTempDir("wellknownsignatures", mBasePath);
            mWellKnownKeyStorePath.deleteOnExit();
            pullWellKnownSignatures();
            getApexPackageList();
            pullApexFiles();
            extractApexFiles();
        }
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        if (mArchiveZip == null && mHasTestFailure) {
            // Archive all operation data and materials in host
            // /tmp/ApexSignatureVerificationTest.zip
            // in case the test result is not expected and need to debug.
            mArchiveZip = ZipUtil.createZip(mBasePath, mBasePath.getName());
        }
    }

    @Rule
    public final OnFailureRule mDumpOnFailureRule = new OnFailureRule() {
        @Override
        protected void onTestFailure(Statement base, Description description, Throwable t) {
            mHasTestFailure = true;
        }
    };

    @Test
    public void testApexIncludePubKey() {
        for (Map.Entry<String, File> entry : mExtractedTestDirMap.entrySet()) {
            final File pubKeyFile = FileUtil.findFile(entry.getValue(), APEX_PUB_KEY_NAME);

            assertWithMessage("apex:" + entry.getKey() + " do not contain pubkey").that(
                    pubKeyFile.exists()).isTrue();
        }
    }

    @Test
    public void testApexPubKeyIsNotWellKnownKey() {
        assertThat(mWellKnownKeyFileList).isNotNull();

        for (Map.Entry<String, File> entry : mExtractedTestDirMap.entrySet()) {
            final File pubKeyFile = FileUtil.findFile(entry.getValue(), APEX_PUB_KEY_NAME);
            final Iterator it = mWellKnownKeyFileList.iterator();

            assertThat(pubKeyFile).isNotNull();

            while (it.hasNext()) {
                final File wellKnownKey = (File) it.next();

                try {
                    assertWithMessage("Well-known key:" + wellKnownKey.getName() + ", match apex:"
                            + entry.getKey()).that(
                            FileUtil.compareFileContents(pubKeyFile, wellKnownKey)).isFalse();
                } catch (IOException e) {
                    throw new AssertionError("compareFileContents IOException" + e);
                }
            }
        }
    }

    @Ignore
    @Test
    public void testApexPubKeyMatchPayloadImg() {
        // TODO(b/142919428): Need more investigation to find a way verify apex_paylaod.img
        //                    was signed by apex_pubkey
    }

    private void extractApexFiles() {
        final String subFilesFilter = "\\w+.*";

        try {
            for (Map.Entry<String, File> entry : mLocalApexFileMap.entrySet()) {
                final String testSrcDirPath = TEST_APEX_SOURCE_DIR_PREFIX + entry.getKey();
                File apexDir = FileUtil.createTempDir(testSrcDirPath, mBasePath);
                apexDir.deleteOnExit();
                ZipUtil.extractZip(new ZipFile(entry.getValue()), apexDir);

                assertThat(apexDir).isNotNull();

                mExtractedTestDirMap.put(entry.getKey(), apexDir);

                assertThat(FileUtil.findFiles(apexDir, subFilesFilter)).isNotNull();
            }
        } catch (IOException e) {
            throw new AssertionError("extractApexFile IOException" + e);
        }
    }

    private void getApexPackageList() {
        Set<ITestDevice.ApexInfo> apexes;
        try {
            apexes = mDevice.getActiveApexes();
            for (ITestDevice.ApexInfo ap : apexes) {
                mPreloadedApexPathMap.put(ap.name, ap.sourceDir);
            }

            assertThat(mPreloadedApexPathMap.size()).isAtLeast(0);
        } catch (DeviceNotAvailableException e) {
            throw new AssertionError("getApexPackageList DeviceNotAvailableException" + e);
        }
    }

    private static Collection<String> getResourcesFromJarFile(final File file,
            final Pattern pattern) {
        final ArrayList<String> candidateList = new ArrayList<>();
        ZipFile zf;
        try {
            zf = new ZipFile(file);
            assertThat(zf).isNotNull();
        } catch (final ZipException e) {
            throw new AssertionError("Query Jar file ZipException" + e);
        } catch (final IOException e) {
            throw new AssertionError("Query Jar file IOException" + e);
        }
        final Enumeration e = zf.entries();
        while (e.hasMoreElements()) {
            final ZipEntry ze = (ZipEntry) e.nextElement();
            final String fileName = ze.getName();
            final boolean isMatch = pattern.matcher(fileName).matches();
            if (isMatch) {
                candidateList.add(fileName);
            }
        }
        try {
            zf.close();
        } catch (final IOException e1) {
        }
        return candidateList;
    }

    private void pullApexFiles() {
        try {
            for (Map.Entry<String, String> entry : mPreloadedApexPathMap.entrySet()) {
                final File localTempFile = File.createTempFile(entry.getKey(), "", mBasePath);

                assertThat(localTempFile).isNotNull();
                assertThat(mDevice.pullFile(entry.getValue(), localTempFile)).isTrue();

                mLocalApexFileMap.put(entry.getKey(), localTempFile);
            }
        } catch (DeviceNotAvailableException e) {
            throw new AssertionError("pullApexFile DeviceNotAvailableException" + e);
        } catch (IOException e) {
            throw new AssertionError("pullApexFile IOException" + e);
        }
    }

    private void pullWellKnownSignatures() {
        final Collection<String> keyPath;

        try {
            File jarFile = new File(
                    this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            keyPath = getResourcesFromJarFile(jarFile, WELL_KNOWN_PUBKEY_PATTERN);

            assertThat(keyPath).isNotNull();
        } catch (URISyntaxException e) {
            throw new AssertionError("Iterate well-known key name from jar IOException" + e);
        }

        Iterator<String> keyIterator = keyPath.iterator();
        while (keyIterator.hasNext()) {
            final String tmpKeyPath = keyIterator.next();
            final String keyFileName = tmpKeyPath.substring(tmpKeyPath.lastIndexOf("/"));
            File outFile;
            try (InputStream in = getClass().getResourceAsStream("/" + tmpKeyPath)) {
                outFile = File.createTempFile(keyFileName, "", mWellKnownKeyStorePath);
                mWellKnownKeyFileList.add(outFile);
                FileUtil.writeToFile(in, outFile);
            } catch (IOException e) {
                throw new AssertionError("Copy well-known keys to tmp IOException" + e);
            }
        }
    }

    /**
     * Custom JUnit4 rule that provides a callback upon test failures.
     */
    public abstract class OnFailureRule implements TestRule {
        public OnFailureRule() {
        }

        @Override
        public Statement apply(Statement base, Description description) {
            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    try {
                        base.evaluate();
                    } catch (Throwable t) {
                        onTestFailure(base, description, t);
                        throw t;
                    }
                }
            };
        }

        protected abstract void onTestFailure(Statement base, Description description, Throwable t);
    }
}