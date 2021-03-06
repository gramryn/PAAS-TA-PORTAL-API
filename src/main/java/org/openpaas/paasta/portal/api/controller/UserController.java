package org.openpaas.paasta.portal.api.controller;

import org.cloudfoundry.client.v2.users.GetUserResponse;
import org.cloudfoundry.uaa.users.UpdateUserResponse;
import org.cloudfoundry.uaa.users.User;
import org.openpaas.paasta.portal.api.common.Common;
import org.openpaas.paasta.portal.api.common.Constants;
import org.openpaas.paasta.portal.api.model.UserDetail;
import org.openpaas.paasta.portal.api.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@RestController
public class UserController extends Common {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    private Runtime r = Runtime.getRuntime();
    /**
     * V1 URL HEAD = (empty string)
     */
    private static final String V1_URL = Constants.V1_URL;

    /**
     * V2 URL HEAD = "/v2"
     */
    private static final String V2_URL = Constants.V2_URL;


    /**
     * Update user password map.
     *
     * @param userId   the user id
     * @param body     the body
     * @param request  the request
     * @param response the response
     * @return the map
     * @throws Exception the exception
     */
    @PutMapping(value = {V2_URL + "/users/{userId}/password/update"})
    public Map updateUserPassword(@PathVariable String userId, @RequestBody Map<String, Object> body, @RequestHeader(AUTHORIZATION_HEADER_KEY) String token, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOGGER.info("> into updateUserPassword");
        String oldPassword = (String) body.get("oldPassword");
        String newPassword = (String) body.get("password");
        String userGuid = (String) body.get("userGuid");
        Map<String, Object> result = userService.updateUserPassword(userId,userGuid, oldPassword, newPassword, token);
        return result;
    }


    /**
     * Update user password map.
     *
     * @param body     the body
     * @param request  the request
     * @param response the response
     * @return the map
     * @throws Exception the exception
     */
    @PostMapping(value = {V2_URL + "/users/password/reset"})
    public Map resetPassword(@RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOGGER.info("> into resetPassword");
        String newPassword = (String) body.get("password");
        String userId = (String) body.get("userId");
        Map<String, Object> result = userService.resetPassword(userId, newPassword);
        return result;
    }


    /**
     * Update user password map.
     *
     * @param userGuid the user id
     * @param body     the body
     * @param request  the request
     * @return the map
     * @throws Exception the exception
     */
    @PutMapping(value = {V2_URL + "/users/{userGuid}/password/expired"})
    public Map expiredPassword(@PathVariable String userGuid, @RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOGGER.info("> into expiredPassword");
        Map<String, Object> result = userService.expiredPassword(userGuid);
        return result;
    }


    /**
     * Delete user map.
     *
     * @param guid the user id
     * @return the map
     * @throws Exception the exception
     */
    @DeleteMapping(value = {V2_URL + "/users/{guid}"})
    public Map deleteUser(@PathVariable String guid, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOGGER.info("> into deleteUser");
        Map result = userService.deleteUser(guid);
        return result;
    }


    /**
     * 모든 Uaa 유저의 이름과 Guid를 목록으로 가져온다.
     *
     * @return map all user name
     * @throws Exception the exception
     */
    @GetMapping(value = {V2_URL + "/users"})
    public List<User> getAllUserName(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<User> userInfo = userService.allUsers();
        Map<String, Object> resultMap = new HashMap();
        return userInfo;
    }


    /**
     * 유저의 이름과 Guid를 목록으로 가져온다.
     *
     * @return map all user name
     * @throws Exception the exception
     */
    @GetMapping(value = {V2_URL + "/users/{userId}"})
    public GetUserResponse getUser(@PathVariable String userId, @RequestHeader(AUTHORIZATION_HEADER_KEY) String token, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return userService.getUser(userId, token);
    }


    /**
     * 계정생성 화면
     *
     * @param userDetail the request
     * @param response   the response
     * @return map
     */
    @PostMapping(value = V2_URL + "/users")
    @ResponseBody
    public Map<String, Object> addUser(@RequestBody UserDetail userDetail, HttpServletRequest request, HttpServletResponse response) {
        Map result = userService.createUser(userDetail);
        return result;
    }

    /**
     * 유저 이름(user name)으로 유저의 GUID(user id)를 가져온다.
     *
     * @param username  user name
     * @return User ID
     * @throws Exception the exception
     */
    @GetMapping(V2_URL + "/user/name-by-id/{username}")
    public String getUserId(@PathVariable String username) {
        return userService.getUserIdByUsername(username);
    }

    /**
     * 유저 GUID(user id)로 유저의 이름(user name)을 가져온다.
     *
     * @param userId  user Id
     * @return User name
     * @throws Exception the exception
     */
    @GetMapping(V2_URL + "/user/id-by-name/{userId}")
    public String getUsername(@PathVariable String userId) {
        return userService.getUsernameByUserId(userId);
    }

    /**
     * 실행중인 응용 프로그램에 대한 시큐리티 그룹 조회
     *
     * @param userIdentifier  ListSecurityGroups page
     * @return ListSecurityGroupRunningDefaultsResponse
     * @throws Exception the exception
     */
    @GetMapping(V2_URL + "/user/summary/{userIdentifier}")
    public User getUserSummary(@PathVariable String userIdentifier, @RequestParam String type) {
        String filterType;
        if (null == type) filterType = "default";
        else filterType = type;

        switch (filterType) {
            case "name":
                return userService.getUserSummaryByUsername(userIdentifier);
            default:
            case "id":
                return userService.getUserSummary(userIdentifier);
        }
    }

    /**
     * 사용자 포탈 접속 가능 유무 수정
     *
     * @param userid     userId
     * @return Map(자바클래스)
     * @throws Exception Exception(자바클래스)
     */
    @PutMapping(V2_URL + "/user/{userid}/active")
    public UpdateUserResponse UpdateUserActive(@PathVariable String userid) throws Exception {
        return userService.UpdateUserActive(userid);
    }
}