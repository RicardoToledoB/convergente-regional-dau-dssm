package cl.dssm.dau.dto;

public record ApiResponse<T>(boolean ok, String message, T data) {}
