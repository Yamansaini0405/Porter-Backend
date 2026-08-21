package com.porterclone.security;

/** Lightweight authenticated-user context extracted from the JWT — attached to SecurityContext. */
public record UserPrincipal(Long userId, String role) {
}
