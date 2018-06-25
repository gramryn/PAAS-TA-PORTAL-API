package org.openpaas.paasta.portal.api.service;


import com.corundumstudio.socketio.SocketIOClient;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.cloudfoundry.client.lib.org.codehaus.jackson.map.ObjectMapper;
import org.cloudfoundry.client.lib.org.codehaus.jackson.type.TypeReference;
import org.cloudfoundry.client.v2.OrderDirection;
import org.cloudfoundry.client.v2.applications.*;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.client.v2.events.ListEventsResponse;
import org.cloudfoundry.client.v2.routemappings.CreateRouteMappingRequest;
import org.cloudfoundry.client.v2.routemappings.DeleteRouteMappingRequest;
import org.cloudfoundry.client.v2.routemappings.GetRouteMappingRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.DeleteRouteRequest;
import org.cloudfoundry.client.v2.routes.Route;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingResponse;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstanceServiceBindingsRequest;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstanceServiceBindingsResponse;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.LogsRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.openpaas.paasta.portal.api.common.Common;
import org.openpaas.paasta.portal.api.model.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 앱 서비스 - 애플리케이션 정보 조회, 구동, 정지 등의 API 를 호출 하는 서비스이다.
 *
 * @author 조민구
 * @version 1.0
 * @since 2016.4.4 최초작성
 */
