package com.ipachi.pos.dto;


public record AuthResponse(String token, String username, String role, Long businessProfileId, String terminalId){}
