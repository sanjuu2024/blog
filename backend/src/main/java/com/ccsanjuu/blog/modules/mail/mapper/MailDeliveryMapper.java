package com.ccsanjuu.blog.modules.mail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.modules.mail.model.dto.MailDeliveryQueryDTO;
import com.ccsanjuu.blog.modules.mail.model.vo.MailDeliveryItemVO;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MailDeliveryMapper extends BaseMapper<MailDelivery> {

    /**
     * 联表查询投递记录和收件用户当前资料。
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 投递分页
     */
    Page<MailDeliveryItemVO> selectAdminPage(Page<MailDeliveryItemVO> page, @Param("query") MailDeliveryQueryDTO query);

    /**
     * 查询单条投递及其收件用户资料。
     *
     * @param deliveryId 投递记录 ID
     * @return 投递详情
     */
    MailDeliveryItemVO selectAdminDetail(@Param("deliveryId") Long deliveryId);

    /**
     * 锁定投递记录，串行化手动重试，避免同一失败记录被并发处理。
     *
     * @param deliveryId 投递记录 ID
     * @return 投递记录
     */
    MailDelivery selectByIdForUpdate(@Param("deliveryId") Long deliveryId);
}
