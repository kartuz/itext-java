/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2022 iText Group NV
    Authors: iText Software.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.signatures.verify;

import com.itextpdf.bouncycastleconnector.BouncyCastleFactoryCreator;
import com.itextpdf.commons.bouncycastle.IBouncyCastleFactory;
import com.itextpdf.commons.bouncycastle.operator.AbstractOperatorCreationException;
import com.itextpdf.commons.bouncycastle.pkcs.AbstractPKCSException;
import com.itextpdf.commons.bouncycastle.tsp.ITimeStampToken;
import com.itextpdf.commons.utils.DateTimeUtil;
import com.itextpdf.commons.utils.MessageFormatUtil;
import com.itextpdf.signatures.CertificateVerification;
import com.itextpdf.signatures.SignaturesTestUtils;
import com.itextpdf.signatures.VerificationException;
import com.itextpdf.signatures.exceptions.SignExceptionMessageConstant;
import com.itextpdf.signatures.testutils.PemFileHelper;
import com.itextpdf.signatures.testutils.SignTestPortUtil;
import com.itextpdf.signatures.testutils.builder.TestCrlBuilder;
import com.itextpdf.signatures.testutils.client.TestCrlClient;
import com.itextpdf.signatures.testutils.client.TestTsaClient;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.ITextTest;
import com.itextpdf.test.annotations.LogMessage;
import com.itextpdf.test.annotations.LogMessages;
import com.itextpdf.test.annotations.type.BouncyCastleUnitTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(BouncyCastleUnitTest.class)
public class CertificateVerificationClassTest extends ExtendedITextTest {
    private static final IBouncyCastleFactory FACTORY = BouncyCastleFactoryCreator.getFactory();

    private static final Provider PROVIDER = FACTORY.getProvider();

    // Such messageTemplate is equal to any log message. This is required for porting reasons.
    private static final String ANY_LOG_MESSAGE = "{0}";
    private static final int COUNTER_TO_MAKE_CRL_AVAILABLE_AT_THE_CURRENT_TIME = -1;

    private static final String CERTS_SRC = "./src/test/resources/com/itextpdf/signatures/certs/";
    private static final char[] PASSWORD = "testpass".toCharArray();

    @BeforeClass
    public static void before() {
        Security.addProvider(PROVIDER);
        ITextTest.removeCryptographyRestrictions();
    }

    @AfterClass
    public static void after() {
        ITextTest.restoreCryptographyRestrictions();
    }

    @Test
    public void validCertificateChain01()
            throws GeneralSecurityException, IOException, AbstractPKCSException, AbstractOperatorCreationException {
        Certificate[] certChain = PemFileHelper.readFirstChain(CERTS_SRC + "signCertRsaWithChain.pem");

        String caCertFileName = CERTS_SRC + "rootRsa.pem";
        KeyStore caKeyStore = PemFileHelper.initStore(caCertFileName, PASSWORD, PROVIDER);

        List<VerificationException> verificationExceptions = CertificateVerification.verifyCertificates(certChain, caKeyStore);

        Assert.assertTrue(verificationExceptions.isEmpty());
    }

