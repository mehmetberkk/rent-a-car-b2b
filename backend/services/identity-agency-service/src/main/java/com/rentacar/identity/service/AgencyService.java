package com.rentacar.identity.service;

import com.rentacar.common.events.identity.AgencyUpsertedPayload;
import com.rentacar.identity.dto.AgencyDtos.*;
import com.rentacar.identity.dto.AuthDtos.RefreshedTokenResponse;
import com.rentacar.identity.event.IdentityEventProducer;
import com.rentacar.identity.model.Agency;
import com.rentacar.identity.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final JwtService jwtService;
    private final IdentityEventProducer eventProducer;

    @Transactional
    public CreateAgencyResponse createAgency(UUID ownerId, String userEmail, CreateAgencyRequest request) {
        Agency agency = Agency.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .city(request.getCity())
                .district(request.getDistrict())
                .phone(request.getPhone())
                .contactPerson(request.getContactPerson())
                .address(request.getAddress())
                .build();

        agency = agencyRepository.save(agency);
        log.info("Agency created: id={}, name={}, ownerId={}", agency.getId(), agency.getName(), ownerId);

        // Publish event to Kafka
        AgencyUpsertedPayload payload = new AgencyUpsertedPayload(
                agency.getId(),
                agency.getName(),
                agency.getCity(),
                agency.getDistrict(),
                agency.getPhone(),
                agency.getContactPerson(),
                agency.getAddress()
        );
        eventProducer.publishAgencyUpserted(payload, agency.getId(), UUID.randomUUID());

        // Generate refreshed token with newly added agencyId
        List<UUID> updatedAgencyIds = agencyRepository.findAgencyIdsByOwnerId(ownerId);
        String refreshedToken = jwtService.generateToken(ownerId, userEmail, updatedAgencyIds);

        return CreateAgencyResponse.builder()
                .agency(toDto(agency))
                .refreshedToken(RefreshedTokenResponse.builder()
                        .accessToken(refreshedToken)
                        .tokenType("Bearer")
                        .expiresIn(jwtService.getExpirationMs() / 1000)
                        .agencyIds(updatedAgencyIds)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AgencyDto> getAgenciesByOwner(UUID ownerId) {
        return agencyRepository.findByOwnerId(ownerId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgencyDto getAgencyById(UUID agencyId, UUID ownerId) {
        Agency agency = agencyRepository.findByIdAndOwnerId(agencyId, ownerId)
                .orElseThrow(() -> new SecurityException("Bu dükkan bulunamadı veya bu işlem için yetkiniz yok."));
        return toDto(agency);
    }

    @Transactional
    public AgencyDto updateAgency(UUID agencyId, UUID ownerId, CreateAgencyRequest request) {
        Agency agency = agencyRepository.findByIdAndOwnerId(agencyId, ownerId)
                .orElseThrow(() -> new SecurityException("Bu dükkan bulunamadı veya bu işlem için yetkiniz yok."));

        agency.setName(request.getName());
        agency.setCity(request.getCity());
        agency.setDistrict(request.getDistrict());
        agency.setPhone(request.getPhone());
        agency.setContactPerson(request.getContactPerson());
        agency.setAddress(request.getAddress());

        agency = agencyRepository.save(agency);
        log.info("Agency updated: id={}, name={}", agency.getId(), agency.getName());

        AgencyUpsertedPayload payload = new AgencyUpsertedPayload(
                agency.getId(),
                agency.getName(),
                agency.getCity(),
                agency.getDistrict(),
                agency.getPhone(),
                agency.getContactPerson(),
                agency.getAddress()
        );
        eventProducer.publishAgencyUpserted(payload, agency.getId(), UUID.randomUUID());

        return toDto(agency);
    }

    @Transactional
    public DeleteAgencyResponse deleteAgency(UUID agencyId, UUID ownerId, String userEmail) {
        Agency agency = agencyRepository.findByIdAndOwnerId(agencyId, ownerId)
                .orElseThrow(() -> new SecurityException("Bu dükkan bulunamadı veya bu işlem için yetkiniz yok."));

        agencyRepository.delete(agency);
        log.info("Agency deleted: id={}", agencyId);

        List<UUID> updatedAgencyIds = agencyRepository.findAgencyIdsByOwnerId(ownerId);
        String refreshedToken = jwtService.generateToken(ownerId, userEmail, updatedAgencyIds);

        return DeleteAgencyResponse.builder()
                .message("Dükkan başarıyla silindi.")
                .refreshedToken(RefreshedTokenResponse.builder()
                        .accessToken(refreshedToken)
                        .tokenType("Bearer")
                        .expiresIn(jwtService.getExpirationMs() / 1000)
                        .agencyIds(updatedAgencyIds)
                        .build())
                .build();
    }

    private AgencyDto toDto(Agency agency) {
        return AgencyDto.builder()
                .id(agency.getId())
                .ownerId(agency.getOwnerId())
                .name(agency.getName())
                .city(agency.getCity())
                .district(agency.getDistrict())
                .phone(agency.getPhone())
                .contactPerson(agency.getContactPerson())
                .address(agency.getAddress())
                .createdAt(agency.getCreatedAt())
                .build();
    }
}

