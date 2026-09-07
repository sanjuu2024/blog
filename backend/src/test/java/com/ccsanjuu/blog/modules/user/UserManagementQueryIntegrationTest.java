package com.ccsanjuu.blog.modules.user;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.user.model.dto.UserManagementPageQueryDTO;
import com.ccsanjuu.blog.modules.user.model.vo.AdminUserItemVO;
import com.ccsanjuu.blog.modules.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserManagementQueryIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void userPageQueryShouldCombineUsernameAndEmailFilters() {
        UserManagementPageQueryDTO matchingQuery = new UserManagementPageQueryDTO();
        matchingQuery.setUsername(" demo ");
        matchingQuery.setEmail(" bob@example.com ");

        PageResult<AdminUserItemVO> matchingResult = userService.userPageQuery(matchingQuery);

        assertEquals(1, matchingResult.getTotal());
        assertEquals("demo_bob", matchingResult.getRecords().getFirst().getUsername());

        UserManagementPageQueryDTO excludedQuery = new UserManagementPageQueryDTO();
        excludedQuery.setUsername("demo_alice");
        excludedQuery.setEmail("bob@example.com");

        PageResult<AdminUserItemVO> excludedResult = userService.userPageQuery(excludedQuery);

        assertTrue(excludedResult.getRecords().isEmpty());
    }
}
