package com.ccsanjuu.blog.modules.message.model.bo;

import lombok.Data;

@Data
public class TurnstileVerificationBO {

    private boolean success;

    private String hostname;

    private String action;
}
