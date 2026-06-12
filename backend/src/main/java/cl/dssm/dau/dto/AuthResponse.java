package cl.dssm.dau.dto;

public record AuthResponse(String token, String username, String fullName, String role, String providerName) {}
