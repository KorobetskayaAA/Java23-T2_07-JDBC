package ru.teamscore.java23.booking.entities;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class BookingCreate {
    private String bookingRef;
    private String bookingDate;
    @Getter
    private List<Ticket> tickets = new ArrayList<>();
    public BigDecimal getTotalAmount() {
        return tickets.stream()
                .flatMap(t -> t.flights.stream())
                .map(tf -> tf.amount)
                .reduce(BigDecimal.ZERO, (a, sum) -> sum.add(a).setScale(2));
    }
    @Data
    public class Ticket {
        private String ticketNo;
        private String passengerId;
        private String passengerName;
        private String contactData;
        @Getter
        private List<TicketFlight> flights = new ArrayList<>();

        @Data
        public class TicketFlight {
            private int flightId;
            private String fare_conditions;
            private BigDecimal amount;
        }
    }
}
