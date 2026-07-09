package RMC_Booking_Engine.rmc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_notification_read")
@IdClass(StaffNotificationRead.StaffNotificationReadId.class)
@Getter
@Setter
public class StaffNotificationRead {

    @Id
    @Column(name = "staff_user_id")
    private Long staffUserId;

    @Id
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt = Instant.now();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class StaffNotificationReadId implements Serializable {
        private Long staffUserId;
        private Long notificationId;
    }
}
