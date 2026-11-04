package com.rentacar.common.events.identity;

import java.util.UUID;

public record UserRegisteredPayload(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String activationToken
) {
}

