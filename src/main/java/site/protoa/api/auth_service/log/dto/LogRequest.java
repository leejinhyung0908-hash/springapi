package site.protoa.api.auth_service.log.dto;

import lombok.Data;

@Data
public class LogRequest {
    private String action;
    private String url;
    private Integer tokenLength;
}
