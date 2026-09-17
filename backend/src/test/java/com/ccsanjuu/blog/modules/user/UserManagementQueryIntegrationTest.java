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
    void userPageQueryShouldCombineCaseInsensitiveUsernameAndEmailFilters() {
        UserManagementPageQueryDTO matchingQuery = new UserManagementPageQueryDTO();
        matchingQuery.setUsername(" DEMO_B ");
        matchingQuery.setEmail(" BOB@EXAMPLE.COM ");

        PageResult<AdminUserItemVO> matchingResult = userService.userPageQuery(matchingQuery);

        assertEquals(1, matchingResult.getTotal());
        assertEquals("demo_bob", matchingResult.getRecords().getFirst().getUsername());

        UserManagementPageQueryDTO excludedQuery = new UserManagementPageQueryDTO();
        excludedQuery.setUsername("demo_alice");
        excludedQuery.setEmail("bob@example.com");

        PageResult<AdminUserItemVO> excludedResult = userService.userPageQuery(excludedQuery);

        assertTrue(excludedResult.getRecords().isEmpty());
    }

    @Test
    void userPageQueryShouldTreatSqlWildcardAsPlainText() {
        UserManagementPageQueryDTO query = new UserManagementPageQueryDTO();
        query.setUsername("demo%b");

        PageResult<AdminUserItemVO> result = userService.userPageQuery(query);

        assertTrue(result.getRecords().isEmpty());
    }
}
