package ru.teamscore.java23.booking.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class BookingInfo {
    private String bookingRef;
    private OffsetDateTime bookingDate;
    private String ticketNo;
    private String flightNo;
    private String departureAirport;
    private String arrivalAirport;
    private String passengerName;
    private String fareConditions;
    private BigDecimal amount;
}
