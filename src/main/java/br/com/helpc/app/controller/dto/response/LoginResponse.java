package br.com.helpc.app.controller.dto.response;

public record LoginResponse(String accessToken, Long expiresIn) {
}