    @Test
    public void timestampCertificateAndKeyStoreCorrespondTest() throws Exception {
        String tsaCertFileName = CERTS_SRC + "tsCertRsa.pem";

        KeyStore caKeyStore = PemFileHelper.initStore(tsaCertFileName, PASSWORD, PROVIDER);

        Assert.assertTrue(verifyTimestampCertificates(tsaCertFileName, caKeyStore));
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = "certificate hash does not match certID hash."))
    public void timestampCertificateAndKeyStoreDoNotCorrespondTest() throws Exception {
        String tsaCertFileName = CERTS_SRC + "tsCertRsa.pem";
        String notTsaCertFileName = CERTS_SRC + "rootRsa.pem";

        KeyStore caKeyStore = PemFileHelper.initStore(notTsaCertFileName, PASSWORD, PROVIDER);

        Assert.assertFalse(verifyTimestampCertificates(tsaCertFileName, caKeyStore));
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = ANY_LOG_MESSAGE))
    public void keyStoreWithoutCertificatesTest() throws Exception {
        String tsaCertFileName = CERTS_SRC + "tsCertRsa.pem";

        Assert.assertFalse(verifyTimestampCertificates(tsaCertFileName, null));
    }

    @Test
    public void expiredCertificateTest() throws CertificateException, IOException {

        final X509Certificate expiredCert =
                (X509Certificate) PemFileHelper.readFirstChain(CERTS_SRC + "expiredCert.pem")[0];

        final String verificationResult = CertificateVerification.verifyCertificate(expiredCert, null);
        final String expectedResultString = SignaturesTestUtils.getExpiredMessage(expiredCert);

        Assert.assertEquals(expectedResultString, verificationResult);
    }

    @Test
    public void unsupportedCriticalExtensionTest() throws CertificateException, IOException {

        final X509Certificate unsupportedExtensionCert = (X509Certificate) PemFileHelper.readFirstChain(CERTS_SRC + "unsupportedCriticalExtensionCert.pem")[0];

        final String verificationResult = CertificateVerification.verifyCertificate(unsupportedExtensionCert, null);

        Assert.assertEquals(CertificateVerification.HAS_UNSUPPORTED_EXTENSIONS, verificationResult);
    }

    @Test
    public void clrWithGivenCertificateTest()
            throws CertificateException, IOException, CRLException, AbstractPKCSException, AbstractOperatorCreationException {

        final String caCertFileName = CERTS_SRC + "rootRsa.pem";
        X509Certificate caCert = (X509Certificate) PemFileHelper.readFirstChain(caCertFileName)[0];
        PrivateKey caPrivateKey = PemFileHelper.readFirstKey(caCertFileName, PASSWORD);

        final String checkCertFileName = CERTS_SRC + "signCertRsa01.pem";
        X509Certificate checkCert = (X509Certificate) PemFileHelper.readFirstChain(checkCertFileName)[0];

        TestCrlBuilder crlBuilder = new TestCrlBuilder(caCert, caPrivateKey,
                DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(),
                        COUNTER_TO_MAKE_CRL_AVAILABLE_AT_THE_CURRENT_TIME));
        crlBuilder.addCrlEntry(caCert, DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(),
                COUNTER_TO_MAKE_CRL_AVAILABLE_AT_THE_CURRENT_TIME), FACTORY.createCRLReason().getKeyCompromise());

        TestCrlBuilder crlForCheckBuilder = new TestCrlBuilder(caCert, caPrivateKey,
                DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(),
                        COUNTER_TO_MAKE_CRL_AVAILABLE_AT_THE_CURRENT_TIME));
        crlForCheckBuilder.addCrlEntry(checkCert, DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(),
                COUNTER_TO_MAKE_CRL_AVAILABLE_AT_THE_CURRENT_TIME), FACTORY.createCRLReason().getKeyCompromise());

        TestCrlClient crlClient = new TestCrlClient().addBuilderForCertIssuer(crlBuilder);
        TestCrlClient crlForCheckClient = new TestCrlClient().addBuilderForCertIssuer(crlForCheckBuilder);

        Collection<byte[]> crlBytesForRootCertCollection = crlClient.getEncoded(caCert, null);
        Collection<byte[]> crlBytesForCheckCertCollection = crlForCheckClient.getEncoded(checkCert, null);

        List<CRL> crls = new ArrayList<>();
        for (byte[] crlBytes : crlBytesForRootCertCollection) {
            crls.add(SignTestPortUtil.parseCrlFromStream(new ByteArrayInputStream(crlBytes)));
        }
        for (byte[] crlBytes : crlBytesForCheckCertCollection) {
            crls.add(SignTestPortUtil.parseCrlFromStream(new ByteArrayInputStream(crlBytes)));
        }

        final String verificationResult = CertificateVerification.verifyCertificate(checkCert, crls);

        Assert.assertEquals(CertificateVerification.CERTIFICATE_REVOKED, verificationResult);
    }

    @Test
    public void validCertWithEmptyCrlCollectionTest() throws CertificateException, IOException {
        final String caCertFileName = CERTS_SRC + "rootRsa.pem";
        X509Certificate rootCert = (X509Certificate) PemFileHelper.readFirstChain(caCertFileName)[0];

        final String verificationResult = CertificateVerification.verifyCertificate(rootCert, Collections.<CRL>emptyList());

        Assert.assertNull(verificationResult);
    }

    @Test
    public void validCertWithCrlDoesNotContainCertTest()
            throws CertificateException, IOException, CRLException, AbstractPKCSException, AbstractOperatorCreationException {
        final int COUNTER_TO_MAKE_CRL_AVAILABLE_AT_THE_CURRENT_TIME = -1;
        final String rootCertFileName = CERTS_SRC + "rootRsa.pem";
        X509Certificate rootCert = (X509Certificate) PemFileHelper.readFirstChain(rootCertFileName)[0];

        final String certForAddingToCrlName = CERTS_SRC + "signCertRsa01.pem";
        X509Certificate certForCrl = (X509Certificate) PemFileHelper.readFirstChain(certForAddingToCrlName)[0];
        PrivateKey caPrivateKey = PemFileHelper.readFirstKey(certForAddingToCrlName, PASSWORD);

        TestCrlBuilder crlForCheckBuilder = new TestCrlBuilder(certForCrl, caPrivateKey,
                DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(),
                        COUNTER_TO_MAKE_CRL_AVAILABLE_AT_THE_CURRENT_TIME));

        TestCrlClient crlClient = new TestCrlClient().addBuilderForCertIssuer(crlForCheckBuilder);

        Collection<byte[]> crlBytesForRootCertCollection = crlClient.getEncoded(certForCrl, null);

        final List<CRL> crls = new ArrayList<>();
        for (byte[] crlBytes : crlBytesForRootCertCollection) {
            crls.add(SignTestPortUtil.parseCrlFromStream(new ByteArrayInputStream(crlBytes)));
        }

        Assert.assertNull(CertificateVerification.verifyCertificate(rootCert, crls));
    }

    @Test
    public void emptyCertChainTest() throws CertificateEncodingException, IOException {
        Certificate[] emptyCertChain = new Certificate[] {};
        final String expectedResult = MessageFormatUtil.format("Certificate Unknown failed: {0}",
                SignExceptionMessageConstant.INVALID_STATE_WHILE_CHECKING_CERT_CHAIN);

        List<VerificationException> resultedExceptionList = CertificateVerification.verifyCertificates(emptyCertChain,
                null, (Collection<CRL>) null);

        Assert.assertEquals(1, resultedExceptionList.size());
        Assert.assertEquals(expectedResult, resultedExceptionList.get(0).getMessage());
    }

    @Test
    public void validCertChainWithEmptyKeyStoreTest()
            throws GeneralSecurityException, IOException, AbstractPKCSException, AbstractOperatorCreationException {
        final String validCertChainFileName = CERTS_SRC + "signCertRsaWithChain.pem";
        final String emptyCertChain = CERTS_SRC + "emptyCertChain.pem";

        Certificate[] validCertChain = PemFileHelper.readFirstChain(validCertChainFileName);
        KeyStore emptyKeyStore = PemFileHelper.initStore(emptyCertChain, PASSWORD, PROVIDER);

        List<VerificationException> resultedExceptionList = CertificateVerification.verifyCertificates(validCertChain,
                emptyKeyStore, (Collection<CRL>) null);

        final String expectedResult = MessageFormatUtil.format(
                SignExceptionMessageConstant.CERTIFICATE_TEMPLATE_FOR_EXCEPTION_MESSAGE,
                FACTORY.createX500Name((X509Certificate) validCertChain[2]).toString(),
                SignExceptionMessageConstant.CANNOT_BE_VERIFIED_CERTIFICATE_CHAIN);

        Assert.assertEquals(1, resultedExceptionList.size());
        Assert.assertEquals(expectedResult, resultedExceptionList.get(0).getMessage());
    }

    @Test
    public void validCertChainWithRootCertAsKeyStoreTest()
            throws GeneralSecurityException, IOException, AbstractPKCSException, AbstractOperatorCreationException {
        final String validCertChainFileName = CERTS_SRC + "signCertRsaWithChain.pem";
        final String emptyCertChain = CERTS_SRC + "rootRsa.pem";

        Certificate[] validCertChain = PemFileHelper.readFirstChain(validCertChainFileName);
        KeyStore emptyKeyStore = PemFileHelper.initStore(emptyCertChain, PASSWORD, PROVIDER);

        List<VerificationException> resultedExceptionList = CertificateVerification.verifyCertificates(validCertChain,
                emptyKeyStore, (Collection<CRL>) null);

        Assert.assertEquals(0, resultedExceptionList.size());
    }

    @Test
    public void certChainWithExpiredCertTest()
            throws CertificateException, IOException {
        final String validCertChainFileName = CERTS_SRC + "signCertRsaWithExpiredChain.pem";

        Certificate[] validCertChain = PemFileHelper.readFirstChain(validCertChainFileName);

        X509Certificate expectedExpiredCert = (X509Certificate) validCertChain[1];
        final String expiredCertName = FACTORY.createX500Name(expectedExpiredCert).toString();
        X509Certificate rootCert = (X509Certificate) validCertChain[2];
        final String rootCertName = FACTORY.createX500Name(rootCert).toString();

        List<VerificationException> resultedExceptionList = CertificateVerification.verifyCertificates(validCertChain,
                null, (Collection<CRL>) null);

        Assert.assertEquals(2, resultedExceptionList.size());
        final String expectedFirstResultMessage = MessageFormatUtil.format(
                SignExceptionMessageConstant.CERTIFICATE_TEMPLATE_FOR_EXCEPTION_MESSAGE,
                expiredCertName, SignaturesTestUtils.getExpiredMessage(expectedExpiredCert));
        final String expectedSecondResultMessage = MessageFormatUtil.format(
                SignExceptionMessageConstant.CERTIFICATE_TEMPLATE_FOR_EXCEPTION_MESSAGE,
                rootCertName, SignExceptionMessageConstant.CANNOT_BE_VERIFIED_CERTIFICATE_CHAIN);

        Assert.assertEquals(expectedFirstResultMessage, resultedExceptionList.get(0).getMessage());
        Assert.assertEquals(expectedSecondResultMessage, resultedExceptionList.get(1).getMessage());
    }

    private static boolean verifyTimestampCertificates(String tsaClientCertificate, KeyStore caKeyStore)
            throws Exception {
        Certificate[] tsaChain = PemFileHelper.readFirstChain(tsaClientCertificate);
        PrivateKey tsaPrivateKey = PemFileHelper.readFirstKey(tsaClientCertificate, PASSWORD);

        TestTsaClient testTsaClient = new TestTsaClient(Arrays.asList(tsaChain), tsaPrivateKey);

        byte[] tsaCertificateBytes = testTsaClient.getTimeStampToken(testTsaClient.getMessageDigest().digest());
        ITimeStampToken timeStampToken = FACTORY.createTimeStampToken(
                FACTORY.createContentInfo(FACTORY.createASN1Sequence(tsaCertificateBytes)));

        return CertificateVerification.verifyTimestampCertificates(timeStampToken, caKeyStore, null);
    }
}
