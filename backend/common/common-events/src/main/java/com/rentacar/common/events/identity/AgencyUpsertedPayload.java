package com.rentacar.common.events.identity;

import java.util.UUID;

public record AgencyUpsertedPayload(
        UUID agencyId,
        String name,
        String city,
        String district,
        String phone,
        String contactPerson,
        String address
) {
}

