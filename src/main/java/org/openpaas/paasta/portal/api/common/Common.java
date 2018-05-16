package org.openpaas.paasta.portal.api.common;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.identity.uaa.api.UaaConnectionFactory;
import org.cloudfoundry.identity.uaa.api.client.UaaClientOperations;
import org.cloudfoundry.identity.uaa.api.common.UaaConnection;
import org.cloudfoundry.identity.uaa.api.group.UaaGroupOperations;
import org.cloudfoundry.identity.uaa.api.user.UaaUserOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.ClientCredentialsGrantTokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;
import org.openpaas.paasta.portal.api.config.cloudfoundry.provider.TokenGrantTokenProvider;
import org.openpaas.paasta.portal.api.service.LoginService;
import org.openpaas.paasta.portal.api.util.SSLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Common {

    private static final Logger LOGGER = LoggerFactory.getLogger(Common.class);

    @Value("${cloudfoundry.cc.api.url}")
    public String apiTarget;

    @Value("${cloudfoundry.cc.api.uaaUrl}")
    public String uaaTarget;

    @Value("${cloudfoundry.cc.api.sslSkipValidation}")
    public boolean cfskipSSLValidation;

    @Value("${cloudfoundry.user.admin.username}")
    public String adminUserName;

    @Value("${cloudfoundry.user.admin.password}")
    public String adminPassword;

    public static final String AUTHORIZATION_HEADER_KEY = "cf-Authorization";

    @Value("${cloudfoundry.user.uaaClient.clientId}")
    public String uaaClientId;

    @Value("${cloudfoundry.user.uaaClient.clientSecret}")
    public String uaaClientSecret;

    @Value("${cloudfoundry.user.uaaClient.adminClientId}")
    public String uaaAdminClientId;

    @Value("${cloudfoundry.user.uaaClient.adminClientSecret}")
    public String uaaAdminClientSecret;

    @Value("${cloudfoundry.user.uaaClient.loginClientId}")
    public String uaaLoginClientId;

    @Value("${cloudfoundry.user.uaaClient.loginClientSecret}")
    public String uaaLoginClientSecret;

    @Value("${cloudfoundry.user.uaaClient.skipSSLValidation}")
    public boolean skipSSLValidation;

    @Value("${monitoring.api.url}")
    public String monitoringApiTarget;


    @Autowired
    private LoginService loginService;

    public String getToken() throws MalformedURLException, URISyntaxException {
        return loginService.login(adminUserName, adminPassword).getValue();
    }

    public URL getTargetURL(String target) throws MalformedURLException, URISyntaxException {
        return getTargetURI(target).toURL();
    }

    private URI getTargetURI(String target) throws URISyntaxException {
        return new URI(target);
    }

    /**
     * get CloudFoundryClinet Object from token String
     *
     * @param token
     * @return CloudFoundryClinet
     */
    public CloudFoundryClient getCloudFoundryClient(String token) throws MalformedURLException, URISyntaxException {

        return new CloudFoundryClient(getCloudCredentials(token), getTargetURL(apiTarget), true);
    }

    /**
     * get CloudFoundryClinet Object from token String
     * with organization, space
     *
     * @param token
     * @param organization
     * @param space
     * @return CloudFoundryClinet
     */
    public CloudFoundryClient getCloudFoundryClient(String token, String organization, String space) throws MalformedURLException, URISyntaxException {

        return new CloudFoundryClient(getCloudCredentials(token), getTargetURL(apiTarget), organization, space, true);
    }

    /**
     * get CloudFoundryClinet Object from id, password
     *
     * @param id
     * @param password
     * @return CloudFoundryClinet
     */
    public CloudFoundryClient getCloudFoundryClient(String id, String password) throws MalformedURLException, URISyntaxException {
        return new CloudFoundryClient(getCloudCredentials(id, password), getTargetURL(apiTarget), true);
    }

    /**
     * get CloudFoundryClinet Object from token String
     * with organization, space
     *
     * @param id
     * @param password
     * @param organization
     * @param space
     * @return CloudFoundryClinet
     */
    public CloudFoundryClient getCloudFoundryClient(String id, String password, String organization, String space) throws MalformedURLException, URISyntaxException {

        return new CloudFoundryClient(getCloudCredentials(id, password), getTargetURL(apiTarget), organization, space, true);
    }


    /**
     * get CloudCredentials Object from token String
     *
     * @param token
     * @return CloudCredentials
     */
    public CloudCredentials getCloudCredentials(String token) {
        return new CloudCredentials(getOAuth2AccessToken(token), false);
    }

    /**
     * get CloudCredentials Object from id, password
     *
     * @param id
     * @param password
     * @return CloudCredentials
     */
    public CloudCredentials getCloudCredentials(String id, String password) {

        LOGGER.info("============getCloudCredentials==============");
        CloudCredentials test = new CloudCredentials(id, password);
        LOGGER.info("getToken       :" + test.getToken());
        LOGGER.info("getClientId    :" + test.getClientId());
        LOGGER.info("getClientSecret:" + test.getClientSecret());
        LOGGER.info("getEmail       :" + test.getEmail());
        LOGGER.info("getPassword    :" + test.getPassword());
        LOGGER.info("getProxyUser   :" + test.getProxyUser());
        return test;
//        return new CloudCredentials(id, password);
    }

    /**
     * get DefailtOAuth2AccessToken Object from token String
     *
     * @param token
     * @return
     */
    private DefaultOAuth2AccessToken getOAuth2AccessToken(String token) {
        return new DefaultOAuth2AccessToken(token);
    }

    /* 회원 생성시 사용(메일) */
    public UaaUserOperations getUaaUserOperations(String uaaClientId) throws Exception {
        UaaConnection connection = getUaaConnection(uaaClientId);
        return connection.userOperations();
    }

    /* 권한그룹 조회 등록시 사용 */
    public UaaGroupOperations getUaaGroupOperations(String uaaClientId) throws Exception {
        UaaConnection connection = getUaaConnection(uaaClientId);
        return connection.groupOperations();
    }

    public UaaClientOperations getUaaClientOperations(String uaaClientId) throws Exception {
        UaaConnection connection = getUaaConnection(uaaClientId);
        return connection.clientOperations();
    }

    //uaa 커넥션 생성
    private UaaConnection getUaaConnection(String uaaClientId) throws Exception {

        ResourceOwnerPasswordResourceDetails credentials = getCredentials(uaaClientId);
        URL uaaHost = new URL(uaaTarget);

        //ssl 유효성 체크 비활성
        if (skipSSLValidation) {
            SSLUtils.turnOffSslChecking();
        }

        UaaConnection connection = UaaConnectionFactory.getConnection(uaaHost, credentials);
        return connection;
    }

    //credentials 세팅
    private ResourceOwnerPasswordResourceDetails getCredentials(String uaaClientId) {
        ResourceOwnerPasswordResourceDetails credentials = new ResourceOwnerPasswordResourceDetails();
        credentials.setAccessTokenUri(uaaTarget + "/oauth/token?grant_type=client_credentials&response_type=token");
        credentials.setClientAuthenticationScheme(AuthenticationScheme.header);

        credentials.setClientId(uaaClientId);

        if (uaaClientId.equals(uaaAdminClientId)) {
            credentials.setClientSecret(uaaAdminClientSecret);
        } else if (uaaClientId.equals(uaaLoginClientId)) {
            credentials.setClientSecret(uaaLoginClientSecret);
        }

        //credentials.setUsername(adminUserName);
        //credentials.setPassword(adminPassword);
        return credentials;
    }


    /**
     * 요청 파라미터들의 빈값 또는 null값 확인을 하나의 메소드로 처리할 수 있도록 생성한 메소드
     * 요청 파라미터 중 빈값 또는 null값인 파라미터가 있는 경우, false를 리턴한다.
     *
     * @param params
     * @return
     */
    public boolean stringNullCheck(String... params) {
        return Arrays.stream(params).allMatch(param -> null != param && !param.equals(""));
    }

    //요청 문자열 파라미터 중, 공백을 포함하고 있는 파라미터가 있을 경우 false를 리턴
    public boolean stringContainsSpaceCheck(String... params) {
        return Arrays.stream(params).allMatch(param -> !param.contains(" "));
    }

    /**
     * Gets property value.
     *
     * @param key the key
     * @return property value
     * @throws Exception the exception
     */
    public static String getPropertyValue(String key) throws Exception {
        return getPropertyValue(key, "/config.properties");
    }


    /**
     * Gets process property value.
     *
     * @param key            the key
     * @param configFileName the config file name
     * @return property value
     * @throws Exception the exception
     */
    private static String getProcPropertyValue(String key, String configFileName) throws Exception {
        if (Constants.NONE_VALUE.equals(configFileName)) return "";

        Properties prop = new Properties();

        try (InputStream inputStream = ClassLoader.class.getResourceAsStream(configFileName)) {
            prop.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return prop.getProperty(key);
    }

    /**
     * Gets property value.
     *
     * @param key            the key
     * @param configFileName the config file name
     * @return property value
     * @throws Exception the exception
     */
    public static String getPropertyValue(String key, String configFileName) throws Exception {
        return getProcPropertyValue(key, Optional.ofNullable(configFileName).orElse(Constants.NONE_VALUE));
    }


    public static int diffDay(Date d, Date accessDate) {
        /**
         * 날짜 계산
         */
        Calendar curC = Calendar.getInstance();
        Calendar accessC = Calendar.getInstance();
        curC.setTime(d);
        accessC.setTime(accessDate);
        accessC.compareTo(curC);
        int diffCnt = 0;
        while (!accessC.after(curC)) {
            diffCnt++;
            accessC.add(Calendar.DATE, 1); // 다음날로 바뀜

            System.out.println(accessC.get(Calendar.YEAR) + "년 " + (accessC.get(Calendar.MONTH) + 1) + "월 " + accessC.get(Calendar.DATE) + "일");
        }
        System.out.println("기준일로부터 " + diffCnt + "일이 지났습니다.");
        System.out.println(accessC.compareTo(curC));
        return diffCnt;
    }

    public static String convertApiUrl(String url) {
        return url.replace("https://", "").replace("http://", "");
    }


    /**
     * DefaultCloudFoundryOperations을 생성하여, 반환한다.
     *
     * @param connectionContext
     * @param tokenProvider
     * @return DefaultCloudFoundryOperations
     */
    public static DefaultCloudFoundryOperations cloudFoundryOperations(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return cloudFoundryOperations(cloudFoundryClient(connectionContext, tokenProvider), dopplerClient(connectionContext, tokenProvider), uaaClient(connectionContext, tokenProvider));
    }

    /**
     * DefaultCloudFoundryOperations을 생성하여, 반환한다.
     *
     * @param cloudFoundryClient
     * @param dopplerClient
     * @param uaaClient
     * @return DefaultCloudFoundryOperations
     */
    public static DefaultCloudFoundryOperations cloudFoundryOperations(org.cloudfoundry.client.CloudFoundryClient cloudFoundryClient, DopplerClient dopplerClient, UaaClient uaaClient) {
        return DefaultCloudFoundryOperations.builder().cloudFoundryClient(cloudFoundryClient).dopplerClient(dopplerClient).uaaClient(uaaClient).build();
    }

    /**
     * DefaultCloudFoundryOperations을 생성하여, 반환한다.
     *
     * @param connectionContext
     * @param tokenProvider
     * @param org
     * @param space
     * @return DefaultCloudFoundryOperations
     */
    public static DefaultCloudFoundryOperations cloudFoundryOperations(ConnectionContext connectionContext, TokenProvider tokenProvider, String org, String space) {
        return cloudFoundryOperations(cloudFoundryClient(connectionContext, tokenProvider), dopplerClient(connectionContext, tokenProvider), uaaClient(connectionContext, tokenProvider), org, space);
    }

    /**
     * DefaultCloudFoundryOperations을 생성하여, 반환한다.
     *
     * @param cloudFoundryClient
     * @param dopplerClient
     * @param uaaClient
     * @param org
     * @param space
     * @return DefaultCloudFoundryOperations
     */
    public static DefaultCloudFoundryOperations cloudFoundryOperations(org.cloudfoundry.client.CloudFoundryClient cloudFoundryClient, DopplerClient dopplerClient, UaaClient uaaClient, String org, String space) {
        return DefaultCloudFoundryOperations.builder().cloudFoundryClient(cloudFoundryClient).dopplerClient(dopplerClient).uaaClient(uaaClient).organization(org).space(space).build();
    }

    /**
     * ReactorCloudFoundryClient 생성하여, 반환한다.
     *
     * @param connectionContext
     * @param tokenProvider
     * @return DefaultCloudFoundryOperations
     */
    public static ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorCloudFoundryClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    }


    public static ReactorDopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorDopplerClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    }

    public static ReactorUaaClient uaaClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorUaaClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    }

    public static ReactorUaaClient uaaClient(ConnectionContext connectionContext, String clientId, String clientSecret) {
        return ReactorUaaClient.builder().connectionContext(connectionContext).tokenProvider(ClientCredentialsGrantTokenProvider.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build()
        ).build();
    }

    public DefaultConnectionContext connectionContext() {
        return DefaultConnectionContext.builder().apiHost(convertApiUrl(apiTarget)).skipSslValidation(cfskipSSLValidation).build();
    }


    public static DefaultConnectionContext connectionContext(String apiUrl, boolean skipSSLValidation) {
        return DefaultConnectionContext.builder().apiHost(convertApiUrl(apiUrl)).skipSslValidation(skipSSLValidation).build();
    }

    public static TokenGrantTokenProvider tokenProvider(String token) {
        if (token.indexOf("bearer") < 0) {
            token = "bearer " + token;
        }
        return new TokenGrantTokenProvider(token);
    }

    /**
     * token을 제공하는 클레스 사용자 임의의 clientId를 사용하며,
     * user token, client token을 모두 얻을 수 있다.
     *
     * @param username
     * @param password
     * @return
     */
    public static PasswordGrantTokenProvider tokenProvider(String username, String password) {
        return PasswordGrantTokenProvider.builder().password(password).username(username).build();
    }

}