@Service
public class AppService extends Common {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppService.class);


    @HystrixCommand(commandKey = "getAppSummary")
    public SummaryApplicationResponse getAppSummary(String guid, String token) throws IOException {

        SummaryApplicationResponse summaryApplicationResponse = Common.cloudFoundryClient(connectionContext(), tokenProvider(token))
                .applicationsV2()
                .summary(SummaryApplicationRequest.builder()
                        .applicationId(guid)
                        .build())
                .log()
                .block();

        return summaryApplicationResponse;
    }


    /**
     * 앱 실시간 상태를 조회한다.
     *
     * @param guid  the app guid
     * @param token the client
     * @return the app stats
     */
    @HystrixCommand(commandKey = "getAppStats")
    public ApplicationStatisticsResponse getAppStats(String guid, String token) {
        ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext(), tokenProvider(token));

        ApplicationStatisticsResponse applicationStatisticsResponse =
                cloudFoundryClient.applicationsV2()
                        .statistics(ApplicationStatisticsRequest.builder()
                                .applicationId(guid)
                                .build()).block();

        return applicationStatisticsResponse;
    }

    /**
     * 앱을 변경한다.
     *
     * @param app   the app
     * @param token the client
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "renameApp")
    public Map renameApp(App app, String token){
        HashMap result = new HashMap();
        try{
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext(), tokenProvider(adminUserName, adminPassword));
            UpdateApplicationResponse response =
                    cloudFoundryClient.applicationsV2().update(UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).name(app.getNewName()).build()).block();

            LOGGER.info("Update app response :", response);

            result.put("result", true);
            result.put("msg", "You have successfully completed the task.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", false);
            result.put("msg", e.getMessage());
        }

        return result;

    }


    /**
     * 앱을 실행한다.
     *
     * @param app   the app
     * @param token the client
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "startApp")
    public Map startApp(App app, String token) throws Exception {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext(), tokenProvider(token));

            DefaultCloudFoundryOperations cloudFoundryOperations = cloudFoundryOperations(connectionContext(), tokenProvider(token),app.getOrgName(),app.getSpaceName());
            cloudFoundryOperations.applications().start(StartApplicationRequest.builder().name(app.getName()).build()).block();

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }


    /**
     * 앱을 중지한다.
     *
     * @param app   the app
     * @param token the client
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "stopApp")
    public Map stopApp(App app, String token) throws Exception {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext(), tokenProvider(token));

            cloudFoundryClient.applicationsV3()
                    .stop(org.cloudfoundry.client.v3.applications.StopApplicationRequest.builder()
                            .applicationId(app.getGuid().toString())
                            .build()
                    ).block();
            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }


    /**
     * 앱을 삭제한다.
     *
     * @param guid the app
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "deleteApp")
    public Map deleteApp(String guid) {
        HashMap result = new HashMap();
        try {
            //앱 삭제
            ReactorCloudFoundryClient reactorCloudFoundryClient = Common.cloudFoundryClient(connectionContext(), tokenProvider(adminUserName, adminPassword));
            List<Route> routes = reactorCloudFoundryClient.applicationsV2().summary(SummaryApplicationRequest.builder().applicationId(guid).build()).block().getRoutes();
            for(Route route : routes) {
                reactorCloudFoundryClient.routes().delete(DeleteRouteRequest.builder().routeId(route.getId()).build()).block();
            }
            reactorCloudFoundryClient.applicationsV2().delete(DeleteApplicationRequest.builder().applicationId(guid).build()).block();
            result.put("result", true);
            result.put("msg", "You have successfully completed the task.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", false);
            result.put("msg", e.getMessage());
        }

        try {
            //AutoScale 설정 삭제
            HashMap map = new HashMap();
            map.put("guid", String.valueOf(guid));

        } catch (Exception e) {
            e.printStackTrace();
            result.put("result", false);
            result.put("msg", e.getMessage());
        }

        return result;
    }


    /**
     * 앱을 리스테이징한다.
     *
     * @param app   the app
     * @param token the client
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "restageApp")
    public Map restageApp(App app, String token) throws Exception {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext(), tokenProvider(token));

            cloudFoundryClient.applicationsV2()
                    .restage(RestageApplicationRequest.builder()
                            .applicationId(app.getGuid().toString())
                            .build()
                    ).block();

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * 앱 인스턴스를 변경한다.
     *
     * @param app   the app
     * @param token the client
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "updateApp")
    public Map updateApp(App app, String token) throws Exception {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext(), tokenProvider(token));
            if (app.getInstances() > 0) {
                cloudFoundryClient.applicationsV2().update(UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).instances(app.getInstances()).build()).block();
            }
            if (app.getMemory() > 0) {
                cloudFoundryClient.applicationsV2().update(UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).memory(app.getMemory()).build()).block();
            }
            if (app.getDiskQuota() > 0) {
                cloudFoundryClient.applicationsV2().update(UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).diskQuota(app.getDiskQuota()).build()).block();
            }
            if (app.getName() != null && !app.getName().equals("")) {
                cloudFoundryClient.applicationsV2().update(UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).name(app.getName()).build()).block();
            }
            if (app.getEnvironment() != null && app.getEnvironment().size() > 0) {
                cloudFoundryClient.applicationsV2().update(UpdateApplicationRequest.builder().applicationId(app.getGuid().toString()).environmentJsons(app.getEnvironment()).build()).block();
            }

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * 앱-서비스를 바인드한다.
     *
     * @param body
     * @param token the client
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "bindService")
    public Map bindService(Map body, String token) throws Exception {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext(), tokenProvider(token));

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parameterMap = mapper.readValue(body.get("parameter").toString(), new TypeReference<Map<String, Object>>() {
            });

            cloudFoundryClient.serviceBindingsV2()
                    .create(CreateServiceBindingRequest.builder()
                            .applicationId(body.get("applicationId").toString())
                            .serviceInstanceId(body.get("serviceInstanceId").toString())
                            .parameters(parameterMap)
                            .build()
                    ).block();

            resultMap.put("result", true);

        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }


    /**
     * 앱-서비스를 언바인드한다.
     *
     * @param serviceInstanceId
     * @param applicationId
     * @param token             the client
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "unbindService")
    public Map unbindService(String serviceInstanceId, String applicationId, String token) throws Exception {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext(), tokenProvider(token));

            ListServiceInstanceServiceBindingsResponse listServiceInstanceServiceBindingsResponse =
                    cloudFoundryClient.serviceInstances()
                            .listServiceBindings(ListServiceInstanceServiceBindingsRequest.builder()
                                    .applicationId(applicationId)
                                    .serviceInstanceId(serviceInstanceId)
                                    .build()
                            ).block();

            String instancesServiceBindingGuid = listServiceInstanceServiceBindingsResponse.getResources().get(0).getMetadata().getId();

            DeleteServiceBindingResponse deleteServiceBindingResponse = cloudFoundryClient.serviceBindingsV2()
                    .delete(DeleteServiceBindingRequest.builder()
                            .serviceBindingId(instancesServiceBindingGuid)
                            .build()
                    ).block();

            resultMap.put("result", true);

        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * 앱 이벤트를 조회한다.
     *
     * @param guid
     * @param token the client
     * @return the app events
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "getAppEvents")
    public ListEventsResponse getAppEvents(String guid, String token) throws Exception {
        ReactorCloudFoundryClient cloudFoundryClient = Common.cloudFoundryClient(connectionContext(), tokenProvider(token));

        ListEventsRequest.Builder requestBuilder = ListEventsRequest.builder().actee(guid).resultsPerPage(100).orderDirection(OrderDirection.DESCENDING);
        ListEventsResponse listEventsResponse = cloudFoundryClient.events().list(requestBuilder.build()).block();

        return listEventsResponse;
    }

    /**
     * 앱 환경변수를 조회한다.
     *
     * @param guid
     * @param token the token
     * @return the application env
     * @throws Exception the exception
     * @author 김도준
     * @version 1.0
     * @since 2016.6.29 최초작성
     */
    @HystrixCommand(commandKey = "getApplicationEnv")
    public ApplicationEnvironmentResponse getApplicationEnv(String guid, String token) throws Exception {
        ReactorCloudFoundryClient cloudFoundryClient = Common.cloudFoundryClient(connectionContext(), tokenProvider(token));

        ApplicationEnvironmentResponse applicationEnvironmentResponse =
                cloudFoundryClient.applicationsV2()
                        .environment(ApplicationEnvironmentRequest.builder()
                                .applicationId(guid)
                                .build()
                        ).block();

        return applicationEnvironmentResponse;
    }

    /**
     * 라우트 추가 및 라우트와 앱을 연결한다. (앱에 URI를 추가함)
     *
     * @param body
     * @param token the token
     * @return the boolean
     * @throws Exception the exception
     * @author 김도준
     * @version 1.0
     * @since 2016.7.6 최초작성
     */
    @HystrixCommand(commandKey = "addApplicationRoute")
    public Map addApplicationRoute(Map body, String token) throws Exception {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = Common.cloudFoundryClient(connectionContext(), tokenProvider(token));

            CreateRouteResponse createRouteResponse =
                    cloudFoundryClient.routes()
                            .create(CreateRouteRequest.builder()
                                    .host(body.get("host").toString())
                                    .domainId(body.get("domainId").toString())
                                    .spaceId(body.get("spaceId").toString())
                                    .build()
                            ).block();

            cloudFoundryClient.routeMappings()
                    .create(CreateRouteMappingRequest.builder()
                            .applicationId(body.get("applicationId").toString())
                            .routeId(createRouteResponse.getMetadata().getId())
                            .build()
                    ).block();

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * 앱 라우트를 해제한다.
     *
     * @param guid
     * @param route_guid
     * @param token      the token
     * @return the boolean
     * @throws Exception the exception
     * @author 김도준
     * @version 1.0
     * @since 2016.7.6 최초작성
     */
    @HystrixCommand(commandKey = "removeApplicationRoute")
    public Map removeApplicationRoute(String guid, String route_guid, String token) throws Exception {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = Common.cloudFoundryClient(connectionContext(), tokenProvider(token));

            cloudFoundryClient.applicationsV2()
                    .removeRoute(
                            RemoveApplicationRouteRequest.builder()
                                    .applicationId(guid)
                                    .routeId(route_guid)
                                    .build()
                    ).block();

            cloudFoundryClient.routes()
                    .delete(DeleteRouteRequest.builder()
                            .routeId(route_guid)
                            .build()
                    ).block();

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    /**
     * 라우트를 삭제한다.
     *
     * @param orgName   the org name
     * @param spaceName the space name
     * @param urls      the urls
     * @param token     the token
     * @return the boolean
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "deleteRoute")
    public boolean deleteRoute(String orgName, String spaceName, List<String> urls, String token) throws Exception {
        return true;
    }

    /**
     * 인덱스로 앱 인스턴스를 종료한다.
     *
     * @param guid
     * @param index
     * @param token
     * @return the map
     * @throws Exception the exception
     */
    @HystrixCommand(commandKey = "terminateInstance")
    public Map terminateInstance(String guid, String index, String token) throws Exception {
        Map resultMap = new HashMap();

        try {
            ReactorCloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext(), tokenProvider(token));

            TerminateApplicationInstanceRequest.Builder requestBuilder = TerminateApplicationInstanceRequest.builder();
            requestBuilder.applicationId(guid);
            requestBuilder.index(index);
            cloudFoundryClient.applicationsV2().terminateInstance(requestBuilder.build()).block();

            resultMap.put("result", true);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", false);
            resultMap.put("msg", e);
        }

        return resultMap;
    }

    @HystrixCommand(commandKey = "getRecentLog")
    public List<Envelope> getRecentLog(String guid, String token) {
        TokenProvider tokenProvider = tokenProvider(token);
        ReactorDopplerClient reactorDopplerClient = Common.dopplerClient(connectionContext(), tokenProvider);

        RecentLogsRequest.Builder requestBuilder = RecentLogsRequest.builder();
        requestBuilder.applicationId(guid);

        List<Envelope> getRecentLog = reactorDopplerClient.recentLogs(requestBuilder.build()).collectList().block();
        return getRecentLog;
    }

    @HystrixCommand(commandKey = "getTailLog")
    public List<LogMessage> getTailLog(String guid, String token) {
        DefaultCloudFoundryOperations cloudFoundryOperations = cloudFoundryOperations(connectionContext(), tokenProvider(token), "demo.org", "dev");

        cloudFoundryOperations.applications()
                .logs(LogsRequest.builder()
                        .name("github-test-app2")
                        .build()
                ).subscribe((msg) -> {
                    printLog(msg);
                },
                (error) -> {
                    error.printStackTrace();
                }
        );

        return null;
    }

    private void printLog(LogMessage msg) {
        LOGGER.info(" [" + msg.getSourceType() + "/" + msg.getSourceInstance() + "] [" + msg.getMessageType() + msg.getMessageType() + "] " + msg.getMessage());
    }

    @HystrixCommand(commandKey = "socketTailLogs")
    public SocketIOClient socketTailLogs(SocketIOClient client, String appName, String orgName, String spaceName, String token) {
        DefaultCloudFoundryOperations cloudFoundryOperations = cloudFoundryOperations(connectionContext(), tokenProvider(token), orgName, spaceName);

        cloudFoundryOperations.applications()
                .logs(LogsRequest.builder()
                        .name(appName)
                        .build()
                ).subscribe((msg) -> {
                    printLog(msg);
                    client.sendEvent("message", " [" + msg.getSourceType() + "/" + msg.getSourceInstance() + "] [" + msg.getMessageType() + msg.getMessageType() + "] " + msg.getMessage());
                },
                (error) -> {
                    error.printStackTrace();
                }
        );
        return client;
    }

}
