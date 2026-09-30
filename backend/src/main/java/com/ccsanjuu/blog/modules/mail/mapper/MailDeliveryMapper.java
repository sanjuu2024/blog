package com.ccsanjuu.blog.modules.mail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MailDeliveryMapper extends BaseMapper<MailDelivery> {

    /**
     * 锁定投递记录，串行化手动重试，避免同一失败记录被并发处理。
     *
     * @param deliveryId 投递记录 ID
     * @return 投递记录
     */
    MailDelivery selectByIdForUpdate(@Param("deliveryId") Long deliveryId);
}
